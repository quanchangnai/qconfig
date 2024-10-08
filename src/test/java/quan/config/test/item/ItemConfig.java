package quan.config.test.item;

import com.alibaba.fastjson2.*;
import java.time.*;
import java.util.*;
import quan.config.*;
import quan.config.load.ConfigLoader;
import quan.config.read.ConfigConverter;

/**
 * 道具/道具<br/>
 * 代码自动生成，请勿手动修改
 */
public class ItemConfig extends Config {

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
    public final ItemType type;

    /**
     * 使用效果
     */
    public final UseEffect useEffect;

    /**
     * 奖励
     */
    public final Reward reward;

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
    public final LocalDateTime effectiveTime;

    /**
     * 生效时间
     */
    public final String effectiveTime$Str;


    public ItemConfig(JSONObject json) {
        super(json);

        this.id = json.getIntValue(Field.ID);
        this.key = json.getOrDefault(Field.KEY, "").toString();
        this.name = json.getOrDefault(Field.NAME, "").toString();
        this.type = ItemType.valueOf(json.getIntValue(Field.TYPE));

        JSONObject useEffect = json.getJSONObject(Field.USE_EFFECT);
        if (useEffect != null) {
            this.useEffect = UseEffect.create(useEffect);
        } else {
            this.useEffect = null;
        }

        JSONObject reward = json.getJSONObject(Field.REWARD);
        if (reward != null) {
            this.reward = Reward.create(reward);
        } else {
            this.reward = null;
        }

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

        this.effectiveTime$Str = json.getOrDefault(Field.EFFECTIVE_TIME, "").toString();
        this.effectiveTime = ConfigConverter.parseDateTime(this.effectiveTime$Str);
    }

    @Override
    public ItemConfig create(JSONObject json) {
        return new ItemConfig(json);
    }

    @Override
    public String toString() {
        return "ItemConfig{" +
                "id=" + id +
                ",key='" + key + '\'' +
                ",name='" + name + '\'' +
                ",type=" + type +
                ",useEffect=" + useEffect +
                ",reward=" + reward +
                ",list=" + list +
                ",set=" + set +
                ",map=" + map +
                ",effectiveTime='" + effectiveTime$Str + '\'' +
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
         * 使用效果
         */
        public static final String USE_EFFECT = "useEffect";

        /**
         * 奖励
         */
        public static final String REWARD = "reward";

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


    //所有ItemConfig
    private static volatile List<ItemConfig> _configs = Collections.emptyList();

    //索引:ID
    private static volatile Map<Integer, ItemConfig> _idConfigs = Collections.emptyMap();

    //索引:常量Key
    private static volatile Map<String, ItemConfig> _keyConfigs = Collections.emptyMap();

    //索引:类型
    private static volatile Map<ItemType, List<ItemConfig>> _typeConfigs = Collections.emptyMap();

    public static List<ItemConfig> getAll() {
        return _configs;
    }

    public static Map<Integer, ItemConfig> getIdAll() {
        return _idConfigs;
    }

    public static ItemConfig get(int id) {
        return _idConfigs.get(id);
    }

    public static Map<String, ItemConfig> getKeyAll() {
        return _keyConfigs;
    }

    public static ItemConfig getByKey(String key) {
        return _keyConfigs.get(key);
    }

    public static Map<ItemType, List<ItemConfig>> getTypeAll() {
        return _typeConfigs;
    }

    public static List<ItemConfig> getByType(ItemType type) {
        return _typeConfigs.getOrDefault(type, Collections.emptyList());
    }


    @SuppressWarnings({"unchecked"})
    private static List<String> load(List<ItemConfig> configs) {
        Map<Integer, ItemConfig> idConfigs = new HashMap<>();
        Map<String, ItemConfig> keyConfigs = new HashMap<>();
        Map<ItemType, List<ItemConfig>> typeConfigs = new HashMap<>();

        List<String> errors = new ArrayList<>();

        for (ItemConfig config : configs) {
            load(idConfigs, errors, config, true, "id", config.id);
            if (!config.key.isEmpty()) {
                load(keyConfigs, errors, config, true, "key", config.key);
            }
            load(typeConfigs, errors, config, false, "type", config.type);
        }

        configs = Collections.unmodifiableList(configs);
        idConfigs = unmodifiableMap(idConfigs);
        keyConfigs = unmodifiableMap(keyConfigs);
        typeConfigs = unmodifiableMap(typeConfigs);

        ItemConfig._configs = configs;
        ItemConfig._idConfigs = idConfigs;
        ItemConfig._keyConfigs = keyConfigs;
        ItemConfig._typeConfigs = typeConfigs;

        return errors;
    }

    static {
        ConfigLoader.registerLoadFunction(ItemConfig.class, ItemConfig::load);
    }

}
