package quan.config.definition;

import org.apache.commons.lang3.StringUtils;
import quan.config.definition.parser.DefinitionParser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 各种【定义】的抽象类
 */
public abstract class Definition {

    public static final int KIND_ENUM = 1;

    public static final int KIND_BEAN = 2;

    public static final int KIND_MESSAGE = 3;

    public static final int KIND_FIELD = 4;

    public static final int KIND_DATA = 5;

    public static final int KIND_CONFIG = 6;

    public static final int KIND_INDEX = 7;

    public static final int KIND_CONSTANT = 8;


    private String name;

    protected String underscoreName;

    private String comment = "";

    protected DefinitionParser parser;

    /**
     * 附加动态参数
     */
    private Map<String, Object> params = new HashMap<>();

    public Definition() {
    }

    public void setParser(DefinitionParser parser) {
        this.parser = parser;
    }

    public DefinitionParser getParser() {
        return parser;
    }

    public String getName() {
        return name;
    }

    public String getValidationName() {
        return getValidationName("");
    }

    public String getValidationName(String append) {
        String kindName = getKindName();
        String validationName;

        if (name != null) {
            validationName = kindName + "[" + name + "]" + append;
        } else {
            validationName = kindName + append;
        }

        return validationName;
    }

    public void setName(String name) {
        if (!StringUtils.isBlank(name)) {
            this.name = name.trim();
        }
    }

    public String getUnderscoreName() {
        return underscoreName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        if (!StringUtils.isBlank(comment)) {
            this.comment = comment.trim();
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public abstract int getKind();

    public abstract String getKindName();

    public abstract Pattern getNamePattern();

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "name='" + name + '\'' +
                '}';
    }


}

