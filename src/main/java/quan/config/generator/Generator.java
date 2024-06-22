package quan.config.generator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quan.config.TableType;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ClassDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.ConstantDefinition;
import quan.config.definition.DependentSource;
import quan.config.definition.DependentSource.DependentType;
import quan.config.definition.EnumDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.Language;
import quan.config.definition.parser.DefinitionParser;
import quan.config.definition.parser.TableDefinitionParser;
import quan.config.definition.parser.XmlDefinitionParser;
import quan.config.load.DefinitionConfigLoader;
import quan.config.read.ConfigConverter;
import quan.config.util.ClassUtils;
import quan.config.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * 代码生成器
 */
public abstract class Generator {

    protected static final Logger logger = LoggerFactory.getLogger(Generator.class);

    /**
     * 生成器选项
     */
    protected Properties options;

    protected Map<String, String> basicTypes = new HashMap<>();

    protected Map<String, String> classTypes = new HashMap<>();

    /**
     * 类的简单名对应全名
     */
    protected Map<String, String> classNames = new HashMap<>();

    protected boolean enable = true;

    /**
     * 是否开启增量生成
     */
    protected boolean incremental;

    protected String definitionEncoding;

    protected Set<String> definitionPaths = new HashSet<>();

    protected String packagePrefix;

    protected String codePath;

    protected DefinitionParser parser;

    protected Configuration freemarkerCfg;

    protected Map<Class<? extends ClassDefinition>, Template> templates = new HashMap<>();

    //配置加载器，用于生成常量
    protected DefinitionConfigLoader configLoader;

    protected String definitionType;

    protected String tableType;

    protected String tablePath;

    protected String tableBodyStartRow;


    //生成或删除代码文件数量
    protected int count;

    //上一次代码生成记录
    protected Map<String, String> oldRecords = new HashMap<>();

    //当前代码生成记录
    protected Map<String, String> newRecords = new HashMap<>();

    protected Set<String> addClasses = new HashSet<>();

    protected Set<String> deleteClasses = new HashSet<>();

    public Generator(Properties options) {
        parseOptions(options);
        if (enable) {
            checkOptions();
        }
    }

    public void setDefinitionPath(Collection<String> definitionPaths) {
        this.definitionPaths.clear();
        this.definitionPaths.addAll(definitionPaths);
    }

    public void setDefinitionPath(String definitionPath) {
        this.definitionPaths.clear();
        this.definitionPaths.add(definitionPath);
    }

