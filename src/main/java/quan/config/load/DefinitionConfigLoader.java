package quan.config.load;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import ognl.DefaultTypeConverter;
import ognl.MapPropertyAccessor;
import ognl.NoSuchPropertyException;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Triple;
import quan.config.Config;
import quan.config.TableType;
import quan.config.ValidatedException;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.IndexDefinition;
import quan.config.definition.Language;
import quan.config.definition.parser.DefinitionParser;
import quan.config.definition.parser.XmlDefinitionParser;
import quan.config.read.CSVConfigReader;
import quan.config.read.ConfigReader;
import quan.config.read.ExcelConfigReader;
import quan.config.read.JsonConfigReader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 使用配置定义的配置加载器，根据配置定义实现了索引、引用校验等
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DefinitionConfigLoader extends ConfigLoader {

    protected DefinitionParser parser;

    //表格正文起始行号
    private int tableBodyStartRow;

    {
        tableType = TableType.xlsx;
    }

    private String tableTag;

    private static ThreadLocal<JSONObject> ognlJson = new ThreadLocal<>();

    private static ThreadLocal<BeanDefinition> ognlDefinition = new ThreadLocal<>();

    static {
        OgnlRuntime.setPropertyAccessor(JSONObject.class, new MapPropertyAccessor() {
            @Override
            public Object getProperty(OgnlContext context, Object target, Object name) throws OgnlException {
                BeanDefinition beanDefinition = ognlDefinition.get();
                if (ognlJson.get() == target && beanDefinition != null && beanDefinition.getField(name.toString()) == null) {
                    throw new OgnlException(String.format("%s不存在字段:%s", beanDefinition.getValidatedName(), name));
                } else {
                    return super.getProperty(context, target, name);
                }
            }
        });
    }

    public DefinitionConfigLoader(String tablePath) {
        super(tablePath);
    }


    /**
     * 设置表格正文起始行号
     */
    public void setTableBodyStartRow(int tableBodyStartRow) {
        if (tableBodyStartRow <= 0) {
            return;
        }
        int minTableBodyStartRow = parser.getMinTableBodyStartRow();
        if (tableBodyStartRow < minTableBodyStartRow) {
            throw new IllegalArgumentException("表格正文起始行号不能小于" + minTableBodyStartRow);
        }
        this.tableBodyStartRow = tableBodyStartRow;
    }

    public void setLoadMode(LoadMode loadMode) {
        Objects.requireNonNull(loadMode, "加载模式不能为空");
        this.loadMode = loadMode;
    }

    public String getTableTag() {
        return tableTag;
    }

    public void setTableTag(String tableTag) {
        this.tableTag = tableTag;
    }

    /**
     * 使用XML配置解析器
     */
    public DefinitionParser useXmlDefinition(List<String> definitionPaths, String packagePrefix) {
        parser = new XmlDefinitionParser();
        parser.setDefinitionPaths(definitionPaths);
        parser.setPackagePrefix(packagePrefix);
        setParser(parser);
        return parser;
    }

    /**
     * 使用XML配置解析器
     */
    public DefinitionParser useXmlDefinition(String definitionPath, String packagePrefix) {
        return useXmlDefinition(Collections.singletonList(definitionPath), packagePrefix);
    }

    public DefinitionParser useXmlDefinition(String definitionPath) {
        return useXmlDefinition(Collections.singletonList(definitionPath), null);
    }

    /**
     * 设置配置解析器
     */
    public void setParser(DefinitionParser parser) {
        Objects.requireNonNull(parser, "配置解析器不能为空");
        this.parser = parser;
        tableBodyStartRow = parser.getMinTableBodyStartRow();
    }

    public DefinitionParser getParser() {
        return parser;
    }

    /**
     * 解析配置定义
     */
    private void parseDefinitions() {
        Objects.requireNonNull(parser, "配置定义解析器不能为空");
        if (!parser.getClassDefinitions().isEmpty()) {
            return;
        }

        try {
            parser.parse();
        } catch (Exception e) {
            logger.error("解析配置定义文件[{}]出错", parser.getDefinitionPaths(), e);
            return;
        }

        LinkedHashSet<String> validatedErrors = parser.getValidatedErrors();
        if (!validatedErrors.isEmpty()) {
            String error = String.format("解析配置定义文件%s共发现%d条错误。", parser.getDefinitionPaths(), validatedErrors.size());
            ValidatedException validatedException = new ValidatedException(error);
            validatedException.addErrors(validatedErrors);
            throw validatedException;
        }
    }

    protected void doLoadAll() {
        parseDefinitions();

        //配置对应的已索引JSON数据
        Map<ConfigDefinition, Map<IndexDefinition, Map>> allConfigIndexedJsons = new HashMap<>();

        for (ConfigDefinition configDefinition : parser.getConfigDefinitions().values()) {
            if (supportValidate()) {
                allConfigIndexedJsons.put(configDefinition, validateIndex(configDefinition));
                validateByOgnl(configDefinition);
            }
            if (supportLoad()) {
                load(configDefinition.getFullName(), getConfigTables(configDefinition), false);
            }
        }

        if (supportValidate()) {
            for (ConfigDefinition configDefinition : allConfigIndexedJsons.keySet()) {
                validateRef(configDefinition, allConfigIndexedJsons);
            }
        }
    }

    /**
     * 加载JSON格式配置数据
     *
     * @param configDefinition 配置定义
     * @param onlySelf         true:只包含自己的数据，false:包含自己和子孙配置的数据
     * @return JSON格式配置数据
     */
    public List<JSONObject> loadJsons(ConfigDefinition configDefinition, boolean onlySelf) {
        List<JSONObject> jsons = new ArrayList<>();
        for (String configTable : getConfigTables(configDefinition, onlySelf)) {
            jsons.addAll(getReader(configTable).getJsons());
        }
        return jsons;
    }

    public void writeJson(String path, Language language) {
        if (parser == null) {
            return;
        }

        Objects.requireNonNull(path, "输出路径不能为空");

        File pathFile = new File(path);
        if (!pathFile.exists() && !pathFile.mkdirs()) {
            logger.error("输出路径[{}]创建失败", path);
            return;
        }

        Set<ConfigDefinition> configDefinitions = new HashSet<>(parser.getConfigDefinitions().values());

        for (ConfigDefinition configDefinition : configDefinitions) {
            if (!configDefinition.isSupportedLanguage(language)) {
                continue;
            }

            JSONArray rows = new JSONArray();
            for (String table : configDefinition.getTables()) {
                ConfigReader reader = readers.get(table);
                if (reader == null) {
                    logger.error("配置表格[{}]从未被加载", table);
                    continue;
                }

                List<JSONObject> jsons = reader.getJsons();
                JSONObject row = new JSONObject();

                for (JSONObject json : jsons) {
                    json.forEach((fieldName, fieldValue) -> {
                        FieldDefinition fieldDefinition = configDefinition.getField(fieldName);
                        if (fieldDefinition == null) {
                            fieldDefinition = configDefinition.getField(fieldName.substring(0, fieldName.length() - 1));
                        }
                        if (fieldDefinition.isSupportedLanguage(language)) {
                            row.put(fieldName, fieldValue);
                        }
                    });
                    rows.add(row);
                }
            }

            String jsonFileName = configDefinition.getLongName() + ".json";
            try (FileOutputStream fos = new FileOutputStream(new File(pathFile, jsonFileName))) {
                JSON.writeTo(fos, rows, JSONWriter.Feature.PrettyFormat,  JSONWriter.Feature.ReferenceDetection);
            } catch (Exception e) {
                logger.error("配置[{}]写到JSON文件出错", configDefinition.getName(), e);
            }
        }
    }

    /**
     * 通过表名查找配置定义
     *
     * @param table Json的表名实际上就是配置类名
     */
    private ConfigDefinition getConfigByTable(String table) {
        if (tableType == TableType.json) {
            return parser.getConfigDefinition(table);
        } else {
            return parser.getConfigDefinitions().get(table);
        }
    }

    private TreeSet<String> getConfigTables(ConfigDefinition configDefinition, boolean onlySelf) {
        TreeSet<String> configTables = new TreeSet<>();
        if (onlySelf) {
            if (tableType == TableType.json) {
                configTables.add(configDefinition.getLongName());
            } else {
                configTables.addAll(configDefinition.getTables());
            }
        } else {
            configTables.addAll(getConfigTables(configDefinition));
        }
        return configTables;
    }

    /**
     * 配置的所有分表和子表
     */
    protected Collection<String> getConfigTables(ConfigDefinition configDefinition) {
        if (tableType == TableType.json) {
            //Json的表名实际上就是不含前缀包名的配置类名
            List<String> configTables = new ArrayList<>();
            for (String configLongName : configDefinition.getMeAndDescendants()) {
                ConfigDefinition configDefinition1 = parser.getConfigDefinition(configLongName);
                configTables.add(configDefinition1.getLongName());
            }
            return configTables;
        } else {
            return configDefinition.getAllTables();
        }
    }

    public String getConfigVersion(ConfigDefinition configDefinition, boolean onlySelf) {
        StringBuilder sb = new StringBuilder();
        for (String configTable : getConfigTables(configDefinition, onlySelf)) {
            ConfigReader configReader = getReader(configTable);
            sb.append(configReader.getTableFile().lastModified());
        }
        return DigestUtils.md5Hex(sb.toString());
    }

    @Override
    protected void checkReload() {
        super.checkReload();
        Objects.requireNonNull(parser, "配置定义解析器不能为空");
    }

    @Override
    public Set<Class<? extends Config>> reloadByConfigName(Collection<String> configNames) {
        checkReload();
        validatedErrors.clear();

        Map<String, ConfigDefinition> configDefinitions = new HashMap<>();
        for (ConfigDefinition configDefinition : parser.getConfigDefinitions().values()) {
            String configName = configDefinition.getPackageName() + "." + configDefinition.getName();
            configDefinitions.put(configName, configDefinition);
        }

        Set<String> readerNames = new LinkedHashSet<>();
        for (String configName : configNames) {
            ConfigDefinition configDefinition = configDefinitions.get(configName);
            if (configDefinition == null) {
                validatedErrors.add(String.format("重加载[%s]失败，不存在该配置", configName));
                continue;
            }
            readerNames.addAll(getConfigTables(configDefinition));
        }

        return reloadByReaderName(readerNames);
    }

    private Set<Class<? extends Config>> reloadByReaderName(Collection<String> readerNames) {
        checkReload();
        validatedErrors.clear();

        Set<ConfigDefinition> needReloadConfigs = new LinkedHashSet<>();
        Set<ConfigReader> reloadReaders = new LinkedHashSet<>();

        for (String readerName : readerNames) {
            ConfigReader configReader = readers.get(readerName);
            if (configReader == null) {
                validatedErrors.add(String.format("重加载[%s]失败，对应配置从未被加载", readerName));
                continue;
            }
            configReader.clear();
            reloadReaders.add(configReader);

            ConfigDefinition configDefinition = getConfigByTable(readerName);
            while (configDefinition != null) {
                needReloadConfigs.add(configDefinition);
                configDefinition = configDefinition.getParent();
            }
        }

        for (ConfigDefinition configDefinition : needReloadConfigs) {
            load(configDefinition.getFullName(), getConfigTables(configDefinition), true);
        }

        for (ConfigReader reloadReader : reloadReaders) {
            List<String> errors = reloadReader.getValidatedErrors();
            if (supportValidate()) {
                this.validatedErrors.addAll(errors);
            }
        }

        Set<Class<? extends Config>> reloadedConfigs = reloadReaders.stream().map(r -> r.getPrototype().getClass()).collect(Collectors.toSet());

        invokeListeners(reloadedConfigs, true);

        if (!validatedErrors.isEmpty()) {
            throw new ValidatedException(validatedErrors);
        }

        return reloadedConfigs;
    }

    /**
     * 通过表名(包含目录)重加载
     */
    public void reloadByTableName(Collection<String> tableNames) {
        checkReload();

        Set<String> readerNames = new LinkedHashSet<>();

        for (String tableName : tableNames) {
            ConfigDefinition configDefinition = parser.getConfigDefinitions().get(tableName);
            if (configDefinition == null) {
                validatedErrors.add(String.format("重加载[%s]失败，不存在该配置", tableName));
                continue;
            }
            if (tableType == TableType.json) {
                readerNames.add(configDefinition.getFullName());
            } else {
                readerNames.add(tableName);
            }
        }

        reloadByReaderName(readerNames);
    }

    /**
     * @see #reloadByTableName(Collection)
     */
    public void reloadByTableName(String... originalNames) {
        reloadByTableName(Arrays.asList(originalNames));
    }


    @Override
    protected ConfigReader createReader(String table) {
        File tableFile = new File(tablePath, table + "." + tableType);

        ConfigReader configReader = null;
        ConfigDefinition configDefinition = getConfigByTable(table);

        switch (tableType) {
            case csv:
                configReader = new CSVConfigReader(tableFile, configDefinition);
                break;
            case xls:
            case xlsx:
                configReader = new ExcelConfigReader(tableFile, configDefinition);
                break;
            case json: {
                configReader = new JsonConfigReader(tableFile, configDefinition.getFullName());
                break;
            }
        }

        configReader.setTableTag(tableTag);
        configReader.setTableBodyStartRow(tableBodyStartRow);
        configReader.setTableEncoding(tableEncoding);

        return configReader;
    }

    /**
     * 校验索引
     */
    private Map<IndexDefinition, Map> validateIndex(ConfigDefinition configDefinition) {
        //索引对应的配置JSON
        Map<IndexDefinition, Map> configIndexedJsons = new HashMap<>();
        //配置JSON对应的表格
        Map<JSONObject, String> jsonTables = new HashMap();

        for (String table : getConfigTables(configDefinition)) {
            List<JSONObject> tableJsons = getReader(table).getJsons();
            for (JSONObject json : tableJsons) {
                jsonTables.put(json, table + "." + tableType);
                for (IndexDefinition indexDefinition : configDefinition.getIndexes()) {
                    Map indexedJsons = configIndexedJsons.computeIfAbsent(indexDefinition, ignoredK -> new HashMap());
                    validateTableIndex(indexDefinition, indexedJsons, jsonTables, json);
                }
            }
        }

        return configIndexedJsons;
    }

    private void validateTableIndex(IndexDefinition indexDefinition, Map indexedJsons, Map<JSONObject, String> jsonTables, JSONObject json) {
        String table = jsonTables.get(json);

        if (indexDefinition.isUnique() && indexDefinition.getFields().size() == 1) {
            FieldDefinition field1 = indexDefinition.getFields().get(0);
            if (!json.containsKey(field1.getName())) {
                return;
            }

            JSONObject oldJson = (JSONObject) indexedJsons.put(json.get(field1.getName()), json);
            if (oldJson != null) {
                String repeatedTables = table;
                if (!jsonTables.get(oldJson).equals(table)) {
                    repeatedTables += "," + jsonTables.get(oldJson);
                }
                validatedErrors.add(String.format("配置[%s]有重复数据[%s = %s]", repeatedTables, field1.getColumn(), json.get(field1.getName())));
            }
        }

        if (indexDefinition.isUnique() && indexDefinition.getFields().size() == 2) {
            FieldDefinition field1 = indexDefinition.getFields().get(0);
            FieldDefinition field2 = indexDefinition.getFields().get(1);
            if (!json.containsKey(field1.getName()) || !json.containsKey(field2.getName())) {
                return;
            }

            JSONObject oldJson = (JSONObject) ((Map) indexedJsons.computeIfAbsent(json.get(field1.getName()), ignoredK -> new HashMap<>()))
                    .put(json.get(field2.getName()), json);
            if (oldJson != null) {
                String repeatedTables = table;
                if (!jsonTables.get(oldJson).equals(table)) {
                    repeatedTables += "," + jsonTables.get(oldJson);
                }
                validatedErrors.add(String.format("配置[%s]有重复数据[(%s,%s) = (%s,%s)]", repeatedTables, field1.getColumn(),
                        field2.getColumn(), json.get(field1.getName()), json.get(field2.getName())));
            }
        }

        if (indexDefinition.isUnique() && indexDefinition.getFields().size() == 3) {
            FieldDefinition field1 = indexDefinition.getFields().get(0);
            FieldDefinition field2 = indexDefinition.getFields().get(1);
            FieldDefinition field3 = indexDefinition.getFields().get(2);
            if (!json.containsKey(field1.getName()) || !json.containsKey(field2.getName()) || !json.containsKey(field3.getName())) {
                return;
            }

            JSONObject oldJson = (JSONObject) ((Map) ((Map) indexedJsons.computeIfAbsent(json.get(field1.getName()), ignoredK -> new HashMap<>()))
                    .computeIfAbsent(json.get(field2.getName()), ignoredK -> new HashMap<>())).put(json.get(field3.getName()), json);

            if (oldJson != null) {
                String repeatedTables = table;
                if (!jsonTables.get(oldJson).equals(table)) {
                    repeatedTables += "," + jsonTables.get(oldJson);
                }
                validatedErrors.add(String.format("配置[%s]有重复数据[(%s,%s,%s) = (%s,%s,%s)]", repeatedTables, field1.getColumn(), field2.getColumn(),
                        field3.getColumn(), json.get(field1.getName()), json.get(field2.getName()), json.get(field3.getName())));
            }
        }
    }


    private void validateByOgnl(ConfigDefinition configDefinition) {
        if (!configDefinition.isHasValidations()) {
            return;
        }

        for (String table : getConfigTables(configDefinition)) {
            ConfigReader reader = getReader(table);
            List<JSONObject> jsons = reader.getJsons();

            for (int i = 0; i < jsons.size(); i++) {
                int rowNum = reader.getTableBodyStartRow() + i;
                JSONObject json = jsons.get(i);
                validateByOgnl(configDefinition, table, rowNum, null, json, json);
            }
        }
    }

    private void validateByOgnl(BeanDefinition beanDefinition, String table, int rowNum, String columnName, JSONObject rowJson, JSONObject beanJson) {
        if (!beanDefinition.isHasValidations()) {
            return;
        }

        BeanDefinition ognlOldDefinition = ognlDefinition.get();
        JSONObject ognlOldJson = ognlJson.get();
        ognlDefinition.set(beanDefinition);
        ognlJson.set(beanJson);

        try {
            String validationOwnerInfo = "";
            if (!(beanDefinition instanceof ConfigDefinition)) {
                validationOwnerInfo = beanDefinition.getValidatedName("的");
            }

            for (Object beanValidation : beanDefinition.getValidations()) {
                try {
                    Object result = Ognl.getValue(beanValidation, createOgnlContext(beanJson), beanJson);
                    if (result != null && !(Boolean) result) {
                        validatedErrors.add(String.format("配置[%s]的第%s行数据不符合%s校验规则:%s", table, rowNum, validationOwnerInfo, beanValidation));
                    }
                } catch (Throwable e) {
                    String reason = getOgnlErrorReason(e);
                    validatedErrors.add(String.format("配置[%s]的第%s行数据执行%s校验规则[%s]时出错:%s", table, rowNum, validationOwnerInfo, beanValidation, reason));
                }
            }

            for (FieldDefinition fieldDefinition : beanDefinition.getFields()) {
                if (beanDefinition instanceof ConfigDefinition) {
                    columnName = fieldDefinition.getColumn();
                }

                Object fieldValue = beanJson.get(fieldDefinition.getName());
                Object fieldValidation = fieldDefinition.getValidation();

                if (fieldValidation != null) {
                    if (fieldValue == null) {
                        fieldValue = fieldDefinition.getDefaultValue();
                    }

                    if (!(beanDefinition instanceof ConfigDefinition)) {
                        validationOwnerInfo = beanDefinition.getValidatedName() + fieldDefinition.getValidatedName("的");
                    }

                    try {
                        OgnlContext ognlContext = createOgnlContext(fieldValue);
                        ognlContext.put("row", rowJson);
                        ognlContext.put("owner", beanJson);

                        Object result = Ognl.getValue(fieldValidation, ognlContext, fieldValue);
                        if (result != null && !(Boolean) result) {
                            validatedErrors.add(String.format("配置[%s]的第%s行[%s]数据不符合%s校验规则", table, rowNum, columnName, validationOwnerInfo));
                        }
                    } catch (Throwable e) {
                        String reason = getOgnlErrorReason(e);
                        validatedErrors.add(String.format("配置[%s]的第%s行[%s]数据执行%s校验规则时出错: %s", table, rowNum, columnName, validationOwnerInfo, reason));
                    }
                }

                if (fieldValue != null) {
                    if (fieldDefinition.isBeanType()) {
                        validateByOgnl(fieldDefinition.getTypeBean(), table, rowNum, columnName, rowJson, (JSONObject) fieldValue);
                    } else if (fieldDefinition.isBeanValueType()) {
                        Collection<Object> fieldBeans = fieldDefinition.isMapType() ? ((JSONObject) fieldValue).values() : (JSONArray) fieldValue;
                        for (Object arrayValue : fieldBeans) {
                            validateByOgnl(fieldDefinition.getValueTypeBean(), table, rowNum, columnName, rowJson, (JSONObject) arrayValue);
                        }
                    }
                }
            }
        } finally {
            ognlDefinition.set(ognlOldDefinition);
            ognlJson.set(ognlOldJson);
        }
    }

    private static OgnlContext createOgnlContext(Object root) {
        return Ognl.createDefaultContext(root, null, new DefaultTypeConverter() {
            @Override
            public Object convertValue(OgnlContext context, Object value, Class<?> toType) {
                if (value == null && toType == String.class) {
                    return "";
                } else {
                    return super.convertValue(context, value, toType);
                }
            }
        });
    }

    private static String getOgnlErrorReason(Throwable e) {
        if (e instanceof OgnlException && !(e instanceof NoSuchPropertyException)) {
            return e.getMessage();
        } else {
            return e.toString();
        }
    }

    /**
     * 校验引用，依赖索引结果
     */
    private void validateRef(ConfigDefinition configDefinition, Map<ConfigDefinition, Map<IndexDefinition, Map>> allConfigIndexedJsons) {
        for (String table : getConfigTables(configDefinition)) {
            ConfigReader reader = getReader(table);
            List<JSONObject> tableJsons = reader.getJsons();
            for (int i = 0; i < tableJsons.size(); i++) {
                int rowNum = reader.getTableBodyStartRow() + i;
                JSONObject json = tableJsons.get(i);
                for (String fieldName : json.keySet()) {
                    FieldDefinition fieldDefinition = configDefinition.getField(fieldName);
                    if (fieldDefinition == null) {
                        continue;
                    }
                    Object fieldValue = json.get(fieldName);
                    Triple position = Triple.of(table + "." + tableType, rowNum, fieldDefinition.getColumn());
                    validateFieldRef(position, configDefinition, fieldDefinition, fieldValue, allConfigIndexedJsons);
                }
            }
        }
    }

    private void validateFieldRef(Triple position, BeanDefinition bean, FieldDefinition field, Object value,
                                  Map<ConfigDefinition, Map<IndexDefinition, Map>> allConfigIndexedJsons) {
        if (field.isPrimitiveType()) {
            validatePrimitiveTypeRef(position, bean, field, value, false, allConfigIndexedJsons);
        } else if (field.isBeanType()) {
            validateBeanTypeRef(position, field.getTypeBean(), (JSONObject) value, allConfigIndexedJsons);
        } else if (field.isMapType()) {
            JSONObject map = (JSONObject) value;
            for (String mapKey : map.keySet()) {
                //校验map的key引用
                validatePrimitiveTypeRef(position, bean, field, mapKey, true, allConfigIndexedJsons);
                //校验map的value引用
                Object mapValue = map.get(mapKey);
                if (field.isPrimitiveValueType()) {
                    validatePrimitiveTypeRef(position, bean, field, mapValue, false, allConfigIndexedJsons);
                } else {
                    validateBeanTypeRef(position, field.getValueTypeBean(), (JSONObject) mapValue, allConfigIndexedJsons);
                }
            }

        } else if (field.isSetType() || field.isListType()) {
            JSONArray array = (JSONArray) value;
            for (Object arrayValue : array) {
                if (field.isPrimitiveValueType()) {
                    validatePrimitiveTypeRef(position, bean, field, arrayValue, false, allConfigIndexedJsons);
                } else {
                    validateBeanTypeRef(position, field.getValueTypeBean(), (JSONObject) arrayValue, allConfigIndexedJsons);
                }
            }
        }
    }

    private void validatePrimitiveTypeRef(Triple position, BeanDefinition bean, FieldDefinition field, Object value, boolean mapKey,
                                          Map<ConfigDefinition, Map<IndexDefinition, Map>> allConfigIndexedJsons) {
        ConfigDefinition fieldRefConfig = field.getRefConfig(mapKey);
        FieldDefinition fieldRefField = field.getRefField(mapKey);
        if (fieldRefConfig == null || fieldRefField == null) {
            return;
        }

        if (value == null || value.equals("")) {
            return;
        }

        String fieldRefs = fieldRefConfig.getName() + "." + fieldRefField.getName();

        IndexDefinition fieldRefIndex = fieldRefConfig.getIndexByField1(fieldRefField);
        Map refIndexedJsons = allConfigIndexedJsons.get(fieldRefConfig).get(fieldRefIndex);

        if (refIndexedJsons == null || !refIndexedJsons.containsKey(value)) {
            String error;
            String keyOrValue = "";
            if (field.isCollectionType()) {
                keyOrValue = mapKey ? "键" : "值";
            }
            if (bean instanceof ConfigDefinition) {
                String format = "配置[%s]的第%s行[%s]的%s引用[%s]数据[%s]不存在";
                error = String.format(format, position.getLeft(), position.getMiddle(), position.getRight(), keyOrValue, fieldRefs, value);
            } else {
                String format = "配置[%s]第%s行[%s]的对象[%s]字段[%s]%s引用[%s]数据[%s]不存在";
                error = String.format(format, position.getLeft(), position.getMiddle(), position.getRight(), bean.getName(), field.getName(), keyOrValue, fieldRefs, value);
            }
            validatedErrors.add(error);
        }
    }

    private void validateBeanTypeRef(Triple position, BeanDefinition bean, JSONObject json,
                                     Map<ConfigDefinition, Map<IndexDefinition, Map>> allConfigIndexedJsons) {
        if (json != null) {
            for (FieldDefinition field : bean.getFields()) {
                Object fieldValue = json.get(field.getName());
                validateFieldRef(position, bean, field, fieldValue, allConfigIndexedJsons);
            }
        }
    }

}
