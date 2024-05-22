
--package.path = package.path .. ";../"

local ItemIds = require("item.ItemIds")
local ItemType = require("item.ItemType")
local ItemConfig = require("item.ItemConfig")
local QuestConfig = require("quest.QuestConfig")

function testConfig()
    print("testConfig=======================")
    print()

    print("ItemIds=====")
    print("ItemIds.item1=" .. ItemIds.item1())
    print()

    print("ItemType=====")
    print("ItemType.type1=" .. ItemType.type1)
    print()

    print("ItemConfig=====")
    for i, v in ipairs(ItemConfig.getAll()) do
        print(v.id, v.name)
    end
    print()

    print("QuestConfig=====")
    print("QuestConfig.getByComposite3(111,222,333)", QuestConfig.getByComposite3("111", 222, 333).name)
end

testConfig()