    public void setCodePath(String codePath) {
        this.codePath = FileUtils.toPlatPath(codePath);
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public void setTableType(TableType tableType) {
        this.tableType = tableType.name();
    }

    public void setTablePath(String tablePath) {
        this.tablePath = tablePath;
    }

    public void useXmlParser() {
        parser = new XmlDefinitionParser();
        parser.setDefinitionPaths(definitionPaths);
        parseOptions(options);
    }

    public void useXmlParser(String definitionPath) {
        setDefinitionPath(definitionPath);
        useXmlParser();
    }

    public void setParser(DefinitionParser parser) {
        if (parser == null) {
            return;
        }

        this.parser = parser;

        parseOptions(options);

        parser.setDefinitionEncoding(definitionEncoding);

        if (!parser.getDefinitionPaths().isEmpty() && definitionPaths.isEmpty()) {
            definitionPaths.addAll(parser.getDefinitionPaths());
        } else {
            parser.setDefinitionPaths(definitionPaths);
        }
    }

    public DefinitionParser getParser() {
        return parser;
    }


    protected abstract Language language();

    protected boolean support(ClassDefinition classDefinition) {
        return true;
    }

    protected void parseOptions(Properties options) {
        this.options = options;

        String optionPrefix = language() + ".";

        String enable = options.getProperty("enable");
        if (!StringUtils.isBlank(enable)) {
            this.enable = enable.trim().equals("true");
        }

        String incremental = options.getProperty("incremental");
        if (!StringUtils.isBlank(incremental)) {
            this.incremental = incremental.trim().equals("true");
        }

        String definitionPath = options.getProperty("definitionPath");
        if (!StringUtils.isBlank(definitionPath)) {
            definitionPaths.addAll(Arrays.asList(definitionPath.split("[,，]")));
        }

        String definitionEncoding = options.getProperty("definitionEncoding");
        if (!StringUtils.isBlank(definitionEncoding)) {
            this.definitionEncoding = definitionEncoding;
        }

        String codePath = options.getProperty(optionPrefix + "codePath");
        if (!StringUtils.isBlank(codePath)) {
            setCodePath(codePath);
        } else {
            this.enable = false;
        }

        packagePrefix = options.getProperty(optionPrefix + "packagePrefix");

        definitionType = options.getProperty("definitionType");
        if (StringUtils.isBlank(definitionType)) {
            definitionType = "xml";
        }

        tableType = options.getProperty("tableType");
        tablePath = options.getProperty("tablePath");
        tableBodyStartRow = options.getProperty("tableBodyStartRow");

        ConfigConverter.setDateTimePattern(options.getProperty("datetimePattern"));
        ConfigConverter.setDatePattern(options.getProperty("datePattern"));
        ConfigConverter.setTimePattern(options.getProperty("timePattern"));

        if (parser != null) {
            parser.setDefinitionEncoding(definitionEncoding);
            parser.setConfigNamePattern(options.getProperty("configNamePattern"));
            parser.setBeanNamePattern(options.getProperty("beanNamePattern"));
            parser.setEnumNamePattern(options.getProperty("enumNamePattern"));
            parser.setConstantNamePattern(options.getProperty("constantNamePattern"));
            parser.setPackagePrefix(packagePrefix);

            if (parser instanceof TableDefinitionParser) {
                TableDefinitionParser tableDefinitionParser = (TableDefinitionParser) parser;
                for (String language : Language.names()) {
                    String alias = options.getProperty(language + ".alias");
                    if (!StringUtils.isBlank(alias)) {
                        tableDefinitionParser.getLanguageAliases().put(language, alias);
                    }
                }
            }
        }
    }

    /**
     * 检查生成器必须要设置的选项
     */
    protected void checkOptions() {
        if (definitionPaths.isEmpty()) {
            throw new IllegalArgumentException("配置的定义文件路径[definitionPaths]不能为空");
        }

        if (codePath == null) {
            throw new IllegalArgumentException("配置的目标代码[" + language() + "]文件路径[codePath]不能为空");
        }

        if (tableType == null) {
            throw new IllegalArgumentException("配置的表格类型[tableType]不能为空");
        }

        try {
            TableType.valueOf(tableType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("配置的表格类型[tableType]不合法,当前值:" + tableType + ",合法值:" + Arrays.toString(TableType.values()));
        }

        if (StringUtils.isBlank(tablePath)) {
            throw new IllegalArgumentException("配置的表格文件路径[tablePath]不能为空");
        }

        if (parser != null) {
            if (parser instanceof TableDefinitionParser) {
                ((TableDefinitionParser) parser).checkLanguageAlias();
            }

            int minTableBodyStartRow = parser.getMinTableBodyStartRow();
            if (!StringUtils.isBlank(this.tableBodyStartRow)) {
                try {
                    int tableBodyStartRow = Integer.parseInt(this.tableBodyStartRow);
                    Validate.isTrue(tableBodyStartRow >= minTableBodyStartRow);
                } catch (Exception e) {
                    throw new IllegalArgumentException("配置的表格正文开始行号[tableBodyStartRow]不合法，合法值为空值或者大于等于" + minTableBodyStartRow + "的整数");
                }
            } else {
                tableBodyStartRow = String.valueOf(minTableBodyStartRow);
            }
        }
    }

    protected void initFreemarker() {
        freemarkerCfg = new Configuration(Configuration.VERSION_2_3_23);
        freemarkerCfg.setClassForTemplateLoading(getClass(), "");
        freemarkerCfg.setDefaultEncoding("UTF-8");

        try {
            Template enumTemplate = freemarkerCfg.getTemplate("Enum." + language() + ".ftl");
            Template configTemplate = freemarkerCfg.getTemplate("Config." + language() + ".ftl");
            Template constantTemplate = freemarkerCfg.getTemplate("constant." + language() + ".ftl");

            templates.put(EnumDefinition.class, enumTemplate);
            templates.put(BeanDefinition.class, configTemplate);
            templates.put(ConfigDefinition.class, configTemplate);
            templates.put(ConstantDefinition.class, constantTemplate);
        } catch (IOException e) {
            logger.error("加载Freemarker模板失败", e);
        }
    }

    protected void parseDefinitions() {
        if (parser == null) {
            throw new IllegalArgumentException("配置的定义解析器[definitionParser]不能为空");
        }
        parser.setPackagePrefix(packagePrefix);
        parser.parse();
    }

    public void generate() {
        generate(true);
    }

    /**
     * 初始化配置加载器，读取常量key用于常量类生成
     */
    protected void initConfigLoader(TableType tableType, String tablePath) {
        if (parser == null) {
            throw new IllegalArgumentException("配置的定义解析器[definitionParser]不能为空");
        }

        int tableBodyStartRow = 0;
        if (!StringUtils.isBlank(this.tableBodyStartRow)) {
            tableBodyStartRow = Integer.parseInt(this.tableBodyStartRow);
        }

        configLoader = new DefinitionConfigLoader(tablePath);
        configLoader.setParser(parser);
        configLoader.setTableType(tableType);
        configLoader.setTableBodyStartRow(tableBodyStartRow);
        configLoader.setTableEncoding(options.getProperty("tableEncoding"));
        configLoader.setTableTag(options.getProperty("tableTag"));
    }

    public void generate(boolean printErrors) {
        if (!enable) {
            return;
        }

        checkOptions();
        parseDefinitions();
        initConfigLoader(TableType.valueOf(tableType), tablePath);

        if (!parser.getValidatedErrors().isEmpty()) {
            if (printErrors) {
                printErrors();
            }
            return;
        }

        if (parser.getClassDefinitions().isEmpty()) {
            return;
        }

        initFreemarker();

        readRecords();

        List<ClassDefinition> classDefinitions = new ArrayList<>();
        for (ClassDefinition classDefinition : parser.getClassDefinitions()) {
            if (support(classDefinition) && classDefinition.isSupportedLanguage(this.language())) {
                classDefinition.reset();
                prepareClass(classDefinition);
                classDefinitions.add(classDefinition);
            }
        }

        generate(classDefinitions);

        oldRecords.keySet().forEach(this::delete);

        writeRecords();

        logger.info("生成{}配置代码完成\n", language());
    }

    @SuppressWarnings("unchecked")
    private void readRecords() {
        try {
            File recordsFile = new File(".records", getClass().getSimpleName() + ".json");
            if (recordsFile.exists()) {
                oldRecords = JSON.parseObject(new String(Files.readAllBytes(recordsFile.toPath())), HashMap.class);
            }
        } catch (IOException e) {
            logger.error("read records error ", e);
        }

    }

    protected void writeRecords() {
        try {
            File recordsPath = new File(".records");
            File recordsFile = new File(recordsPath, getClass().getSimpleName() + ".json");
            if (recordsPath.exists() || recordsPath.mkdirs()) {
                JSON.writeJSONString(new FileWriter(recordsFile), newRecords, SerializerFeature.PrettyFormat);
            }
        } catch (IOException e) {
            logger.error("write records error ", e);
        }

        oldRecords.clear();
        newRecords.clear();
    }

    protected void putRecord(ClassDefinition classDefinition) {
        String fullName = classDefinition.getFullName();

        if (oldRecords.remove(fullName) == null) {
            addClasses.add(fullName);
        }

        newRecords.put(fullName, classDefinition.getVersion());
    }

    /**
     * 删除失效的代码文件
     */
    protected void delete(String fullName) {
        count++;
        deleteClasses.add(fullName);
        File classFile = new File(codePath, fullName.replace(".", File.separator) + "." + language());
        if (classFile.delete()) {
            logger.error("删除配置[{}]完成", classFile);
        } else {
            logger.error("删除配置[{}]失败", classFile);
        }
    }

    protected void generate(List<ClassDefinition> classDefinitions) {
        classDefinitions.forEach(this::generate);
    }

    protected boolean checkChanged(ClassDefinition classDefinition) {
        if (incremental) {
            String fullName = classDefinition.getFullName();
            String version = classDefinition.getVersion();
            return !version.equals(oldRecords.get(fullName));
        } else {
            return true;
        }
    }

    protected void generate(ClassDefinition classDefinition) {
        if (!checkChanged(classDefinition)) {
            putRecord(classDefinition);
            return;
        }

        String fullPackageName = classDefinition.getFullPackageName();
        File packagePath;

        if (StringUtils.isBlank(fullPackageName)) {
            packagePath = new File(codePath);
        } else {
            packagePath = new File(codePath, fullPackageName.replace(".", File.separator));
        }

        File classFile = new File(packagePath, classDefinition.getName() + "." + language());

        if (!packagePath.exists() && !packagePath.mkdirs()) {
            logger.error("生成配置[{}]失败，无法创建目录[{}]", classFile, packagePath);
            return;
        }

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(classFile.toPath()), StandardCharsets.UTF_8)) {
            count++;
            templates.get(classDefinition.getClass()).process(classDefinition, writer);
        } catch (Exception e) {
            logger.error("生成配置[{}]失败", classFile, e);
            return;
        }

        putRecord(classDefinition);

        logger.info("生成配置[{}]完成", classFile);
    }

