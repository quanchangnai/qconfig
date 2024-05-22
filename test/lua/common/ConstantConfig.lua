---
---常量
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

---所有ConstantConfig
local configs = {
    { key = "constant1", itemId = 1, reward = nil, rewardList = {  }, comment = "常量1" },
    { key = "constant2", itemId = 2, reward = nil, rewardList = {  }, comment = "常量2" },
    { key = "constant2", itemId = 3, reward = nil, rewardList = {  }, comment = "常量3" },
    { key = "", itemId = 4, reward = nil, rewardList = {  }, comment = "常量4" },
}

---索引:常量Key
local keyConfigs = {}

---加载配置，建立索引
local function loadConfigs()
    for _, config in ipairs(configs) do
        load(keyConfigs, config, true, { config.key })
    end
end

loadConfigs()

---常量
local ConstantConfig = {}

---
---获取所有ConstantConfig
---@return list<ConstantConfig>
function ConstantConfig.getAll()
    return configs
end

---
---获取所有ConstantConfig
---@return map<key:string,ConstantConfig>
function ConstantConfig.getKeyAll()
     return keyConfigs
end

---
---通过索引[key]获取ConstantConfig
---@param key string 常量Key
---@return ConstantConfig
function ConstantConfig.getByKey(key)
    return keyConfigs[key]
end

return ConstantConfig