package quan.config.read;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * JSON配置读取器
 */
@SuppressWarnings({"unchecked"})
public class JsonConfigReader extends ConfigReader {

    public JsonConfigReader(File jsonFile, String configFullName) {
        super(jsonFile, null);
        initPrototype(configFullName);
    }

    @Override
    protected void read() {
        try (FileInputStream inputStream = new FileInputStream(getTableFile())) {
            byte[] availableBytes = new byte[inputStream.available()];
            Validate.isTrue(inputStream.read(availableBytes) > 0);
            List<JSONObject> jsons = (List<JSONObject>) JSON.parse(availableBytes);
            this.jsons.addAll(jsons);
        } catch (Exception e) {
            logger.error("读取配置[{}]出错", getTableFile().getName(), e);
        }
    }
}