    protected void prepareClass(ClassDefinition classDefinition) {
        classDefinition.setCurrentLanguage(language());
        classDefinition.setDependentClassNames(this.classNames);

        if (classDefinition instanceof BeanDefinition) {
            prepareBean((BeanDefinition) classDefinition);
        }

        //不同包下的同名类依赖
        Map<String, TreeMap<DependentSource, ClassDefinition>> dependentsClasses = classDefinition.getDependentsClasses();

        for (String dependentName : dependentsClasses.keySet()) {
            ClassDefinition simpleNameClassDefinition = null;//同名类中只有一个可以使用简单类名
            TreeMap<DependentSource, ClassDefinition> dependentClasses = dependentsClasses.get(dependentName);

            for (DependentSource dependentSource : dependentClasses.keySet()) {
                ClassDefinition dependentClassDefinition = dependentClasses.get(dependentSource);
                String dependentClassFullName = dependentClassDefinition.getFullName();
                Pair<Boolean, Boolean> useDependent = howUseDependent(classDefinition, dependentClassDefinition, simpleNameClassDefinition);

                if (!useDependent.getLeft() && simpleNameClassDefinition == null) {
                    simpleNameClassDefinition = dependentClassDefinition;
                }

                if (useDependent.getRight()) {
                    if (useDependent.getLeft()) {
                        classDefinition.getImports().put(dependentClassDefinition.getOtherImport(), dependentClassFullName);
                    } else {
                        classDefinition.getImports().put(dependentClassDefinition.getOtherImport(), dependentClassDefinition.getName());
                    }
                }

                if (useDependent.getLeft()) {
                    if (dependentSource.getType() == DependentType.FIELD) {
                        ((FieldDefinition) dependentSource.getOwnerDefinition()).setClassType(dependentClassFullName);
                    } else if (dependentSource.getType() == DependentType.FIELD_VALUE) {
                        ((FieldDefinition) dependentSource.getOwnerDefinition()).setValueClassType(dependentClassFullName);
                    } else if (dependentSource.getType() == DependentType.FIELD_REF) {
                        ((FieldDefinition) dependentSource.getOwnerDefinition()).setRefType(dependentClassFullName);
                    } else if (dependentSource.getType() == DependentType.PARENT) {
                        ((BeanDefinition) dependentSource.getOwnerDefinition()).setDependentParentFullName(dependentClassFullName);
                    } else if (dependentSource.getType() == DependentType.CHILD) {
                        ((BeanDefinition) dependentSource.getOwnerDefinition()).getDependentChildren().put(dependentClassDefinition.getLongName(), dependentClassFullName);
                    }
                }
            }
        }

        if (classDefinition instanceof ConstantDefinition) {
            prepareConstant((ConstantDefinition) classDefinition);
        }
    }

