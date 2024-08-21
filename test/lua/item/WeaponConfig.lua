---
---道具/武器
---代码自动生成，请勿手动修改
---

local function load(configs, config, unique, keys)
    local map = configs
    
    for i = 1, #keys - 1 do
        local key = keys[i]
        if not map[key] then
            map[key] = {}
        end
        map = map[key]
    end

    if not unique then
        local list = map[keys[#keys]]
        if not list then
            list = {}
            map[keys[#keys]] = list
        end
        table.insert(list, config)
        return
    end

    map[keys[#keys]] = config
end

---所有WeaponConfig
local configs = {
    { id = 6, key = "", name = "武器1", type = 1, useEffect = { class = "UseEffect", aaa = 0 }, reward = { itemId = 5, itemNum = 0 }, list = { 1, 2, 342, 45 }, set = {  }, map = { [54] = 34 }, effectiveTime = "", position = 2, color = 4, w1 = 11, w2 = 43, rewardList = { { itemId = 12, itemNum = 22 }, { itemId = 32, itemNum = 56 }, { itemId = 23, itemNum = 56 } }, rewardSet = { { itemId = 433, itemNum = 51 }, { itemId = 433, itemNum = 52 } }, rewardMap = { [2] = { itemId = 12, itemNum = 22 }, [21] = { itemId = 32, itemNum = 56 }, [54] = { itemId = 23, itemNum = 56 } }, list2 = { 21, 32 } },
    { id = 7, key = "", name = "武器3", type = 1, useEffect = { class = "UseEffect2", aaa = 522, bbb = 3324 }, reward = { itemId = 6, itemNum = 12 }, list = { 44, 25, 342, 45 }, set = {  }, map = { [88] = 33 }, effectiveTime = "", position = 0, color = 4, w1 = 11, w2 = 43, rewardList = { { itemId = 12, itemNum = 22 }, { itemId = 32, itemNum = 56 }, { itemId = 23, itemNum = 56 } }, rewardSet = { { itemId = 23, itemNum = 56 }, { itemId = 23, itemNum = 55 } }, rewardMap = { [22] = { itemId = 12, itemNum = 22 }, [21] = { itemId = 32, itemNum = 56 }, [65] = { itemId = 23, itemNum = 56 } }, list2 = { 41, 22 } },
    { id = 8, key = "", name = "武器4", type = 1, useEffect = nil, reward = { itemId = 7, itemNum = 13 }, list = { 44, 44, 0, 342, 45 }, set = {  }, map = { [22] = 32 }, effectiveTime = "", position = 2, color = 4, w1 = 11, w2 = 43, rewardList = { { itemId = 42, itemNum = 25 }, { itemId = 32, itemNum = 54 }, { itemId = 63, itemNum = 56 } }, rewardSet = { { itemId = 23, itemNum = 56 }, { itemId = 23, itemNum = 53 } }, rewardMap = { [4] = { itemId = 12, itemNum = 22 }, [6] = { itemId = 32, itemNum = 56 }, [3] = { itemId = 23, itemNum = 56 } }, list2 = { 2, 324 } },
}

---索引:ID
local idConfigs = {}

---索引:常量Key
local keyConfigs = {}

---索引:类型
local typeConfigs = {}

---索引:部位
local positionConfigs = {}

local composite1Configs = {}

local composite2Configs = {}

---加载配置，建立索引
local function loadConfigs()
    for _, config in ipairs(configs) do
        load(idConfigs, config, true, { config.id })
        load(keyConfigs, config, true, { config.key })
        load(typeConfigs, config, false, { config.type })
        load(positionConfigs, config, false, { config.position })
        load(composite1Configs, config, false, { config.color, config.w1 })
        load(composite2Configs, config, true, { config.w1, config.w2 })
    end
end

loadConfigs()

---道具/武器
local WeaponConfig = {}

---
---获取所有WeaponConfig
---@return list<WeaponConfig>
function WeaponConfig.getAll()
    return configs
end

---
---获取所有WeaponConfig
---@return map<id:int,WeaponConfig>
function WeaponConfig.getIdAll()
     return idConfigs
end

---
---通过索引[id]获取WeaponConfig
---@param id int ID
---@return WeaponConfig
function WeaponConfig.get(id)
    return idConfigs[id]
end

---
---获取所有WeaponConfig
---@return map<key:string,WeaponConfig>
function WeaponConfig.getKeyAll()
     return keyConfigs
end

---
---通过索引[key]获取WeaponConfig
---@param key string 常量Key
---@return WeaponConfig
function WeaponConfig.getByKey(key)
    return keyConfigs[key]
end

---
---获取所有WeaponConfig
---@return map<type:ItemType,list<WeaponConfig>> 
function WeaponConfig.getTypeAll()
     return typeConfigs
end

---
---通过索引[type]获取WeaponConfig
---@param type ItemType 类型
---@return list<WeaponConfig>
function WeaponConfig.getByType(type)
    return typeConfigs[type] or {}
end

---
---获取所有WeaponConfig
---@return map<position:int,list<WeaponConfig>> 
function WeaponConfig.getPositionAll()
     return positionConfigs
end

---
---通过索引[position]获取WeaponConfig
---@param position int 部位
---@return list<WeaponConfig>
function WeaponConfig.getByPosition(position)
    return positionConfigs[position] or {}
end

---
---获取所有WeaponConfig
---@return map<color:int,map<w1:int,list<WeaponConfig>>>
function WeaponConfig.getComposite1All()
     return composite1Configs
end

---
---通过索引[composite1]获取WeaponConfig
---@overload fun(color:int):map<w1:int,list<WeaponConfig>>
---@param color int 颜色
---@param w1 int 字段1
---@return list<WeaponConfig>
function WeaponConfig.getByComposite1(color, w1)
    local map = composite1Configs[color] or {}
    if (not w1) then
        return map
    else
        return map[w1]
    end
end

---
---获取所有WeaponConfig
---@return map<w1:int,map<w2:int,WeaponConfig>>
function WeaponConfig.getComposite2All()
     return composite2Configs
end

---
---通过索引[composite2]获取WeaponConfig
---@overload fun(w1:int):map<w2:int,WeaponConfig>
---@param w1 int 字段1
---@param w2 int 字段2
---@return WeaponConfig
function WeaponConfig.getByComposite2(w1, w2)
    local map = composite2Configs[w1] or {}
    if (not w2) then
        return map
    else
        return map[w2]
    end
end

return WeaponConfig