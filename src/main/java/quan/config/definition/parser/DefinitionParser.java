package quan.config.definition.parser;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ClassDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 【定义】解析器
 */
public abstract class DefinitionParser {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 包名前缀
     */
    protected String packagePrefix;

    /**
     * 定义文件编码
     */
    protected String definitionEncoding;

    protected LinkedHashSet<String> definitionPaths = new LinkedHashSet<>();

    protected LinkedHashSet<File> definitionFiles = new LinkedHashSet<>();

    /**
     * 定义文件的相对路径名
     */
    protected Map<File, String> definitionFilePaths = new HashMap<>();

    private Pattern enumNamePattern;

    private Pattern beanNamePattern;

    private Pattern configNamePattern;

    //配置常量类名格式
    private Pattern constantNamePattern;

    //解析出来的类定义，还未校验类名
    protected List<ClassDefinition> parsedClasses = new ArrayList<>();

    //key:长类名
    private Map<String, ClassDefinition> longName2Classes = new HashMap<>();

    //key:短类名
    private Map<String, Set<ClassDefinition>> shortName2Classes = new HashMap<>();

    //校验出的错误信息
    private LinkedHashSet<String> validatedErrors = new LinkedHashSet<>();

    //key:表格文件名
    private Map<String, ConfigDefinition> tableConfigs = new HashMap<>();


    public void setDefinitionEncoding(String definitionEncoding) {
        if (!StringUtils.isBlank(definitionEncoding)) {
            this.definitionEncoding = definitionEncoding;
        }
    }

    public String getDefinitionEncoding() {
        return definitionEncoding;
    }

    public void setDefinitionPaths(Collection<String> definitionPaths) {
        for (String path : definitionPaths) {
            this.definitionPaths.add(path);
            Path definitionPath = Paths.get(path);

            Set<File> definitionFiles = FileUtils.listFiles(new File(path));
            this.definitionFiles.addAll(definitionFiles);
            for (File definitionFile : definitionFiles) {
                Path relativizedPath = definitionPath.relativize(Paths.get(definitionFile.getPath()));
                definitionFilePaths.put(definitionFile, relativizedPath.toString());
            }
        }
    }

    public void setDefinitionPath(String definitionPath) {
        setDefinitionPaths(Collections.singletonList(definitionPath));
    }

    public void setPackagePrefix(String packagePrefix) {
        if (packagePrefix != null) {
            this.packagePrefix = packagePrefix;
        }
    }