    protected void prepareConstant(ConstantDefinition constantDefinition) {
        prepareField(constantDefinition.getValueField());
        if (configLoader != null) {
            constantDefinition.updateVersion(configLoader.getConfigVersion(constantDefinition.getOwnerDefinition(), false));
            if (checkChanged(constantDefinition)) {
                List<JSONObject> configJsons = configLoader.loadJsons(constantDefinition.getOwnerDefinition(), false);
                constantDefinition.setConfigs(configJsons);
            }
        }
    }

    /**
     * 判断依赖类的使用方式
     *
     * @return Pair<使用全类名还是简单类名, 是否使用import或require>
     */
    protected Pair<Boolean, Boolean> howUseDependent(ClassDefinition ownerClassDefinition, ClassDefinition dependentClassDefinition, ClassDefinition simpleNameClassDefinition) {
        Language language = language();
        String fullPackageName = ownerClassDefinition.getFullPackageName();
        String dependentFullPackageName = dependentClassDefinition.getFullPackageName();

        if (language == Language.java) {
            if (ownerClassDefinition.getName().equals(dependentClassDefinition.getName())) {
                return Pair.of(true, false);
            } else if (simpleNameClassDefinition == null) {
                return fullPackageName.equals(dependentFullPackageName) ? Pair.of(false, false) : Pair.of(false, true);
            } else {
                return dependentClassDefinition == simpleNameClassDefinition ? Pair.of(false, false) : Pair.of(true, false);
            }
        } else if (language == Language.lua) {
            //lua
            boolean require = !(dependentClassDefinition instanceof EnumDefinition);
            if (simpleNameClassDefinition == null || simpleNameClassDefinition == dependentClassDefinition) {
                return Pair.of(false, require);
            }
            return Pair.of(true, require);
        } else {
            //不应该走到这里
            throw new IllegalArgumentException(language.toString());
        }
    }

