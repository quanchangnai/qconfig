package quan.config.generator;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import quan.config.Bean;
import quan.config.Config;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.Language;
import quan.config.load.ConfigLoader;
import quan.config.read.ConfigConverter;

import java.util.Properties;

/**
 * 生成Java代码的配置生成器
 */
public class JavaGenerator extends Generator {

    public JavaGenerator(Properties options) {
        super(options);
        initBasicTypes();
        initClassTypes();
        initClassNames();
    }

    private void initBasicTypes() {
        basicTypes.put("byte", "byte");
        basicTypes.put("bool", "boolean");
        basicTypes.put("short", "short");
        basicTypes.put("int", "int");
        basicTypes.put("long", "long");
        basicTypes.put("float", "float");
        basicTypes.put("double", "double");
        basicTypes.put("string", "String");
        basicTypes.put("set", "Set");
        basicTypes.put("list", "List");
        basicTypes.put("map", "Map");
        basicTypes.put("date", "LocalDate");
        basicTypes.put("time", "LocalTime");
        basicTypes.put("datetime", "LocalDateTime");
    }


    private void initClassTypes() {
        classTypes.put("byte", "Byte");
        classTypes.put("bool", "Boolean");
        classTypes.put("short", "Short");
        classTypes.put("int", "Integer");
        classTypes.put("long", "Long");
        classTypes.put("float", "Float");
        classTypes.put("double", "Double");
        classTypes.put("string", "String");
        classTypes.put("set", "HashSet");
        classTypes.put("list", "ArrayList");
        classTypes.put("map", "HashMap");
        classTypes.put("date", "LocalDate");
        classTypes.put("time", "LocalTime");
        classTypes.put("datetime", "LocalDateTime");
    }

    private void initClassNames() {
        classNames.put("Boolean", "java.lang.Boolean");
        classNames.put("Short", "java.lang.Short");
        classNames.put("Integer", "java.lang.Integer");
        classNames.put("Long", "java.lang.Long");
        classNames.put("Float", "java.lang.Float");
        classNames.put("Double","java.lang.Double");
        classNames.put("String", "java.lang.String");

        classNames.put("Set", "java.util.Set");
        classNames.put("HashSet", "java.util.HashSet");
        classNames.put("List", "java.util.List");
        classNames.put("ArrayList", "java.util.ArrayList");
        classNames.put("Map", "java.util.Map");
        classNames.put("HashMap", "java.util.HashMap");

        classNames.put("Object", "java.lang.Object");
        classNames.put("Class", "java.lang.Class");
        classNames.put("Override", "java.lang.Override");
        classNames.put("SuppressWarnings", "java.lang.SuppressWarnings");

        classNames.put("Objects", "java.lang.Objects");
        classNames.put("Arrays", "java.util.Arrays");
        classNames.put("Collection", "java.util.Collection");
        classNames.put("Collections", "java.util.Collections");
        classNames.put("LocalDate", "java.time.LocalDate");
        classNames.put("LocalTime", "java.time.LocalTime");
        classNames.put("LocalDateTime", "java.time.LocalDateTime");
        classNames.put("Bean", Bean.class.getName());
        classNames.put("Config", Config.class.getName());
        classNames.put("ConfigLoader", ConfigLoader.class.getName());
        classNames.put("ConfigConverter", ConfigConverter.class.getName());
        classNames.put("JSONObject", JSONObject.class.getName());
        classNames.put("JSONArray", JSONArray.class.getName());
    }

    @Override
    protected Language language() {
        return Language.java;
    }

    @Override
    protected void prepareBean(BeanDefinition beanDefinition) {
        beanDefinition.addImport("com.alibaba.fastjson2.*");

        if (beanDefinition instanceof ConfigDefinition) {
            beanDefinition.addImport("java.util.*");
            beanDefinition.addImport(ConfigLoader.class.getName());
        }

        if (beanDefinition.getParent() == null || beanDefinition instanceof ConfigDefinition) {
            beanDefinition.addImport("quan.config.*");
        }

        beanDefinition.getFields().forEach(this::prepareField);
    }

    @Override
    protected void prepareField(FieldDefinition fieldDefinition) {
        super.prepareField(fieldDefinition);
        if (fieldDefinition.isCollectionType() || fieldDefinition.isSimpleRef() && fieldDefinition.getRefIndex().isNormal()) {
            fieldDefinition.getOwner().addImport("java.util.*");
        } else if (fieldDefinition.isTimeType()) {
            fieldDefinition.getOwner().addImport("java.time.*");
            fieldDefinition.getOwner().addImport(ConfigConverter.class.getName());
        }
    }

}
