package quan.config.read;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quan.config.Config;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.FieldDefinition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置读取器
 */
@SuppressWarnings({"unchecked"})
public abstract class ConfigReader {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String tableTag;

    private File tableFile;

    protected String tableEncoding;

    private ConfigDefinition configDefinition;

    private ConfigConverter converter;

    /**
     * 表格正文开始行号(从1开始计数)
     */
    protected int tableBodyStartRow = 2;

    protected Config prototype;

    protected List<JSONObject> jsons = new ArrayList<>();

    private LinkedHashSet<String> initErrors = new LinkedHashSet<>();

    private LinkedHashSet<String> validatedErrors = new LinkedHashSet<>();

    private final List<Config> configs = new ArrayList<>();

    public ConfigReader(File tableFile, ConfigDefinition configDefinition) {
        this.tableFile = tableFile;
        this.configDefinition = configDefinition;

        if (configDefinition != null) {
            converter = new ConfigConverter(configDefinition.getParser());
            initPrototype(configDefinition.getFullName());
        }
    }

    protected void initPrototype(String configFullName) {
        try {
            Class<Config> configClass = (Class<Config>) Class.forName(configFullName);
            prototype = configClass.getDeclaredConstructor(JSONObject.class).newInstance(new JSONObject());
        } catch (Throwable e) {
            initErrors.add(String.format("实例化配置类[%s]失败:%s", configFullName, e));
        }
    }

    public String getTableTag() {
        return tableTag;
    }

    public void setTableTag(String tableTag) {
        this.tableTag = tableTag;
    }

    public File getTableFile() {
        return tableFile;
    }

    public String getTableEncoding() {
        return tableEncoding;
    }

    public void setTableEncoding(String tableEncoding) {
        this.tableEncoding = tableEncoding;
    }

    public Config getPrototype() {
        return prototype;
    }

    public ConfigReader setTableBodyStartRow(int tableBodyStartRow) {
        this.tableBodyStartRow = tableBodyStartRow;
        return this;
    }

    public int getTableBodyStartRow() {
        return tableBodyStartRow;
    }

    public ConfigDefinition getConfigDefinition() {
        return configDefinition;
    }

    public List<JSONObject> getJsons() {
        if (!StringUtils.isBlank(tableTag) && !tableFile.getName().contains("@")) {
            StringBuilder tagTableFilePath = new StringBuilder(tableFile.getPath());
            tagTableFilePath.insert(tagTableFilePath.lastIndexOf("."), "@" + tableTag);
            File tagTableFile = new File(tagTableFilePath.toString());
            if (tagTableFile.exists()) {
                tableFile = tagTableFile;
            }
        }

        if (jsons.isEmpty()) {
            if (tableFile.exists()) {
                read();
            } else {
                logger.error("配置文件[{}]不存在", tableFile);
            }
        }

        return jsons;
    }


    public List<Config> getConfigs() {
        if (prototype == null || !configs.isEmpty()) {
            return configs;
        }

        getJsons();

        for (JSONObject json : jsons) {
            configs.add(prototype.create(json));
        }

        return configs;
    }

    public List<String> getValidatedErrors() {
        List<String> errors = new ArrayList<>();
        errors.addAll(initErrors);
        errors.addAll(validatedErrors);
        return errors;
    }

    protected abstract void read();

    public void clear() {
        jsons.clear();
        configs.clear();
        validatedErrors.clear();
    }

    protected void validateColumnNames(List<String> columns) {
        configDefinition.getFields().forEach(f -> f.getColumnNums().clear());

        Set<FieldDefinition> missingFields = new HashSet<>(configDefinition.getFields());
        Set<String> validatedColumns = new HashSet<>();

        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            FieldDefinition fieldDefinition = configDefinition.getColumnFields().get(columnName);
            if (fieldDefinition == null) {
                continue;
            }

            if (validatedColumns.contains(columnName) && !fieldDefinition.isCollectionType() && !fieldDefinition.isBeanType()) {
                validatedErrors.add(String.format("配置[%s]的%s类型[%s]不支持对应多列[%s]", tableFile.getName(), fieldDefinition.getValidatedName(), fieldDefinition.getType(), columnName));
            }

            missingFields.remove(fieldDefinition);
            validatedColumns.add(columnName);
            fieldDefinition.getColumnNums().add(i + 1);
        }

        for (FieldDefinition fieldDefinition : configDefinition.getFields()) {
            BeanDefinition fieldBean = fieldDefinition.getTypeBean();
            if (!fieldDefinition.isLegalColumnCount() && fieldBean != null) {
                validatedErrors.add(String.format("配置[%s]的%s对应列数非法，要么单独对应1列，要么按%s的字段数量拆开成%s列", tableFile.getName(), fieldDefinition.getValidatedName(), fieldBean.getValidatedName(), fieldBean.getFields().size()));
            }
            if (!fieldDefinition.isLegalColumnCount() && fieldDefinition.isMapType()) {
                validatedErrors.add(String.format("配置[%s]的字段类型[%s]对应列数非法，要么单独对应1列，要么按键值对拆开成偶数列", tableFile.getName(), fieldDefinition.getType()));
            }
        }