    protected void prepareBean(BeanDefinition beanDefinition) {
        beanDefinition.getFields().forEach(this::prepareField);
    }

    protected void prepareField(FieldDefinition fieldDefinition) {
        ClassDefinition owner = fieldDefinition.getOwner();
        String fieldType = fieldDefinition.getType();
        if (fieldDefinition.isBuiltinType()) {
            fieldDefinition.setBasicType(owner.getDependentName(basicTypes.get(fieldType)));
            fieldDefinition.setClassType(owner.getDependentName(classTypes.get(fieldType)));
        }

        if (fieldDefinition.isCollectionType()) {
            if (fieldType.equals("map") && fieldDefinition.isBuiltinKeyType()) {
                String fieldKeyType = fieldDefinition.getKeyType();
                fieldDefinition.setKeyBasicType(owner.getDependentName(basicTypes.get(fieldKeyType)));
                fieldDefinition.setKeyClassType(owner.getDependentName(classTypes.get(fieldKeyType)));
            }

            String fieldValueType = fieldDefinition.getValueType();
            if (fieldDefinition.isBuiltinValueType()) {
                fieldDefinition.setValueBasicType(owner.getDependentName(basicTypes.get(fieldValueType)));
                fieldDefinition.setValueClassType(owner.getDependentName(classTypes.get(fieldValueType)));
            }
        }
    }

