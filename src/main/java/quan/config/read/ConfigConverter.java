package quan.config.read;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ClassDefinition;
import quan.config.definition.EnumDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.parser.DefinitionParser;
import quan.config.read.ConvertException.ErrorType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置转换器
 */
public class ConfigConverter {

    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DefinitionParser parser;

    public ConfigConverter(DefinitionParser parser) {
        this.parser = parser;
    }


    public static DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public static void setDateTimePattern(String pattern) {
        if (!StringUtils.isBlank(pattern)) {
            dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        }
    }

    public static DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    public static void setDatePattern(String pattern) {
        if (!StringUtils.isBlank(pattern)) {
            dateFormatter = DateTimeFormatter.ofPattern(pattern);
        }
    }

    public static DateTimeFormatter getTimeFormatter() {
        return timeFormatter;
    }

    public static void setTimePattern(String pattern) {
        if (!StringUtils.isBlank(pattern)) {
            timeFormatter = DateTimeFormatter.ofPattern(pattern);
        }
    }

    public static LocalDate parseDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        } else {
            return LocalDate.parse(date, dateFormatter);
        }
    }

    public static LocalTime parseTime(String time) {
        if (StringUtils.isBlank(time)) {
            return null;
        } else {
            return LocalTime.parse(time, timeFormatter);
        }
    }

    public static LocalDateTime parseDateTime(String datetime) {
        if (StringUtils.isBlank(datetime)) {
            return null;
        } else {
            return LocalDateTime.parse(datetime, dateTimeFormatter);
        }
    }

    public Object convert(FieldDefinition fieldDefinition, String value) {
        String type = fieldDefinition.getType();
        if (fieldDefinition.isPrimitiveType()) {
            return convertPrimitiveType(fieldDefinition.getType(), value, (Number) fieldDefinition.getMin(), (Number) fieldDefinition.getMax());
        } else if (fieldDefinition.isTimeType()) {
            return validateTimeType(fieldDefinition.getType(), value);
        } else if (type.equals("list")) {
            return convertList(fieldDefinition, value);
        } else if (type.equals("set")) {
            return convertSet(fieldDefinition, value);
        } else if (type.equals("map")) {
            return convertMap(fieldDefinition, value);
        } else if (fieldDefinition.isBeanType()) {
            return convertBean(fieldDefinition.getOwner(), fieldDefinition.getTypeBean(), value);
        } else if (fieldDefinition.isEnumType()) {
            return convertEnumType(fieldDefinition.getEnum(), value);
        }
        return value;
    }

    public Object convertColumnBean(FieldDefinition fieldDefinition, JSONObject object, String columnValue, int columnNum) {
        BeanDefinition beanDefinition = fieldDefinition.getTypeBean();

        //字段对应一列
        if (fieldDefinition.getColumnNums().size() == 1) {
            return convertBean(fieldDefinition.getOwner(), beanDefinition, columnValue);
        }

        //字段对应多列
        if (columnNum == fieldDefinition.getColumnNums().get(0)) {
            if (beanDefinition.hasChildren()) {
                //第1列是类名
                String className = ClassDefinition.getLongName(beanDefinition, columnValue);

                if (beanDefinition.getMeAndDescendants().contains(className)) {
                    object = new JSONObject();
                    object.put("class", columnValue);
                } else if (!StringUtils.isBlank(columnValue)) {
                    throw new ConvertException(ErrorType.TYPE_ERROR, columnValue, beanDefinition.getName());
                }
                return object;
            } else {
                object = new JSONObject();
            }
        }

        if (object == null) {
            return null;
        }

        if (beanDefinition.hasChildren()) {
            String className = object.getString("class");
            beanDefinition = parser.getBeanDefinition(fieldDefinition.getOwner(), className);
        }

        if (beanDefinition == null) {
            return object;
        }

        for (FieldDefinition beanField : beanDefinition.getFields()) {
            if (!object.containsKey(beanField.getName())) {
                Object convertedColumnValue = convert(beanField, columnValue);
                if (convertedColumnValue != null) {
                    object.put(beanField.getName(), convertedColumnValue);
                }
                break;
            }
        }

        return object;
    }

    /**
     * 转换枚举字段，支持枚举值或者枚举名
     */
    private Object convertEnumType(EnumDefinition enumDefinition, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        int enumValue = 0;
        try {
            enumValue = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        FieldDefinition enumField;

        if (enumValue > 0) {
            enumField = enumDefinition.getField(enumValue);
            if (enumField == null) {
                throw new ConvertException(ErrorType.ENUM_VALUE, value);
            }
            return enumValue;
        }

        enumField = enumDefinition.getField(value);
        if (enumField == null) {
            throw new ConvertException(ErrorType.ENUM_NAME, value);
        }

        return Integer.parseInt(enumField.getEnumValue());
    }

    private Object convertPrimitiveType(String type, String value, Number min, Number max) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        Object result;

        try {
            result = parsePrimitiveType(type, value);
        } catch (Exception e) {
            throw new ConvertException(ErrorType.TYPE_ERROR, e, value, type);
        }

        if (result instanceof Number) {
            Number number = (Number) result;
            if (min != null && Double.compare(number.doubleValue(), min.doubleValue()) < 0
                    || max != null && Double.compare(number.doubleValue(), max.doubleValue()) > 0) {
                String range = min == null ? "" : min.toString();
                range += ",";
                range += max == null ? "" : max.toString();
                throw new ConvertException(ErrorType.RANGE_ERROR, range);
            }
        }

        return result;
    }

    private static Object parsePrimitiveType(String type, String value) {
        switch (type) {
            case "bool":
                return Boolean.parseBoolean(value.toLowerCase());
            case "short":
                return Short.parseShort(value);
            case "int":
                return Integer.parseInt(value);
            case "long":
                return Long.parseLong(value);
            case "float":
                return Float.parseFloat(value);
            case "double":
                return Double.parseDouble(value);
            default:
                return value;
        }
    }

    private Object convertPrimitiveType(String type, String value) {
        return convertPrimitiveType(type, value, null, null);
    }

    private String validateTimeType(String type, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            if (type.equals("datetime")) {
                //日期加时间
                dateTimeFormatter.parse(value);
            } else if (type.equals("date")) {
                //纯日期
                dateFormatter.parse(value);
            } else {
                //纯时间
                timeFormatter.parse(value);
            }
            return value;
        } catch (Exception e) {
            throw new ConvertException(ErrorType.COMMON, e);
        }
    }

    private JSONArray convertList(FieldDefinition fieldDefinition, String value) {
        if (StringUtils.isBlank(value)) {
            return new JSONArray();
        }
        String[] values = value.split(fieldDefinition.getEscapedDelimiters().get(0), -1);
        return convertArray(fieldDefinition, values);
    }

    private JSONArray convertSet(FieldDefinition fieldDefinition, String value) {
        if (StringUtils.isBlank(value)) {
            return new JSONArray();
        }

        String[] values = value.split(fieldDefinition.getEscapedDelimiters().get(0), -1);
        Set<String> setValues = new HashSet<>();
        Set<String> duplicateValues = new HashSet<>();

        for (String v : values) {
            if (setValues.contains(v)) {
                duplicateValues.add(v);
            }
            setValues.add(v);
        }

        if (!duplicateValues.isEmpty()) {
            throw new ConvertException(ErrorType.SET_DUPLICATE_VALUE, new ArrayList<>(duplicateValues));
        }

        return convertArray(fieldDefinition, setValues.toArray(new String[0]));
    }

    private JSONArray convertArray(FieldDefinition fieldDefinition, String[] values) {
        JSONArray array = new JSONArray();
        for (String v : values) {
            Object o;
            if (fieldDefinition.isPrimitiveValueType()) {
                o = convertPrimitiveType(fieldDefinition.getValueType(), v);
            } else {
                o = convertBean(fieldDefinition.getOwner(), fieldDefinition.getValueTypeBean(), v);
            }
            if (o != null) {
                array.add(o);
            }
        }
        return array;
    }

    public JSONArray convertColumnArray(FieldDefinition fieldDefinition, JSONObject rowJson, String value) {
        JSONArray array = rowJson.getJSONArray(fieldDefinition.getName());
        if (array == null) {
            array = new JSONArray();
            rowJson.put(fieldDefinition.getName(), array);
        }

        if (fieldDefinition.isListType()) {
            array.addAll(convertList(fieldDefinition, value));
            return array;
        }

        //set
        JSONArray setArray = convertSet(fieldDefinition, value);
        if (fieldDefinition.getColumnNums().size() == 1) {
            array.addAll(setArray);
            return array;
        }

        String[] values = value.split(fieldDefinition.getEscapedDelimiters().get(0), -1);
        Set<Object> set = new HashSet<>(array);
        Set<String> duplicate = new HashSet<>();
        for (int i = 0; i < setArray.size(); i++) {
            Object o = setArray.get(i);
            if (set.contains(o)) {
                duplicate.add(values[i]);
            } else {
                array.add(o);
            }
        }
        if (!duplicate.isEmpty()) {
            throw new ConvertException(ErrorType.SET_DUPLICATE_VALUE, new ArrayList<>(duplicate));
        }

        return array;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public JSONObject convertColumnMap(FieldDefinition fieldDefinition, JSONObject rowJson, String value) {
        //map类型字段对应1列
        if (fieldDefinition.getColumnNums().size() == 1) {
            return convertMap(fieldDefinition, value);
        }

        //map类型字段对应多列
        JSONObject object = rowJson.getJSONObject(fieldDefinition.getName());
        if (object == null) {
            object = new JSONObject();
            rowJson.put(fieldDefinition.getName(), object);
        }

        if (object.containsKey(null)) {
            //上一次转换的key无效忽略这次的value
            object.remove(null);
            return object;
        }

        Object objectKey = null;

        for (String k : object.keySet()) {
            Object v = object.get(k);
            if (v == null) {
                objectKey = k;
                break;
            }
        }

        if (objectKey == null) {
            try {
                objectKey = convertPrimitiveType(fieldDefinition.getKeyType(), value);
            } catch (Exception ignored) {
            }
            if (objectKey == null) {
                object.put(null, null);//标记接下来的value作废
                throw new ConvertException(ErrorType.MAP_INVALID_KEY, value);
            } else if (object.containsKey(objectKey)) {
                throw new ConvertException(ErrorType.MAP_DUPLICATE_KEY, value);
            }
            object.put(objectKey.toString(), null);
        } else {
            Object objectValue = null;
            try {
                if (fieldDefinition.isPrimitiveValueType()) {
                    objectValue = convertPrimitiveType(fieldDefinition.getValueType(), value);
                } else {
                    objectValue = convertBean(fieldDefinition.getOwner(), fieldDefinition.getValueTypeBean(), value);
                }
            } catch (Exception ignored) {
            }
            if (objectValue == null) {
                //value无效删除对应的key
                object.remove(objectKey);
                throw new ConvertException(ErrorType.MAP_INVALID_VALUE, value);
            }
            object.put(objectKey.toString(), objectValue);
        }

        return object;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private JSONObject convertMap(FieldDefinition fieldDefinition, String fieldValue) {
        JSONObject object = new JSONObject();
        if (StringUtils.isBlank(fieldValue)) {
            return object;
        }

        List<String> delimiters = fieldDefinition.getEscapedDelimiters();
        String[] entries = fieldValue.split(delimiters.get(0), -1);

        for (String entry : entries) {
            String[] pair = entry.split(delimiters.get(1));
            if (pair.length != 2) {
                throw new ConvertException(ErrorType.COMMON);
            }

            Object k = null;
            try {
                k = convertPrimitiveType(fieldDefinition.getKeyType(), pair[0]);
            } catch (Exception ignored) {
            }

            if (k == null) {
                throw new ConvertException(ErrorType.MAP_INVALID_KEY, pair[0]);
            }
            if (object.containsKey(k)) {
                throw new ConvertException(ErrorType.MAP_DUPLICATE_KEY, pair[0]);
            }

            Object v = null;
            try {
                if (fieldDefinition.isPrimitiveValueType()) {
                    v = convertPrimitiveType(fieldDefinition.getValueType(), pair[1]);
                } else {
                    v = convertBean(fieldDefinition.getOwner(), fieldDefinition.getValueTypeBean(), pair[1]);
                }
            } catch (Exception ignored) {
            }

            if (v == null) {
                throw new ConvertException(ErrorType.MAP_INVALID_VALUE, pair[1]);
            }

            object.put(k.toString(), v);
        }

        return object;
    }


    private JSONObject convertBean(ClassDefinition owner, BeanDefinition beanDefinition, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        //有子类，按具体类型转换
        boolean beanHasChild = beanDefinition.hasChildren();
        String[] values = value.split(beanDefinition.getEscapedDelimiter(), -1);
        JSONObject object = new JSONObject();

        if (beanHasChild) {
            String className = ClassDefinition.getLongName(beanDefinition, values[0]);
            if (beanDefinition.getMeAndDescendants().contains(className)) {
                object.put("class", values[0]);
                beanDefinition = parser.getBeanDefinition(owner, className);
            } else {
                throw new ConvertException(ErrorType.TYPE_ERROR, values[0], beanDefinition.getName());
            }
        }

        for (int i = 0; i < beanDefinition.getFields().size(); i++) {
            int valueIndex = i;
            if (beanHasChild) {
                valueIndex++;
            }
            FieldDefinition fieldDefinition = beanDefinition.getFields().get(i);
            Object v = convert(fieldDefinition, values[valueIndex]);
            object.put(fieldDefinition.getName(), v);
        }

        return object;
    }

}
