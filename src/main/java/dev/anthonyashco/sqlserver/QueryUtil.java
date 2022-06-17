package dev.anthonyashco.sqlserver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * A utility class for handling SQL Server operations.
 */
public class QueryUtil {
    private final Connection conn;
    private boolean numberedOutput = false;
    private boolean zaiFormat = false;
    private final Path sqlInput = Paths.get("src", "test", "resources", "sqlqueries");
    private final Path csvOutput = Paths.get("src", "test", "resources", "csvsource");

    private CSVPrinter getCsvPrinter(String[] headers, Writer fw) throws IOException {
        if (zaiFormat) {
            return CSVFormat.Builder.create().setQuoteMode(QuoteMode.NONE).setEscape('\\').setDelimiter("~")
                    .setAllowDuplicateHeaderNames(true).setHeader(headers).build().print(fw);
        } else {
            return CSVFormat.Builder.create().setAllowDuplicateHeaderNames(true).setHeader(headers).build().print(fw);
        }
    }

    public QueryUtil(String connectionString) {
        conn = ConnectionUtil.connect(connectionString);
        assert conn != null;
        if (!Files.isDirectory(sqlInput)) {
            throw new NullPointerException(String.format("The sqlInput path %s doesn't exist!", sqlInput));
        }
    }

    public QueryUtil(String connectionString, Properties credentials) {
        conn = ConnectionUtil.connect(connectionString, credentials);
        assert conn != null;
        if (!Files.isDirectory(sqlInput)) {
            throw new NullPointerException(String.format("The sqlInput path %s doesn't exist!", sqlInput));
        }
    }

    public QueryUtil(String connectionString, String user, String pass) {
        conn = ConnectionUtil.connect(connectionString, user, pass);
        assert conn != null;
        if (!Files.isDirectory(sqlInput)) {
            throw new NullPointerException(String.format("The sqlInput path %s doesn't exist!", sqlInput));
        }
    }

    /**
     * If set to true, append a count to the end of the names for output files. If this is set to false and a query
     * outputs multiple times, all but the final output will be overwritten.
     */
    public void setNumberedOutput(boolean numberedOutput) {
        this.numberedOutput = numberedOutput;
    }

    /**
     * An idiosyncratic csv format used by a certain team. Should be set to false unless working with this specific
     * text format.
     */
    public void setZai(boolean zaiFormat) {
        this.zaiFormat = zaiFormat;
    }

    /**
     * Gets the most recent position cycle id from the database.
     */
    public String getPstnCycId(String table) throws SQLException {
        String stmt = "select max(pstn_cyc_id) from " + table;
        PreparedStatement ps = conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getString(1);
    }

    /**
     * Gets a PreparedStatement from a file.
     *
     * @param args String parameters to inject into the PreparedStatement
     */
    public PreparedStatement getStatement(String sqlFileLocation, String... args) throws IOException, SQLException {
        Path sqlPath = Paths.get(sqlFileLocation);
        return getStatement(sqlPath, args);
    }

    /**
     * Gets a PreparedStatement from a file.
     *
     * @param args String parameters to inject into the PreparedStatement
     */
    public PreparedStatement getStatement(Path sqlFilePath, String... args) throws IOException, SQLException {
        File file = sqlFilePath.toFile();
        try (BufferedReader bfr = new BufferedReader(new FileReader(file))) {
            String query = bfr.lines().collect(Collectors.joining(System.lineSeparator()));
            PreparedStatement ps = conn.prepareStatement(query);
            int i = 1;
            for (String arg : args) {
                ps.setString(i, arg);
                i++;
            }
            return ps;
        }
    }

    /**
     * Executes a PreparedStatement to a csv file.
     */
    public void executeToCsv(PreparedStatement ps, String outputFilename, String outputExtension) throws SQLException, IOException {
        Files.createDirectories(csvOutput);
        String outputPath;
        int i = 1;
        boolean isResultSet = ps.execute();

        while (true) {
            if (isResultSet) {
                if (numberedOutput) outputPath = String.format(outputFilename + "_%02d." + outputExtension, i);
                else outputPath = outputFilename + "." + outputExtension;
                try (BufferedWriter bfw = new BufferedWriter(new FileWriter(csvOutput.resolve(outputPath).toString()))) {
                    ResultSet rs = ps.getResultSet();
                    if (rs != null) {
                        ResultSetMetaData md = rs.getMetaData();
                        int columns = md.getColumnCount();
                        String[] headers = new String[columns];
                        for (int j = 1; j <= columns; j++) {
                            headers[j - 1] = md.getColumnLabel(j);
                        }

                        CSVPrinter csv = getCsvPrinter(headers, bfw);
                        while (rs.next()) {
                            String[] row = new String[columns];
                            for (int k = 1; k <= columns; k++) {
                                String cell = rs.getString(k);
                                row[k - 1] = (cell == null) ? "#####" : cell;
                            }
                            csv.printRecord((Object[]) row);
                        }
                        rs.close();
                    }
                }
                i++;
            } else {
                if (ps.getUpdateCount() == -1) {
                    break;
                }
            }
            isResultSet = ps.getMoreResults();
        }
    }
}