    protected void printErrors() {
        if (parser == null) {
            return;
        }

        LinkedHashSet<String> errors = parser.getValidatedErrors();
        if (errors.isEmpty()) {
            return;
        }

        logger.error("生成配置代码失败，路径{}下的定义文件共发现{}条错误", parser.getDefinitionPaths(), errors.size());

        int i = 0;
        for (String error : errors) {
            logger.error("{}{}", error, ++i == errors.size() ? "\n" : "");
        }
    }

    /**
     * 执行代码生成
     *
     * @param optionsFileName 选项文件名为空时使用默认文件
     * @param extraOptions    附加的选项会覆盖选项文件里的选项
     * @return 成功或失败，部分成功也会返回false
     */
    public static boolean generate(String optionsFileName, Properties extraOptions) {
        long startTime = System.currentTimeMillis();
        Properties options = new Properties();

        if (!StringUtils.isBlank(optionsFileName)) {
            File optionsFile = new File(optionsFileName);
            try (InputStream inputStream = Files.newInputStream(optionsFile.toPath())) {
                options.load(inputStream);
                logger.info("加载生成器选项配置文件成功：{}\n", optionsFile.getCanonicalPath());
            } catch (IOException e) {
                logger.error("加载生成器选项配置文件出错", e);
                return false;
            }
        }

        if (extraOptions != null) {
            options.putAll(extraOptions);
        }

        boolean success = generate(options);

        logger.info("生成完成，耗时{}s", (System.currentTimeMillis() - startTime) / 1000D);
        return success;
    }


    public static void generate(String optionsFile) {
        if (StringUtils.isBlank(optionsFile)) {
            optionsFile = "generator.properties";
        }
        generate(optionsFile, null);
    }

    private static boolean generate(Properties options) {
        List<Generator> generators = new ArrayList<>();

        String definitionType = options.getProperty("definitionType");
        DefinitionParser parser = DefinitionParser.createParser(definitionType);

        for (Class<?> clazz : ClassUtils.loadClasses(Generator.class.getPackage().getName(), Generator.class, false, null)) {
            try {
                Generator generator = (Generator) clazz.getConstructor(Properties.class).newInstance(options);
                generator.setParser(parser);
                generators.add(generator);
            } catch (Exception ignored) {
            }
        }

        boolean success = true;

        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            generator.generate(i == generators.size() - 1);
            if (generator.getParser() != null) {
                success &= generator.getParser().getValidatedErrors().isEmpty();
            }
        }

        return success;
    }


    private static Properties getExtraOptions(String[] args) {
        Properties extraOptions = new Properties();

        for (String arg : args) {
            if (!arg.startsWith("--")) {
                continue;
            }
            String option = arg.substring(2);
            String optionKey = option;
            String optionValue = "true";
            if (option.contains("=")) {
                optionKey = option.substring(0, option.indexOf("="));
                optionValue = option.substring(option.indexOf("=") + 1);
            }
            extraOptions.put(optionKey, optionValue);
        }

        return extraOptions;
    }

    public static void main(String[] args) {
        String optionsFile = "";
        if (args.length > 0 && !args[0].startsWith("--")) {
            optionsFile = args[0];
        }

        boolean exit1OnFail = false;
        if (args.length > 1 && !args[1].startsWith("--")) {
            exit1OnFail = Boolean.parseBoolean(args[1]);
        }

        Properties extraOptions = getExtraOptions(args);

        if (!generate(optionsFile, extraOptions) && exit1OnFail) {
            System.exit(1);
        }
    }

}
