package dev.anthonyashco.textutilities;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

/**
 * A utility class for converting csv output files to other formats for presentation.
 */
public class CsvConverter {
    /**
     * Transcribes a csv input to a new page in an Excel workbook.
     *
     * @param workbook The destination Workbook from Apache POI.
     * @param inputFile The source csv file.
     * @param sheetName The name of the new sheet to create.
     * @param invertAxis Whether to invert the x and y axes when transcribing.
     */
    public static void toXlsx(Workbook workbook, Path inputFile, String sheetName, boolean invertAxis) {
        try (BufferedReader bfr = new BufferedReader(new FileReader(inputFile.toFile()))) {
            CSVParser csv = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).build().parse(bfr);

            if (invertAxis) {
                int columnNumber = 0;

                List<String> headers = csv.getHeaderNames();
                Sheet sheet = workbook.createSheet(sheetName);
                for (int i = 0; i < headers.size(); i++) {
                    sheet.createRow(i).createCell(columnNumber).setCellValue(headers.get(i));
                }
                columnNumber++;

                for (CSVRecord record : csv) {
                    List<String> entries = record.toList();
                    for (int j = 0; j < entries.size(); j++) {
                        sheet.getRow(j).createCell(columnNumber).setCellValue(entries.get(j));
                    }
                    columnNumber++;
                }
            } else {
                int rowNumber = 0;

                List<String> headers = csv.getHeaderNames();
                Sheet sheet = workbook.createSheet(sheetName);
                Row headerRow = sheet.createRow(rowNumber++);
                for (int i = 0; i < headers.size(); i++) {
                    headerRow.createCell(i).setCellValue(headers.get(i));
                }

                for (CSVRecord record : csv) {
                    List<String> entries = record.toList();
                    Row entryRow = sheet.createRow(rowNumber++);
                    for (int j = 0; j < entries.size(); j++) {
                        entryRow.createCell(j).setCellValue(entries.get(j));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks a given heading at every row for an expected value.
     *
     * @param inputFile The source csv file to check.
     * @param headingName The heading to check at each row.
     * @param expectedValue The expected value to verify against.
     * @return true if every row passes, else fail
     */
    public static boolean verify(Path inputFile, String headingName, String expectedValue) throws IOException {
        try (BufferedReader bfr = new BufferedReader(new FileReader(inputFile.toFile()))) {
            CSVParser csv = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).build().parse(bfr);
            for (CSVRecord record : csv) {
                String entry = record.get(headingName);
                if (!entry.equals(expectedValue)) return false;
            }
            return true;
        }
    }
}
