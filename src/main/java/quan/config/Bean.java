package quan.config;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

import java.util.Objects;

public abstract class Bean {

    private final JSONObject json;

    public Bean(JSONObject json) {
        Objects.requireNonNull(json, "参数[json]不能为空");
        this.json = json;
    }

    public String toJson(JSONWriter.Feature... features) {
        return json.toString(features);
    }

}
