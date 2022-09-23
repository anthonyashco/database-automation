package dev.anthonyashco.textutilities;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XlsxUtility {
    public static Map<String, Map<String, String>> getFieldMapping(Workbook wb, String sheetName) {
        Sheet sheet = wb.getSheet(sheetName);
        Row row = sheet.getRow(0);
        List<String> headings = new ArrayList<>();
        Map<String, Map<String, String>> fieldMapping = new HashMap<>();

        int i = 0;
        Cell cell;
        String contents;
        while ((cell = row.getCell(i)) != null && !(contents = cell.toString().trim()).isEmpty()) {
            headings.add(contents);
            i++;
        }

        int rowIndex = 1;
        row = sheet.getRow(rowIndex);
        String rowName;
        while (row != null && !(rowName = row.getCell(0).toString().trim()).isEmpty()) {
            Map<String, String> rowMap = new HashMap<>();
            for (int j = 1; j < headings.size(); j++) {
                cell = row.getCell(j);
                String value = (cell != null) ? cell.toString() : "";
                rowMap.put(headings.get(j), value);
            }
            fieldMapping.put(rowName, rowMap);
            rowIndex++;
            row = sheet.getRow(rowIndex);
        }

        return fieldMapping;
    }

    public static List<String> listRowUntilEmpty(Sheet sheet, int rowIndex) {
        List<String> items = new ArrayList<>();
        Row row = sheet.getRow(rowIndex);
        int cellIndex = 0;
        Cell cell;
        String contents;
        while ((cell = row.getCell(cellIndex++)) != null && !(contents = cell.toString()).isEmpty()) {
            items.add(contents);
        }
        return items;
    }

    public static List<String> listColumnUntilEmpty(Sheet sheet, int columnIndex, int startingRow) {
        List<String> items = new ArrayList<>();
        int i = startingRow;
        Row row;
        Cell cell;
        String contents;
        while ((row = sheet.getRow(i++)) != null && (cell = row.getCell(columnIndex)) != null && !(contents = cell.toString()).isEmpty()) {
            items.add(contents);
        }
        return items;
    }
}
