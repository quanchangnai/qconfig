package quan.config.test;

import com.alibaba.fastjson2.*;
import java.util.*;

import quan.config.Config;
import quan.config.load.ConfigLoader;

/**
 * CardConfig<br/>
 * 代码自动生成，请勿手动修改
 */
public class CardConfig extends Config {

    /**
     * ID
     */
    public final int id;

    /**
     * 常量Key
     */
    public final String key;

    /**
     * 名字
     */
    public final String name;

    /**
     * 类型
     */
    public final CardType type;

    /**
     * 价格
     */
    public final int price;

    /**
     * List
     */
    public final List<Integer> list;

    /**
     * Set
     */
    public final Set<Integer> set;

    /**
     * Map
     */
    public final Map<Integer, Integer> map;

    /**
     * 生效时间
     */
    public final Date effectiveTime;

    /**
     * 生效时间
     */
    public final String effectiveTime_;


    public CardConfig(JSONObject json) {
        super(json);

        this.id = json.getIntValue(Field.ID);
        this.key = json.getOrDefault(Field.KEY, "").toString();
        this.name = json.getOrDefault(Field.NAME, "").toString();
        this.type = CardType.valueOf(json.getIntValue(Field.TYPE));
        this.price = json.getIntValue(Field.PRICE);

        JSONArray list$1 = json.getJSONArray(Field.LIST);
        List<Integer> list$2 = new ArrayList<>();
        if (list$1 != null) {
            for (int i = 0; i < list$1.size(); i++) {
                list$2.add(list$1.getInteger(i));
            }
        }
        this.list = Collections.unmodifiableList(list$2);

        JSONArray set$1 = json.getJSONArray(Field.SET);
        Set<Integer> set$2 = new HashSet<>();
        if (set$1 != null) {
            for (int i = 0; i < set$1.size(); i++) {
                set$2.add(set$1.getInteger(i));
            }
        }
        this.set = Collections.unmodifiableSet(set$2);

        JSONObject map$1 = json.getJSONObject(Field.MAP);
        Map<Integer, Integer> map$2 = new HashMap<>();
        if (map$1 != null) {
            for (String map$Key : map$1.keySet()) {
                map$2.put(Integer.valueOf(map$Key), map$1.getInteger(map$Key));
            }
        }
        this.map = Collections.unmodifiableMap(map$2);

        this.effectiveTime = json.getDate(Field.EFFECTIVE_TIME);
        this.effectiveTime_ = json.getOrDefault(Field.EFFECTIVE_TIME + "_", "").toString();
    }

    @Override
    public CardConfig create(JSONObject json) {
        return new CardConfig(json);
    }

    @Override
    public String toString() {
        return "CardConfig{" +
                "id=" + id +
                ",key='" + key + '\'' +
                ",name='" + name + '\'' +
                ",type=" + type +
                ",price=" + price +
                ",list=" + list +
                ",set=" + set +
                ",map=" + map +
                ",effectiveTime='" + effectiveTime_ + '\'' +
                '}';

    }

    public static class Field {

        /**
         * ID
         */
        public static final String ID = "id";

        /**
         * 常量Key
         */
        public static final String KEY = "key";

        /**
         * 名字
         */
        public static final String NAME = "name";

        /**
         * 类型
         */
        public static final String TYPE = "type";

        /**
         * 价格
         */
        public static final String PRICE = "price";

        /**
         * List
         */
        public static final String LIST = "list";

        /**
         * Set
         */
        public static final String SET = "set";

        /**
         * Map
         */
        public static final String MAP = "map";

        /**
         * 生效时间
         */
        public static final String EFFECTIVE_TIME = "effectiveTime";

    }


    //所有CardConfig
    private static volatile List<CardConfig> _configs = Collections.emptyList();

    //索引:ID
    private static volatile Map<Integer, CardConfig> _idConfigs = Collections.emptyMap();

    //索引:类型
    private static volatile Map<CardType, List<CardConfig>> _typeConfigs = Collections.emptyMap();

    private static volatile Map<String, CardConfig> _keyConfigs = Collections.emptyMap();

    public static List<CardConfig> getAll() {
        return _configs;
    }

    public static Map<Integer, CardConfig> getIdAll() {
        return _idConfigs;
    }

    public static CardConfig get(int id) {
        return _idConfigs.get(id);
    }

    public static Map<CardType, List<CardConfig>> getTypeAll() {
        return _typeConfigs;
    }

    public static List<CardConfig> getByType(CardType type) {
        return _typeConfigs.getOrDefault(type, Collections.emptyList());
    }

    public static Map<String, CardConfig> getKeyAll() {
        return _keyConfigs;
    }

    public static CardConfig getByKey(String key) {
        return _keyConfigs.get(key);
    }


    @SuppressWarnings({"unchecked"})
    private static List<String> load(List<CardConfig> configs) {
        Map<Integer, CardConfig> idConfigs = new HashMap<>();
        Map<CardType, List<CardConfig>> typeConfigs = new HashMap<>();
        Map<String, CardConfig> keyConfigs = new HashMap<>();

        List<String> errors = new ArrayList<>();

        for (CardConfig config : configs) {
            load(idConfigs, errors, config, true, "id", config.id);
            load(typeConfigs, errors, config, false, "type", config.type);
            if (!config.key.isEmpty()) {
                load(keyConfigs, errors, config, true, "key", config.key);
            }
        }

        configs = Collections.unmodifiableList(configs);
        idConfigs = unmodifiableMap(idConfigs);
        typeConfigs = unmodifiableMap(typeConfigs);
        keyConfigs = unmodifiableMap(keyConfigs);

        CardConfig._configs = configs;
        CardConfig._idConfigs = idConfigs;
        CardConfig._typeConfigs = typeConfigs;
        CardConfig._keyConfigs = keyConfigs;

        return errors;
    }

    static {
        ConfigLoader.registerLoadFunction(CardConfig.class, CardConfig::load);
    }

}
