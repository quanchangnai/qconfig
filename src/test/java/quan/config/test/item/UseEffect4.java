package quan.config.test.item;

import com.alibaba.fastjson2.*;
import java.util.*;

/**
 * 使用效果4<br/>
 * 代码自动生成，请勿手动修改
 */
public class UseEffect4 extends UseEffect {

    public final ItemType itemType;

    public final List<ItemConfig> itemTypeRef() {
        return ItemConfig.getByType(itemType);
    }


    public UseEffect4(JSONObject json) {
        super(json);

        this.itemType = ItemType.valueOf(json.getIntValue("itemType"));
    }

    public static UseEffect4 create(JSONObject json) {
        return new UseEffect4(json);
    }

    @Override
    public String toString() {
        return "UseEffect4{" +
                "aaa=" + aaa +
                ",itemType=" + itemType +
                '}';

    }

}