        for (FieldDefinition fieldDefinition : missingFields) {
            validatedErrors.add(String.format("配置[%s]缺少%s对应的列[%s]", tableFile.getName(), fieldDefinition.getValidatedName(), fieldDefinition.getColumn()));
        }
    }

    protected void addColumnToRow(JSONObject rowJson, String columnName, String columnValue, int row, int column) {
        FieldDefinition fieldDefinition = configDefinition.getColumnFields().get(columnName);
        if (fieldDefinition == null) {
            return;
        }
        if (!fieldDefinition.isLegalColumnCount()) {
            //Bean或者map类型字段对应的列数不合法
            return;
        }

        String fieldName = fieldDefinition.getName();
        String fieldType = fieldDefinition.getType();
        Object fieldValue;

        String columnStr = buildColumnStr(column);

        try {
            if (fieldDefinition.isBeanType()) {
                fieldValue = converter.convertColumnBean(fieldDefinition, rowJson.getJSONObject(fieldDefinition.getName()), columnValue, column);
            } else if (fieldDefinition.isMapType()) {
                fieldValue = converter.convertColumnMap(fieldDefinition, rowJson, columnValue);
            } else if (fieldType.equals("list") || fieldType.equals("set")) {
                fieldValue = converter.convertColumnArray(fieldDefinition, rowJson, columnValue);
            } else {
                fieldValue = converter.convert(fieldDefinition, columnValue);
            }
        } catch (Exception e) {
            handleConvertException(e, columnName, columnValue, row, columnStr);
            return;
        }

        boolean constantKeyField = configDefinition.isConstantKeyField(fieldDefinition);
        if (fieldValue == null) {
            //索引字段不能为空，常量key除外
            if (configDefinition.isIndexField(fieldDefinition) && !constantKeyField) {
                validatedErrors.add(String.format("配置[%s]的第[%d]行第[%s]列[%s]的索引值不能为空", tableFile.getName(), row, columnStr, columnName));
            }
            //没有默认值的必填字段校验
            boolean cannotNull = fieldDefinition.isEnumType() || fieldDefinition.isTimeType() || fieldDefinition.isBeanType() && fieldDefinition.getColumnNums().size() == 1;
            if (!fieldDefinition.isOptional() && cannotNull) {
                validatedErrors.add(String.format("配置[%s]的第[%d]行第[%s]列[%s]不能为空", tableFile.getName(), row, columnStr, columnName));
            }
            return;
        } else {
            //必填字段校验
            if (fieldDefinition.isBeanType() && column == fieldDefinition.getLastColumnNum() && ((JSONObject) fieldValue).isEmpty()) {
                rowJson.remove(fieldName);
                if (!fieldDefinition.isOptional()) {
                    StringBuilder columnsStr = new StringBuilder();
                    for (int columnNum : fieldDefinition.getColumnNums()) {
                        if (columnNum != fieldDefinition.getColumnNums().get(0)) {
                            columnsStr.append(",");
                        }
                        columnsStr.append(buildColumnStr(columnNum));
                    }
                    validatedErrors.add(String.format("配置[%s]的第[%d]行第[%s]列[%s]不能为空", tableFile.getName(), row, columnsStr, columnName));
                }
                return;
            }
        }

        if (constantKeyField && !FieldDefinition.NAME_PATTERN.matcher(fieldValue.toString()).matches()) {
            validatedErrors.add(String.format("配置[%s]的第[%d]行第[%s]列[%s]的常量key[%s]格式错误,正确格式:%s", tableFile.getName(), row, column, columnName, fieldValue, FieldDefinition.NAME_PATTERN));
        }

        rowJson.put(fieldName, fieldValue);
    }

    private static String buildColumnStr(int c) {
        if (c < 1) {
            throw new IllegalArgumentException("参数[c]必须大于0");
        }

        StringBuilder s = new StringBuilder();
        int a = c, b;

        do {
            b = (a - 1) % 26 + 1;
            a = (a - 1) / 26;
            s.append((char) ('A' + b - 1));
        } while (a > 0);

        return c + "(" + s.reverse() + ")";
    }

    protected void handleConvertException(Exception e, String columnName, String columnValue, int row, String columnStr) {
        String commonError = String.format("配置[%s]的第[%s]行第[%s]列[%s]数据[%s]错误", tableFile.getName(), row, columnStr, columnName, columnValue);
        if (columnValue.isEmpty() || columnValue.length() > 20) {
            commonError = String.format("配置[%s]的第[%d]行第[%s]列[%s]数据错误", tableFile.getName(), row, columnStr, columnName);
        }
        if (!(e instanceof ConvertException)) {
            validatedErrors.add(commonError);
            return;
        }

        ConvertException e1 = (ConvertException) e;
        switch (e1.getErrorType()) {
            case TYPE_ERROR:
                validatedErrors.add(String.format(commonError + ",[%s]不匹配期望类型[%s]", e1.getParam(0), e1.getParam(1)));
                break;
            case RANGE_ERROR:
                validatedErrors.add(String.format(commonError + ",不在限制范围(%s)之中", e1.getParam(0)));
                break;
            case ENUM_NAME:
                validatedErrors.add(String.format(commonError + ",枚举名[%s]不合法", e1.getParam(0)));
                break;
            case ENUM_VALUE:
                validatedErrors.add(String.format(commonError + ",枚举值[%s]不合法", e1.getParam(0)));
                break;
            case SET_DUPLICATE_VALUE:
                validatedErrors.add(String.format(commonError + ",set不能有重复值%s", e1.getParams()));
                break;
            case MAP_INVALID_KEY:
                validatedErrors.add(String.format(commonError + ",map不能有无效键[%s]", e1.getParam(0)));
                break;
            case MAP_INVALID_VALUE:
                validatedErrors.add(String.format(commonError + ",map不能有无效值[%s]", e1.getParam(0)));
                break;
            case MAP_DUPLICATE_KEY:
                validatedErrors.add(String.format(commonError + ",map不能有重复键[%s]", e1.getParam(0)));
                break;
            default:
                validatedErrors.add(commonError);
                break;
        }
    }

}
