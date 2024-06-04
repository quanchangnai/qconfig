package quan.config.test;

import org.apache.commons.io.FileUtils;
import quan.config.TableType;
import quan.config.ValidatedException;
import quan.config.test.quest.QuestConfig;
import quan.config.test.item.EquipConfig;
import quan.config.test.item.ItemConfig;
import quan.config.test.item.WeaponConfig;
import quan.config.load.ConfigLoader;
import quan.config.load.DefinitionConfigLoader;
import quan.config.load.JsonConfigLoader;
import quan.config.definition.Language;
import quan.config.definition.parser.CSVDefinitionParser;
import quan.config.definition.parser.ExcelDefinitionParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by quanchangnai on 2019/7/11.
 */
public class ConfigLoaderTest {

    private static String packagePrefix = "quan.config.test";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        int configLoaderType = args.length == 0 ? 1 : Integer.parseInt(args[0]);
        ConfigLoader configLoader = null;

        switch (configLoaderType) {
            case 1:
                configLoader = xmlDefinitionConfigLoader();
                break;
            case 2:
                configLoader = excelDefinitionConfigLoader();
                break;
            case 3:
                configLoader = csvDefinitionConfigLoader();
                break;
            case 4:
                configLoader = jsonConfigLoader();
                break;
        }

        if (configLoader == null) {
            System.err.println("参数错误:" + Arrays.asList(args));
            return;
        }

        configLoader.registerListener(reload -> System.err.println("配置重加载监听器1,reload:" + reload));
        configLoader.registerListener(ItemConfig.class, reload -> System.err.println("配置重加载监听器2,reload:" + reload));

        loadConfig(configLoader);

        writeJson(configLoader);

        reloadAllConfig(configLoader);

        reloadByConfigName(configLoader);

        reloadByTableName(configLoader);

