package dev.anthonyashco.textutilities;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DatParser {
    private final CSVFormat csv;
    private final Path inputFile;
    private final char delimiter;

    public DatParser(Path inputFile, char delimiter) {
        csv = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true).setIgnoreSurroundingSpaces(true)
                .build();
        this.inputFile = inputFile;
        this.delimiter = delimiter;
    }

    public DatParser(Path inputFile) {
        csv = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader().setSkipHeaderRecord(true)
                .build();
        this.inputFile = inputFile;
        this.delimiter = ',';
    }

    public List<String> verifyDatFields(String... headings) throws IOException {
        try (BufferedReader bfr = new BufferedReader(new FileReader(inputFile.toFile()))) {
            List<String> errors = new ArrayList<>();

            CSVParser dat = csv.parse(bfr);
            List<String> datHeaders = dat.getHeaderNames();

            for (String heading : headings) {
                if (!datHeaders.contains(heading)) {
                    errors.add(String.format("Heading %s absent from file.", heading));
                }
            }

            return errors;
        }
    }

    public List<String> verifyDatFieldDataIsValid(String heading, String... validValues) throws IOException {
        List<String> valuesList = Arrays.asList(validValues);
        try (BufferedReader bfr = new BufferedReader(new FileReader(inputFile.toFile()))) {
            List<String> errors = new ArrayList<>();
            CSVParser dat = csv.parse(bfr);
            for (CSVRecord record : dat) {
                String data = record.get(heading);
                if (!valuesList.contains(data)) {
                    errors.add(String.format("Record %s:%s in row %d is invalid.", heading, data, record.getRecordNumber()));
                }
            }
            return errors;
        }
    }

    public Map<String, Map<String, String>> extract(String primaryKey, String... headings) throws IOException {
        try (BufferedReader bfr = new BufferedReader(new FileReader(inputFile.toFile()))) {
            Map<String, Map<String, String>> resultMap = new HashMap<>();
            CSVParser dat = csv.parse(bfr);
            if (headings.length == 0) {
                headings = dat.getHeaderNames().toArray(new String[0]);
            }
            for (CSVRecord record : dat) {
                try {
                    Map<String, String> entry = new HashMap<>();
                    for (String heading : headings) {
                        entry.put(heading, record.get(heading).trim());
                    }
                    resultMap.put(record.get(primaryKey).trim(), entry);
                } catch (IllegalArgumentException e) {
                    System.out.println("Erroneous record skipped: " + record.toString());
                }
            }
            return resultMap;
        }
    }

    public Map<String, Map<String, String>> extractComposite(String[] compositeKeys, String... headings) throws IOException {
        try (BufferedReader bfr = new BufferedReader(new FileReader(inputFile.toFile()))) {
            Map<String, Map<String, String>> resultMap = new HashMap<>();
            CSVParser dat = csv.parse(bfr);
            for (CSVRecord record : dat) {
                try {
                    Map<String, String> entry = new HashMap<>();
                    List<String> compositeKeyParts = new ArrayList<>();
                    for (String keyPart : compositeKeys) {
                        compositeKeyParts.add(record.get(keyPart));
                    }
                    for (String heading : headings) {
                        entry.put(heading, record.get(heading));
                    }
                    resultMap.put(String.join(String.valueOf(delimiter), compositeKeyParts), entry);
                } catch (IllegalArgumentException e){
                    System.out.println("Erroneous record skipped: " + record.toString());
                }
            }
            return resultMap;
        }
    }

    public Map<String, Map<String, String>> extractComposite(String compositeKey, String... headings) throws IOException {
        String[] compositeKeys = compositeKey.split(String.valueOf(delimiter));
        return extractComposite(compositeKeys, headings);
    }

    public static void main(String[] args) throws IOException {
        Path file = Paths.get("C:\\Users\\adccj0y\\Downloads\\IALM_TMP_REF_ODM_2022-04.csv");
        DatParser parser = new DatParser(file);
        Map<String, Map<String, String>> records = parser.extract("acct_id", "region", "lob", "segment_nm");
        System.out.println(records.size());
        System.out.println(records.get("5999259780").get("segment_nm"));

        Map<String, Map<String, String>> compositeRecords = parser.extractComposite("acct_id,region,ipid", "segment_od_core", "segment_nm");
        System.out.println(compositeRecords.size());
        Map<String, String> record;
        System.out.println(record = compositeRecords.get("3559129780,EU,0000344870250"));
        System.out.println(record.get("segment_od_core"));
        System.out.println(record.get("segment_nm"));
    }
}
