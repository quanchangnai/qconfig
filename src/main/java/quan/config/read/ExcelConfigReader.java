package quan.config.read;

import com.alibaba.fastjson2.JSONObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.parser.TableDefinitionParser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel配置读取器
 */
public class ExcelConfigReader extends ConfigReader {

    private static final DataFormatter formatter = new DataFormatter();

    {
        tableBodyStartRow = TableDefinitionParser.MIN_TABLE_BODY_START_ROW;
    }

    public ExcelConfigReader(File tableFile, ConfigDefinition configDefinition) {
        super(tableFile, configDefinition);
    }

    @Override
    protected void read() {
        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(getTableFile().toPath()))) {
            //只读取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            //总行数
            int totalTowNum = sheet.getLastRowNum() + 1;
            if (totalTowNum < 1) {
                return;
            }

            //第一行是表头
            List<String> columnNames = new ArrayList<>();

            for (Cell cell : sheet.getRow(0)) {
                columnNames.add(formatter.formatCellValue(cell).trim());
            }

            validateColumnNames(columnNames);

            //第[tableBodyStartRow]行起是正文
            if (totalTowNum < tableBodyStartRow) {
                return;
            }

            for (int r = tableBodyStartRow; r <= totalTowNum; r++) {
                Row row = sheet.getRow(r - 1);
                JSONObject rowJson = null;

                for (int c = 1; c <= columnNames.size(); c++) {
                    String columnValue = formatter.formatCellValue(row.getCell(c - 1)).trim();
                    if (c == 1) {
                        if (columnValue.startsWith("#")) {
                            break;
                        } else {
                            rowJson = new JSONObject();
                        }
                    }
                    addColumnToRow(rowJson, columnNames.get(c - 1), columnValue, r, c);
                }

                if (rowJson != null) {
                    jsons.add(rowJson);
                }
            }
        } catch (Exception e) {
            logger.error("读取配置[{}]出错", getTableFile(), e);
        }

    }

}
