package quan.config.test.item;

import com.alibaba.fastjson2.*;
import java.time.*;
import java.util.*;
import quan.config.*;
import quan.config.load.ConfigLoader;
import quan.config.read.ConfigConverter;

/**
 * 道具/装备1,道具/装备2<br/>
 * 代码自动生成，请勿手动修改
 */
public class EquipConfig extends ItemConfig {

    /**
     * 部位
     */
    public final int position;

    /**
     * 颜色
     */
    public final int color;


    public EquipConfig(JSONObject json) {
        super(json);

        this.position = json.getIntValue(Field.POSITION);
        this.color = json.getIntValue(Field.COLOR);
    }

    @Override
    public EquipConfig create(JSONObject json) {
        return new EquipConfig(json);
    }

    @Override
    public String toString() {
        return "EquipConfig{" +
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
                ",position=" + position +
                ",color=" + color +
                '}';

    }

    public static class Field extends ItemConfig.Field{

        /**
         * 部位
         */
        public static final String POSITION = "position";

        /**
         * 颜色
         */
        public static final String COLOR = "color";

    }


    public final static class self {

        private self() {
        }

        //所有EquipConfig
        private static volatile List<EquipConfig> _configs = Collections.emptyList();

        //索引:部位
        private static volatile Map<Integer, List<EquipConfig>> _positionConfigs = Collections.emptyMap();

        //索引:ID
        private static volatile Map<Integer, EquipConfig> _idConfigs = Collections.emptyMap();

        //索引:常量Key
        private static volatile Map<String, EquipConfig> _keyConfigs = Collections.emptyMap();

        //索引:类型
        private static volatile Map<ItemType, List<EquipConfig>> _typeConfigs = Collections.emptyMap();

        public static List<EquipConfig> getAll() {
            return _configs;
        }

        public static Map<Integer, List<EquipConfig>> getPositionAll() {
            return _positionConfigs;
        }

        public static List<EquipConfig> getByPosition(int position) {
            return _positionConfigs.getOrDefault(position, Collections.emptyList());
        }

        public static Map<Integer, EquipConfig> getIdAll() {
            return _idConfigs;
        }

        public static EquipConfig get(int id) {
            return _idConfigs.get(id);
        }

        public static Map<String, EquipConfig> getKeyAll() {
            return _keyConfigs;
        }

        public static EquipConfig getByKey(String key) {
            return _keyConfigs.get(key);
        }

        public static Map<ItemType, List<EquipConfig>> getTypeAll() {
            return _typeConfigs;
        }

        public static List<EquipConfig> getByType(ItemType type) {
            return _typeConfigs.getOrDefault(type, Collections.emptyList());
        }


        @SuppressWarnings({"unchecked"})
        private static List<String> load(List<EquipConfig> configs) {
            Map<Integer, List<EquipConfig>> positionConfigs = new HashMap<>();
            Map<Integer, EquipConfig> idConfigs = new HashMap<>();
            Map<String, EquipConfig> keyConfigs = new HashMap<>();
            Map<ItemType, List<EquipConfig>> typeConfigs = new HashMap<>();

            List<String> errors = new ArrayList<>();

            for (EquipConfig config : configs) {
                Config.load(positionConfigs, errors, config, false, "position", config.position);
                Config.load(idConfigs, errors, config, true, "id", config.id);
                if (!config.key.isEmpty()) {
                    Config.load(keyConfigs, errors, config, true, "key", config.key);
                }
                Config.load(typeConfigs, errors, config, false, "type", config.type);
            }

            configs = Collections.unmodifiableList(configs);
            positionConfigs = unmodifiableMap(positionConfigs);
            idConfigs = unmodifiableMap(idConfigs);
            keyConfigs = unmodifiableMap(keyConfigs);
            typeConfigs = unmodifiableMap(typeConfigs);

            EquipConfig.self._configs = configs;
            EquipConfig.self._positionConfigs = positionConfigs;
            EquipConfig.self._idConfigs = idConfigs;
            EquipConfig.self._keyConfigs = keyConfigs;
            EquipConfig.self._typeConfigs = typeConfigs;

            return errors;
        }

    }

    static {
        ConfigLoader.registerLoadFunction(EquipConfig.class, EquipConfig.self::load);
    }

}
