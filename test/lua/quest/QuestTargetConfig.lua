---
---QuestTargetConfig
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

---所有QuestTargetConfig
local configs = {
    { id = 1, name = "任务目标1", noon = "" },
}

---索引:ID
local idConfigs = {}

---加载配置，建立索引
local function loadConfigs()
    for _, config in ipairs(configs) do
        load(idConfigs, config, true, { config.id })
    end
end

loadConfigs()

---QuestTargetConfig
local QuestTargetConfig = {}

---
---获取所有QuestTargetConfig
---@return list<QuestTargetConfig>
function QuestTargetConfig.getAll()
    return configs
end

---
---获取所有QuestTargetConfig
---@return map<id:int,QuestTargetConfig>
function QuestTargetConfig.getIdAll()
     return idConfigs
end

---
---通过索引[id]获取QuestTargetConfig
---@param id int ID
---@return QuestTargetConfig
function QuestTargetConfig.get(id)
    return idConfigs[id]
end

return QuestTargetConfig