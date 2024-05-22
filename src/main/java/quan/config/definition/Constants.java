package quan.config.definition;

import quan.config.util.CollectionUtils;

import java.util.Set;
import java.util.regex.Pattern;

import static quan.config.util.CollectionUtils.asSet;

/**
 * 通用常量
 */
public final class Constants {

    /**
     * 整数类型
     */
    public static final Set<String> INTEGRAL_NUMBER_TYPES = asSet("short", "int", "long");

    /**
     * 数字类型
     */
    public static final Set<String> NUMBER_TYPES = asSet(INTEGRAL_NUMBER_TYPES, "float", "double");

    /**
     * 原生类型
     */
    public static final Set<String> PRIMITIVE_TYPES = asSet(NUMBER_TYPES, "bool", "string");

    /**
     * 集合类型
     */
    public static final Set<String> COLLECTION_TYPES = asSet("list", "set", "map");

    /**
     * 时间类型
     */
    public static final Set<String> TIME_TYPES = asSet("date", "time", "datetime");

    /**
     * 内建类型
     */
    public static final Set<String> BUILTIN_TYPES = asSet(PRIMITIVE_TYPES, COLLECTION_TYPES, TIME_TYPES);

    /**
     * 合法的分隔符
     */
    public static final Set<String> LEGAL_DELIMITERS = asSet(",", ";", ":", "-", "_", "*", "|", "$", "@", "&", "?");

    /**
     * 需要转义的分隔符(正则表达式特殊字符)
     */
    public static final Set<String> NEED_ESCAPE_DELIMITERS = asSet("-", "*", "|", "$", "?");

    /**
     * Java保留字
     */
    public static final Set<String> JAVA_RESERVED_WORDS = CollectionUtils.asSet(
            "abstract", "assert", "boolean", "break", "throws", "case", "catch", "char", "volatile",
            "const", "continue", "default", "do", "else", "enum", "extends", "finally", "long", "transient",
            "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "double",
            "native", "new", "try", "package", "private", "protected", "public", "void", "strictfp", "short",
            "static", "super", "switch", "synchronized", "throw", "byte", "final", "while", "class", "return"
    );


    /**
     * Lua保留字
     */
    public static final Set<String> LUA_RESERVED_WORDS = CollectionUtils.asSet(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto", "if", "in",
            "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while"
    );

    /**
     * 首字母小写包名格式
     */
    public static final Pattern LOWER_PACKAGE_NAME_PATTERN = Pattern.compile("[a-z][a-z\\d]*(\\.[a-z][a-z\\d]*)*");

    /**
     * 首字母大写包名格式
     */
    public static final Pattern UPPER_PACKAGE_NAME_PATTERN = Pattern.compile("[A-Z][a-z\\d]*(\\.[A-Z][a-z\\d]*)*");

}
