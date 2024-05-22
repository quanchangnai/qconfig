package quan.config.generator;

import quan.config.definition.BeanDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.Language;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

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
        basicTypes.put("date", "LocalDate");
        basicTypes.put("time", "LocalTime");
        basicTypes.put("datetime", "LocalDateTime");
    }

    private void initClassNames() {
        classNames.put("Boolean", Boolean.class.getName());
        classNames.put("Short", Short.class.getName());
        classNames.put("Integer", Integer.class.getName());
        classNames.put("Long", Long.class.getName());
        classNames.put("Float", Float.class.getName());
        classNames.put("Double", Double.class.getName());
        classNames.put("String", String.class.getName());

        classNames.put("Set", Set.class.getName());
        classNames.put("HashSet", HashSet.class.getName());
        classNames.put("List", List.class.getName());
        classNames.put("ArrayList", ArrayList.class.getName());
        classNames.put("Map", Map.class.getName());
        classNames.put("HashMap", HashMap.class.getName());

        classNames.put("Object", Object.class.getName());
        classNames.put("Class", Class.class.getName());
        classNames.put("Override", Override.class.getName());
        classNames.put("SuppressWarnings", SuppressWarnings.class.getName());

        classNames.put("Objects", Objects.class.getName());
        classNames.put("Arrays", Arrays.class.getName());
        classNames.put("Collection", Collection.class.getName());
        classNames.put("Collections", Collections.class.getName());
        classNames.put("LocalDate", LocalDate.class.getName());
        classNames.put("LocalTime", LocalTime.class.getName());
        classNames.put("LocalDateTime", LocalDateTime.class.getName());
        classNames.put("Bean", "quan.config.Bean");
        classNames.put("Config", "quan.config.Config");
        classNames.put("ConfigLoader", "quan.config.load.ConfigLoader");
        classNames.put("ConfigConverter","quan.config.read.ConfigConverter");
        classNames.put("JSONObject", "com.alibaba.fastjson.JSONObject");
        classNames.put("JSONArray", "com.alibaba.fastjson.JSONArray");
    }

    @Override
    protected Language language() {
        return Language.java;
    }

    @Override
    protected void prepareBean(BeanDefinition beanDefinition) {
        if (beanDefinition instanceof ConfigDefinition) {
            beanDefinition.addImport("java.util.*");
            beanDefinition.addImport("quan.config.load.ConfigLoader");
        }
        if (beanDefinition.getParent() == null || beanDefinition instanceof ConfigDefinition) {
            beanDefinition.addImport("quan.config.*");
        }
        beanDefinition.addImport("com.alibaba.fastjson.*");

        super.prepareBean(beanDefinition);
    }

    @Override
    protected void prepareField(FieldDefinition fieldDefinition) {
        super.prepareField(fieldDefinition);
        if (fieldDefinition.isCollectionType() || fieldDefinition.isSimpleRef() && fieldDefinition.getRefIndex().isNormal()) {
            fieldDefinition.getOwner().addImport("java.util.*");
        } else if (fieldDefinition.isTimeType()) {
            fieldDefinition.getOwner().addImport("java.time.*");
            fieldDefinition.getOwner().addImport("quan.config.read.ConfigConverter");
        }
    }

}