    public LinkedHashSet<String> getDefinitionPaths() {
        return definitionPaths;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public Pattern getEnumNamePattern() {
        return enumNamePattern;
    }

    public Pattern getBeanNamePattern() {
        return beanNamePattern;
    }

    public Pattern getConfigNamePattern() {
        return configNamePattern;
    }

    public Pattern getConstantNamePattern() {
        return constantNamePattern;
    }

    public void setEnumNamePattern(String enumNamePattern) {
        if (!StringUtils.isBlank(enumNamePattern)) {
            this.enumNamePattern = Pattern.compile(enumNamePattern);
        }
    }

    public void setBeanNamePattern(String beanNamePattern) {
        if (!StringUtils.isBlank(beanNamePattern)) {
            this.beanNamePattern = Pattern.compile(beanNamePattern);
        }
    }

    public void setConfigNamePattern(String configNamePattern) {
        if (!StringUtils.isBlank(configNamePattern)) {
            this.configNamePattern = Pattern.compile(configNamePattern);
        }
    }

    public void setConstantNamePattern(String constantNamePattern) {
        if (!StringUtils.isBlank(constantNamePattern)) {
            this.constantNamePattern = Pattern.compile(constantNamePattern);
        }
    }

    /**
     * 获取所有的类定义
     */
    public Collection<ClassDefinition> getClassDefinitions() {
        return longName2Classes.values();
    }

    /**
     * 通过长类名获取类定义
     */
    public ClassDefinition getClassDefinition(String longName) {
        return longName2Classes.get(longName);
    }

    /**
     * 通过短类名获取类定义
     */
    public Set<ClassDefinition> getClassDefinitions(String shortName) {
        return shortName2Classes.get(shortName);
    }

    public ClassDefinition getClassDefinition(ClassDefinition owner, String name) {
        ClassDefinition classDefinition = getClassDefinition(ClassDefinition.getLongName(owner, name));
        if (classDefinition == null) {
            classDefinition = getClassDefinition(name);
        }
        return classDefinition;
    }

    public ConfigDefinition getConfigDefinition(String name) {
        ClassDefinition classDefinition = longName2Classes.get(name);
        if (classDefinition instanceof ConfigDefinition) {
            return (ConfigDefinition) classDefinition;
        }
        return null;
    }

    public ConfigDefinition getConfigDefinition(ClassDefinition owner, String name) {
        ConfigDefinition configDefinition = getConfigDefinition(ClassDefinition.getLongName(owner, name));
        if (configDefinition == null) {
            configDefinition = getConfigDefinition(name);
        }
        return configDefinition;
    }

    public Map<String, ConfigDefinition> getConfigDefinitions() {
        return tableConfigs;
    }

    public BeanDefinition getBeanDefinition(String name) {
        ClassDefinition classDefinition = longName2Classes.get(name);
        if (classDefinition instanceof BeanDefinition) {
            return (BeanDefinition) classDefinition;
        }
        return null;
    }

    public BeanDefinition getBeanDefinition(ClassDefinition owner, String name) {
        BeanDefinition beanDefinition = getBeanDefinition(ClassDefinition.getLongName(owner, name));
        if (beanDefinition == null) {
            beanDefinition = getBeanDefinition(name);
        }
        return beanDefinition;
    }

    public void addValidatedError(String error) {
        validatedErrors.add(error);
    }

    public LinkedHashSet<String> getValidatedErrors() {
        return validatedErrors;
    }

    public abstract String getDefinitionType();

    /**
     * 最小表格正文起始行号
     */
    public abstract int getMinTableBodyStartRow();

    public void parse() {
        if (!longName2Classes.isEmpty()) {
            return;
        }

        for (File definitionFile : definitionFiles) {
            if (checkFile(definitionFile)) {
                if (definitionFile.getName().contains(" ")) {
                    addValidatedError(String.format("定义文件[%s]的名字不能带空格", definitionFile));
                }
                try {
                    parseFile(definitionFile);
                } catch (Exception e) {
                    logger.error("定义文件[{}]解析出错", definitionFile, e);
                    addValidatedError(String.format("定义文件[%s]解析出错：%s", definitionFile, e.getMessage()));
                }
            }
        }

        validate();
    }

    protected boolean checkFile(File definitionFile) {
        return definitionFile.getName().endsWith(getDefinitionType());
    }


    protected abstract void parseFile(File definitionFile);

    protected void validate() {
        validateClassName();

        parsedClasses.forEach(ClassDefinition::validate1);
        parsedClasses.forEach(ClassDefinition::validate2);
        parsedClasses.forEach(ClassDefinition::validate3);
    }

    protected void validateClassName() {
        Map<String, ClassDefinition> dissimilarNameClasses = new HashMap<>();

        for (ClassDefinition classDefinition1 : parsedClasses) {
            if (classDefinition1.getName() == null) {
                continue;
            }

            if (shortName2Classes.containsKey(classDefinition1.getName()) && !classDefinition1.isAllowSameName()) {
                for (ClassDefinition classDefinition2 : shortName2Classes.get(classDefinition1.getName())) {
                    validatedErrors.add(classDefinition1.getValidationName("和") + classDefinition2.getValidationName() + "名字相同");
                }
            }

            shortName2Classes.computeIfAbsent(classDefinition1.getName(), k -> new HashSet<>()).add(classDefinition1);

            ClassDefinition classDefinition3 = longName2Classes.get(classDefinition1.getLongName());
            if (classDefinition3 == null) {
                longName2Classes.put(classDefinition1.getLongName(), classDefinition1);
            } else {
                validatedErrors.add(classDefinition1.getValidationName("和") + classDefinition3.getValidationName() + "名字相同");
            }

            ClassDefinition classDefinition4 = dissimilarNameClasses.get(classDefinition1.getLongName().toLowerCase());
            if (classDefinition4 == null) {
                dissimilarNameClasses.put(classDefinition1.getLongName().toLowerCase(), classDefinition1);
            } else if (!classDefinition1.getLongName().equals(classDefinition4.getLongName())) {
                validatedErrors.add(classDefinition1.getValidationName("和") + classDefinition4.getValidationName() + "名字相似");
            }
        }
    }

    public void clear() {
        definitionFilePaths.clear();
        parsedClasses.clear();
        longName2Classes.clear();
        validatedErrors.clear();
        tableConfigs.clear();
    }

    public static DefinitionParser createParser(String definitionType) {
        if (definitionType == null) {
            definitionType = "";
        }

        switch (definitionType.trim()) {
            case "csv":
                return new CSVDefinitionParser();
            case "xls":
            case "xlsx":
                return new ExcelDefinitionParser(definitionType);
            default://xml
                return new XmlDefinitionParser();
        }
    }

}
