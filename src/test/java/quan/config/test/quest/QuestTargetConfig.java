package quan.config.test.quest;

import com.alibaba.fastjson2.*;
import java.time.*;
import java.util.*;
import quan.config.*;
import quan.config.load.ConfigLoader;
import quan.config.read.ConfigConverter;

/**
 * QuestTargetConfig<br/>
 * 代码自动生成，请勿手动修改
 */
public class QuestTargetConfig extends Config {

    /**
     * ID
     */
    public final int id;

    /**
     * 中午
     */
    public final LocalTime noon;

    /**
     * 中午
     */
    public final String noon$Str;


    public QuestTargetConfig(JSONObject json) {
        super(json);

        this.id = json.getIntValue(Field.ID);
        this.noon$Str = json.getOrDefault(Field.NOON, "").toString();
        this.noon = ConfigConverter.parseTime(this.noon$Str);
    }

    @Override
    public QuestTargetConfig create(JSONObject json) {
        return new QuestTargetConfig(json);
    }

    @Override
    public String toString() {
        return "QuestTargetConfig{" +
                "id=" + id +
                ",noon='" + noon$Str + '\'' +
                '}';

    }

    public static class Field {

        /**
         * ID
         */
        public static final String ID = "id";

        /**
         * 名字
         */
        public static final String NAME = "name";

        /**
         * 中午
         */
        public static final String NOON = "noon";

    }


    //所有QuestTargetConfig
    private static volatile List<QuestTargetConfig> _configs = Collections.emptyList();

    //索引:ID
    private static volatile Map<Integer, QuestTargetConfig> _idConfigs = Collections.emptyMap();

    public static List<QuestTargetConfig> getAll() {
        return _configs;
    }

    public static Map<Integer, QuestTargetConfig> getIdAll() {
        return _idConfigs;
    }

    public static QuestTargetConfig get(int id) {
        return _idConfigs.get(id);
    }


    @SuppressWarnings({"unchecked"})
    private static List<String> load(List<QuestTargetConfig> configs) {
        Map<Integer, QuestTargetConfig> idConfigs = new HashMap<>();

        List<String> errors = new ArrayList<>();

        for (QuestTargetConfig config : configs) {
            load(idConfigs, errors, config, true, "id", config.id);
        }

        configs = Collections.unmodifiableList(configs);
        idConfigs = unmodifiableMap(idConfigs);

        QuestTargetConfig._configs = configs;
        QuestTargetConfig._idConfigs = idConfigs;

        return errors;
    }

    static {
        ConfigLoader.registerLoadFunction(QuestTargetConfig.class, QuestTargetConfig::load);
    }

}