        System.err.println("ConfigTest.main()耗时:" + (System.currentTimeMillis() - startTime) / 1000D + "s");
    }


    private static ConfigLoader xmlDefinitionConfigLoader() {
        TableType tableType = TableType.xlsx;
        String tablePath = "test/config1/xlsx";
        List<String> definitionPaths = Collections.singletonList("test/config1");

        DefinitionConfigLoader configLoader = new DefinitionConfigLoader(tablePath);
        configLoader.useXmlDefinition(definitionPaths, "quan.config.test");
        configLoader.initValidators(packagePrefix);

        configLoader.setTableType(tableType);
        configLoader.setLocale("kr");

        return configLoader;
    }

    private static ConfigLoader excelDefinitionConfigLoader() {
        TableType tableType = TableType.xlsx;
        String tablePath = "test/config2";
        List<String> definitionPaths = Collections.singletonList("test/config2");

        DefinitionConfigLoader configLoader = new DefinitionConfigLoader(tablePath);

        ExcelDefinitionParser definitionParser = new ExcelDefinitionParser(definitionPaths);
        definitionParser.setPackagePrefix(packagePrefix);
        configLoader.setParser(definitionParser);

        configLoader.setTableType(tableType);
        configLoader.setLocale("kr");

        return configLoader;
    }

    private static ConfigLoader csvDefinitionConfigLoader() {
        TableType tableType = TableType.csv;
        String tablePath = "test/config2";
        List<String> definitionPaths = Collections.singletonList("test/config2");

        DefinitionConfigLoader configLoader = new DefinitionConfigLoader(tablePath);

        CSVDefinitionParser definitionParser = new CSVDefinitionParser(definitionPaths);
        definitionParser.setPackagePrefix(packagePrefix);
        configLoader.setParser(definitionParser);

        configLoader.setTableType(tableType);
        configLoader.setLocale("kr");

        return configLoader;
    }

    private static ConfigLoader jsonConfigLoader() {
        String tablePath = "test/json";
        JsonConfigLoader configLoader = new JsonConfigLoader(tablePath);
        configLoader.initValidators(packagePrefix);
        configLoader.setPackagePrefix(packagePrefix);
        return configLoader;
    }

    private static void loadConfig(ConfigLoader configLoader) {
        System.err.println("configLoader.loadConfig()=============");
        long startTime = System.currentTimeMillis();

        try {
            configLoader.loadAll();
        } catch (ValidatedException e) {
            printErrors(e);
        }

        printConfig();

        System.err.println("configLoader.loadConfig()耗时:" + (System.currentTimeMillis() - startTime) / 1000D + "s");
        System.err.println();
    }

    private static void writeJson(ConfigLoader configLoader) {
        if (configLoader.getTableType() == TableType.json || !(configLoader instanceof DefinitionConfigLoader)) {
            return;
        }

        DefinitionConfigLoader configLoader1 = (DefinitionConfigLoader) configLoader;

        System.err.println("writeJson()=============");
        long startTime = System.currentTimeMillis();

        String jsonPath = "test/json";
        File pathFile = new File(jsonPath);

        if (pathFile.exists()) {
            try {
                FileUtils.deleteDirectory(pathFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        configLoader1.writeJson(jsonPath, Language.java);

        System.err.println("writeJson()耗时:" + (System.currentTimeMillis() - startTime) / 1000D + "s");
        System.err.println();
    }

    private static void reloadAllConfig(ConfigLoader configLoader) {
        System.err.println("reloadAllConfig()=============");
        long startTime = System.currentTimeMillis();

        try {
            configLoader.reloadAll();
        } catch (ValidatedException e) {
            printErrors(e);
        }
        printConfig();

        System.err.println("reloadAllConfig()耗时:" + (System.currentTimeMillis() - startTime));
        System.err.println();
    }

    private static void reloadByConfigName(ConfigLoader configLoader) {
        List<String> reloadConfigs = Arrays.asList("item.ItemConfig", "WeaponConfig");
        System.err.println("reloadByConfigName()=============" + reloadConfigs);
        long startTime = System.currentTimeMillis();

        try {
            configLoader.reloadByConfigName(reloadConfigs);
        } catch (ValidatedException e) {
            printErrors(e);
        }
        printConfig();

        System.err.println("reloadByConfigName()耗时:" + (System.currentTimeMillis() - startTime) / 1000D + "s");
        System.err.println();
    }

    private static void reloadByTableName(ConfigLoader configLoader) {
        if (!(configLoader instanceof DefinitionConfigLoader)) {
            return;
        }

        DefinitionConfigLoader configLoader1 = (DefinitionConfigLoader) configLoader;
        List<String> reloadConfigs = Arrays.asList("道具/道具", "装备1");
        System.err.println("reloadByTableName()=============" + reloadConfigs);
        long startTime = System.currentTimeMillis();

        try {
            configLoader1.reloadByTableName(reloadConfigs);
        } catch (ValidatedException e) {
            printErrors(e);
        }
        printConfig();

        System.err.println("reloadByTableName()耗时:" + (System.currentTimeMillis() - startTime) / 1000D + "s");
        System.err.println();
    }

    private static void printErrors(ValidatedException e) {
        System.err.println();
        System.err.println("printErrors start============");

        for (String error : e.getErrors()) {
            System.err.println(error);
        }

        System.err.println("printErrors end============");
        System.err.println();
    }

    private static void printConfig() {
        System.err.println();
        System.err.println("printConfigs start============");

        System.err.println("ItemConfig============");
        for (ItemConfig itemConfig : ItemConfig.getAll()) {
            System.err.println(itemConfig + ",effectiveTime:" + itemConfig.effectiveTime);
        }

        System.err.println("EquipConfig============");
        EquipConfig.self.getAll().forEach(System.err::println);

        System.err.println("WeaponConfig============");
        WeaponConfig.self.getAll().forEach(System.err::println);

        System.err.println("QuestConfig============");
        QuestConfig.getAll().forEach(System.err::println);

        System.err.println("CardConfig============");
        CardConfig.getAll().forEach(System.err::println);

        System.err.println("printConfigs end============");
    }

}
