local ItemConfig = require("item.ItemConfig")

---
---道具ID
---@author 代码自动生成，请勿手动修改
local ItemIds = {}

---
---道具1
---@return int
function ItemIds.item1()
    return ItemConfig.getByKey("item1").id
end

---
---道具2
---@return int
function ItemIds.item2()
    return ItemConfig.getByKey("item2").id
end

return ItemIds