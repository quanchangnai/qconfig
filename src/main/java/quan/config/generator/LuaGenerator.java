package quan.config.generator;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ClassDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * 生成Lua代码的配置生成器
 */
public class LuaGenerator extends Generator {

    /**
     * 自定义的配置数据读取器，为空时直接生成配置数据到代码文件里
     */
    private String configReader;

    private Map<String, String> configVersions = new HashMap<>();

    public LuaGenerator(Properties options) {
        super(options);
    }

    @Override
    protected Language language() {
        return Language.lua;
    }

    @Override
    protected void parseParams(Properties params) {
        super.parseParams(params);
        configReader = params.getProperty("lua.configReader");
    }

    @Override
    protected boolean support(ClassDefinition classDefinition) {
        if (classDefinition.getClass() == BeanDefinition.class) {
            return false;
        } else {
            return super.support(classDefinition);
        }
    }

    @Override
    protected void prepareBean(BeanDefinition beanDefinition) {
        super.prepareBean(beanDefinition);

        if (beanDefinition instanceof ConfigDefinition) {
            ConfigDefinition configDefinition = (ConfigDefinition) beanDefinition;
            if (!StringUtils.isBlank(configReader)) {
                configDefinition.getParams().put("configReader", configReader);
                configVersions.put(configDefinition.getFullName(), DigestUtils.md5Hex(configReader + configDefinition.getVersion()));
            } else if (configLoader != null) {
                configVersions.put(configDefinition.getFullName(), DigestUtils.md5Hex(configReader + configLoader.getConfigVersion(configDefinition, false)));
                if (checkChanged(configDefinition)) {
                    List<String> configs = new ArrayList<>();
                    configDefinition.getParams().put("configs", configs);
                    List<JSONObject> configJsons = configLoader.loadJsons(configDefinition, true);
                    for (JSONObject configJson : configJsons) {
                        configs.add(configLuaString(configDefinition, configJson));
                    }
                }
            }
        }
    }

    @Override
    protected String getVersion(ClassDefinition classDefinition) {
        if (classDefinition instanceof ConfigDefinition && configVersions.containsKey(classDefinition.getFullName())) {
            return configVersions.get(classDefinition.getFullName());
        } else {
            return classDefinition.getVersion();
        }
    }

    private String configLuaString(ConfigDefinition configDefinition, JSONObject object) {
        StringBuilder luaBuilder = new StringBuilder();
        beanLuaString(configDefinition, configDefinition, object, luaBuilder);
        return luaBuilder.toString();
    }

    private void beanLuaString(ConfigDefinition configDefinition, BeanDefinition beanDefinition, JSONObject object, StringBuilder luaBuilder) {
        if (object == null) {
            luaBuilder.append("nil");
            return;
        }

        String clazz = object.getString("class");
        if (!StringUtils.isEmpty(clazz)) {
            beanDefinition = parser.getBeanDefinition(configDefinition, clazz);
        }

        if (beanDefinition == null) {
            luaBuilder.append("nil");
            return;
        }

        luaBuilder.append("{ ");

        if (!StringUtils.isEmpty(clazz)) {
            luaBuilder.append("class = ").append("\"").append(clazz).append("\", ");
        }

        boolean start = true;
        for (FieldDefinition field : beanDefinition.getFields()) {
            if (!field.isSupportedLanguage(this.language())) {
                continue;
            }
            if (!start) {
                luaBuilder.append(", ");
            }
            start = false;

            luaBuilder.append(field.getName()).append(" = ");

            if (field.isStringType() || field.isTimeType()) {
                luaBuilder.append("\"").append(object.getOrDefault(field.getName(), "")).append("\"");
            } else if (field.isNumberType()) {
                luaBuilder.append(object.getOrDefault(field.getName(), "0"));
            } else if (field.isMapType()) {
                mapLuaString(configDefinition, field, object.getJSONObject(field.getName()), luaBuilder);
            } else if (field.isListType() || field.isSetType()) {
                arrayLuaString(configDefinition, field, object.getJSONArray(field.getName()), luaBuilder);
            } else if (field.isBeanType()) {
                beanLuaString(configDefinition, field.getTypeBean(), object.getJSONObject(field.getName()), luaBuilder);
            } else {
                luaBuilder.append(object.getOrDefault(field.getName(), "nil"));
            }

        }

        luaBuilder.append(" }");
    }

    private void mapLuaString(ConfigDefinition configDefinition, FieldDefinition fieldDefinition, JSONObject object, StringBuilder luaBuilder) {
        if (object == null) {
            luaBuilder.append("{ }");
            return;
        }

        luaBuilder.append("{ ");
        boolean start = true;

        for (String key : object.keySet()) {
            if (!start) {
                luaBuilder.append(", ");
            }
            start = false;

            if (fieldDefinition.isStringKeyType()) {
                luaBuilder.append(key);
            } else {
                luaBuilder.append("[").append(key).append("]");
            }

            luaBuilder.append(" = ");

            if (fieldDefinition.isStringValueType()) {
                luaBuilder.append("\"").append(object.getString(key)).append("\"");
            } else if (fieldDefinition.isBeanValueType()) {
                beanLuaString(configDefinition, fieldDefinition.getValueTypeBean(), object.getJSONObject(key), luaBuilder);
            } else {
                luaBuilder.append(object.get(key));
            }
        }

        luaBuilder.append(" }");
    }

    private void arrayLuaString(ConfigDefinition configDefinition, FieldDefinition fieldDefinition, JSONArray array, StringBuilder luaBuilder) {
        if (array == null) {
            luaBuilder.append("{ }");
            return;
        }

        luaBuilder.append("{ ");
        boolean start = true;

        for (int i = 0; i < array.size(); i++) {
            if (!start) {
                luaBuilder.append(", ");
            }
            start = false;
            if (fieldDefinition.isStringValueType()) {
                luaBuilder.append("\"").append(array.getString(i)).append("\"");
            } else if (fieldDefinition.isBeanValueType()) {
                beanLuaString(configDefinition, fieldDefinition.getValueTypeBean(), array.getJSONObject(i), luaBuilder);
            } else {
                luaBuilder.append(array.get(i));
            }
        }

        luaBuilder.append(" }");
    }

}
