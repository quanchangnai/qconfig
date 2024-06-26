---
---任务
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

---所有QuestConfig
local configs = {
    { id = 1, name = "任务1", type = 1, target = 1, reward = nil, a1 = 1, a2 = 2, b1 = 11, b2 = false, c1 = "111", c2 = 222, c3 = 333, d1 = "1111", d2 = 2222, d3 = 333, s1 = {  }, l1 = {  }, m1 = {  } },
}

---索引:ID
local idConfigs = {}

---索引:类型
local typeConfigs = {}

---索引:两字段唯一索引
local composite1Configs = {}

---索引:两字段普通索引
local composite2Configs = {}

---索引:三字段唯一索引
local composite3Configs = {}

---索引:三字段普通索引
local composite4Configs = {}

---加载配置，建立索引
local function loadConfigs()
    for _, config in ipairs(configs) do
        load(idConfigs, config, true, { config.id })
        load(typeConfigs, config, false, { config.type })
        load(composite1Configs, config, true, { config.a1, config.a2 })
        load(composite2Configs, config, false, { config.b1, config.b2 })
        load(composite3Configs, config, true, { config.c1, config.c2, config.c3 })
        load(composite4Configs, config, false, { config.d1, config.d2, config.d3 })
    end
end

loadConfigs()

---任务
local QuestConfig = {}

---
---获取所有QuestConfig
---@return list<QuestConfig>
function QuestConfig.getAll()
    return configs
end

---
---获取所有QuestConfig
---@return map<id:int,QuestConfig>
function QuestConfig.getIdAll()
     return idConfigs
end

---
---通过索引[id]获取QuestConfig
---@param id int ID
---@return QuestConfig
function QuestConfig.get(id)
    return idConfigs[id]
end

---
---获取所有QuestConfig
---@return map<type:QuestType,list<QuestConfig>> 
function QuestConfig.getTypeAll()
     return typeConfigs
end

---
---通过索引[type]获取QuestConfig
---@param type QuestType 类型
---@return list<QuestConfig>
function QuestConfig.getByType(type)
    return typeConfigs[type] or {}
end

---
---获取所有QuestConfig
---@return map<a1:int,map<a2:int,QuestConfig>>
function QuestConfig.getComposite1All()
     return composite1Configs
end

---
---通过索引[composite1]获取QuestConfig
---@overload fun(a1:int):map<a2:int,QuestConfig>
---@param a1 int A1
---@param a2 int A2
---@return QuestConfig
function QuestConfig.getByComposite1(a1, a2)
    local map = composite1Configs[a1] or {}
    if (not a2) then
        return map
    else
        return map[a2]
    end
end

---
---获取所有QuestConfig
---@return map<b1:int,map<b2:bool,list<QuestConfig>>>
function QuestConfig.getComposite2All()
     return composite2Configs
end

---
---通过索引[composite2]获取QuestConfig
---@overload fun(b1:int):map<b2:bool,list<QuestConfig>>
---@param b1 int B1
---@param b2 bool B2
---@return list<QuestConfig>
function QuestConfig.getByComposite2(b1, b2)
    local map = composite2Configs[b1] or {}
    if (not b2) then
        return map
    else
        return map[b2]
    end
end

---
---获取所有QuestConfig
---@return map<c1:string,map<c2:int,map<c3:int,QuestConfig>>>
function QuestConfig.getComposite3All()
     return composite3Configs
end

---
---通过索引[composite3]获取QuestConfig
---@overload fun(c1:string):map<c2:int,map<c3:int,QuestConfig>>
---@overload fun(c1:string, c2:int):map<c3:int,QuestConfig>
---@param c1 string C1
---@param c2 int C2
---@param c3 int C3
---@return QuestConfig
function QuestConfig.getByComposite3(c1, c2, c3)
    local map1 = composite3Configs[c1] or {}
    if (not c2) then
        return map1
    end

    local map2 = map1[c2] or {}
    if (not c3) then
        return map2
    end

    return map2[c3]
end

---
---获取所有QuestConfig
---@return map<d1:string,map<d2:int,map<d3:int,list<QuestConfig>>>>
function QuestConfig.getComposite4All()
     return composite4Configs
end

---
---通过索引[composite4]获取QuestConfig
---@overload fun(d1:string):map<d2:int,map<d3:int,list<QuestConfig>>>
---@overload fun(d1:string, d2:int):map<d3:int,list<QuestConfig>>
---@param d1 string D1
---@param d2 int D2
---@param d3 int D3
---@return list<QuestConfig>
function QuestConfig.getByComposite4(d1, d2, d3)
    local map1 = composite4Configs[d1] or {}
    if (not d2) then
        return map1
    end

    local map2 = map1[d2] or {}
    if (not d3) then
        return map2
    end

    return map2[d3]
end

return QuestConfig