package quan.config.read;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.parser.CSVDefinitionParser;
import quan.config.definition.parser.DefinitionParser;
import quan.config.definition.parser.TableDefinitionParser;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV配置读取器
 */
public class CSVConfigReader extends ConfigReader {

    {
        tableBodyStartRow = TableDefinitionParser.MIN_TABLE_BODY_START_ROW;
    }

    public CSVConfigReader(File tableFile, ConfigDefinition configDefinition) {
        super(tableFile, configDefinition);
    }

    @Override
    protected void read() {
        if (StringUtils.isBlank(tableEncoding)) {
            DefinitionParser definitionParser = getConfigDefinition().getParser();
            if (definitionParser instanceof CSVDefinitionParser) {
                setTableEncoding(definitionParser.getDefinitionEncoding());
            }
        }

        if (StringUtils.isBlank(tableEncoding)) {
            setTableEncoding("GBK");
        }

        clear();

        List<CSVRecord> records;
        try (CSVParser parser = new CSVParser(new InputStreamReader(Files.newInputStream(getTableFile().toPath()), tableEncoding), CSVFormat.DEFAULT)) {
            records = parser.getRecords();
        } catch (Exception e) {
            logger.error("读取配置[{}]出错", getTableFile(), e);
            return;
        }

        if (records.isEmpty()) {
            return;
        }

        //第一行是表头
        List<String> columnNames = new ArrayList<>();
        for (String columnName : records.get(0)) {
            columnNames.add(columnName.trim());
        }
        validateColumnNames(columnNames);

        //第[bodyRowNum]行起是正文
        if (records.size() < tableBodyStartRow) {
            return;
        }

        for (int r = tableBodyStartRow; r <= records.size(); r++) {
            CSVRecord record = records.get(r - 1);
            JSONObject rowJson = null;

            for (int c = 1; c <= columnNames.size(); c++) {
                String columnValue = record.get(c - 1).trim();
                if (c == 1) {
                    if (columnValue.startsWith("#")) {
                        break;
                    } else {
                        rowJson = new JSONObject(true);
                    }
                }
                addColumnToRow(rowJson, columnNames.get(c - 1), columnValue, r, c);
            }

            if (rowJson != null) {
                jsons.add(rowJson);
            }
        }
    }

}
