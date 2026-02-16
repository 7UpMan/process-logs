package com.s4apps.processlog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.s4apps.processlog.model.ApacheLog;

/**
 * This RowStringStorage class holds String versions of the input data. All
 * fields are
 * stored as a Stirng
 * in an array, and there are getter methods to get the different fields. There
 * are also methods to
 * check if the row should be ignored or deleted, and a method to get the ignore
 * reason.
 * 
 * @author mat
 */
public class RowStringStorage {

    // Positions in the array of the different items
    private static final int IDX_IP = 0;
    private static final int IDX_DATE = 3;
    private static final int IDX_METHOD = 4;
    private static final int IDX_URL = 5;
    private static final int IDX_QUERY_STRING = 6;
    private static final int IDX_HTTP_VER = 7;
    private static final int IDX_RESPONSE = 8;
    private static final int IDX_SIZE = 9;
    private static final int IDX_SERVER = 10;
    private static final int IDX_SERVER2 = 11;
    private static final int IDX_BROWSER = 12;

    

    // Where to store the array. Use the IDX methods above to
    // get the different items out of the array.
    private final String[] rowColumns = new String[15];

    // If the record was read, hold the existing id
    private String existingId = null;

    // When using the pushCol function, holds current value
    private int nextCol = 0;

    // Hold the config data for later
    ConfigData cd;

    public RowStringStorage(ConfigData configData) {
        cd = configData;
    }

    /**
     * Create a row object by passing in a ResultSet that has the object
     * to create.
     * 
     * @param configData
     * @param rs
     * @throws java.sql.SQLException
     */
    public RowStringStorage(ConfigData configData, ResultSet rs) throws SQLException {
        cd = configData;
        existingId = rs.getString("id");

        rowColumns[IDX_IP] = rs.getString("ip");
        rowColumns[IDX_DATE] = new SimpleDateFormat(ToolsAndConstants.DATE_FORMAT_STRING).format(rs.getTimestamp("date"));
        rowColumns[IDX_METHOD] = rs.getString("method");
        rowColumns[IDX_URL] = rs.getString("url");
        rowColumns[IDX_QUERY_STRING] = rs.getString("queryString");
        rowColumns[IDX_RESPONSE] = rs.getString("response");
        rowColumns[IDX_SIZE] = rs.getString("size");
        rowColumns[IDX_SERVER] = rs.getString("server");
        rowColumns[IDX_SERVER2] = rs.getString("server2");
        rowColumns[IDX_BROWSER] = rs.getString("browser");
    }

    public RowStringStorage(ConfigData configData, ApacheLog log) {
        cd = configData;
        existingId = log.getId();

        rowColumns[IDX_IP] = log.getIp();
        rowColumns[IDX_DATE] = formatDate(log.getDate());
        rowColumns[IDX_METHOD] = log.getMethod();
        rowColumns[IDX_URL] = log.getUrl();
        rowColumns[IDX_QUERY_STRING] = log.getQueryString();
        rowColumns[IDX_RESPONSE] = toStringOrNull(log.getResponse());
        rowColumns[IDX_SIZE] = toStringOrNull(log.getSize());
        rowColumns[IDX_SERVER] = log.getServer();
        rowColumns[IDX_SERVER2] = log.getServer2();
        rowColumns[IDX_BROWSER] = log.getBrowser();
    }

    /**
     * Should this row be ignored because of issues with the IP address. We look for
     * an exact match in the list of IPs to ignore, so if the IP is in the list, it
     * will be
     * ignored.
     *
     * @return
     */
    public boolean ignoreIp() {
        return cd.ipsToIgnore().contains(getIp().toLowerCase());
    }

    /**
     * Should this row be ignored because of issues with the URL. We look for a
     * match at the beginning of the URL in the list of URLs to ignore, so if the
     * URL starts with
     * any of the entries in the list, it will be ignored.
     *
     * @return
     */
    public boolean ignoreUrl() {
        for (String findUrl : cd.urlsToIgnore()) {
            if (getUrl().toLowerCase().startsWith(findUrl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Should this row be ignored because of issues with the Server address. We look
     * for a match at the beginning of the Server address in the list of Servers to
     * ignore, so if the Server address starts with any of the entries in the list,
     * it will be ignored.
     *
     * @return
     */
    public boolean ignoreServer() {
        for (String findServer : cd.serversToIgnore()) {
            if (getServer().toLowerCase().startsWith(findServer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Should this row be ignored because of issues with the Method. We look for an
     * exact match in the list of Methods to ignore. Methods are stored in uppercase
     * in the configuration (normalised at load time), and the incoming method is
     * also uppercased before comparison, so the check is always case-insensitive in
     * practice.
     *
     * @return
     */
    public boolean ignoreMethod() {
        for (String findMethod : cd.methodsToIgnore()) {
            if (getMethod().toUpperCase().equals(findMethod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check out the row and return a flag to indicate if it should be skipped
     * or not. The flag is the product of a logical OR between all the reasons
     * why that specific row should be ignored.
     * 
     * A 0 means that the row should not be ignored, and a non-zero value means it
     * should be ignored, with the value indicating the reason why.
     *
     * @return Logical OR of the reasons to skip
     */
    public int getIgnoreReason() {
        int ignoreReasons = 0;

        // Check the IPs that we want to skip
        if (ignoreIp()) {
            ignoreReasons = ignoreReasons | ToolsAndConstants.REASON_IP;
        }

        // Check the beginning of the URL for ones to skip
        if (ignoreUrl()) {
            ignoreReasons = ignoreReasons | ToolsAndConstants.REASON_URL;
        }

        // Skip the servers that we are not interested in
        if (ignoreServer()) {
            ignoreReasons = ignoreReasons | ToolsAndConstants.REASON_SERVER;
        }

        // Skip the methods that we are not interested in
        if (ignoreMethod()) {
            ignoreReasons = ignoreReasons | ToolsAndConstants.REASON_METHOD;
        }

        return (ignoreReasons);
    }

    /**
     * Store the given column into the next available space.
     *
     * @param val
     */
    public void pushCol(String val) {
        rowColumns[nextCol++] = val;
    }

    public String getIp() {
        return rowColumns[IDX_IP];
    }

    public String getDate() {
        return rowColumns[IDX_DATE];
    }

    public String getMethod() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_METHOD]);
    }

    public String getUrl() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_URL]);
    }

    public String getQueryString() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_QUERY_STRING]);
    }

    public String getHttpVer() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_HTTP_VER]);
    }

    public String getResponse() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_RESPONSE]);
    }

    public String getSize() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_SIZE]);
    }

    public String getServer() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_SERVER]);
    }

    public String getServer2() {
        String server2 = ToolsAndConstants.nullToEmpty(rowColumns[IDX_SERVER2]);
        if (server2.length() > 200) {
            return server2.substring(0, 200);
        }
        return server2;
    }

    public String getBrowser() {
        return ToolsAndConstants.nullToEmpty(rowColumns[IDX_BROWSER]);
    }

    /**
     * A flag to indicate if this row should be deleted, that is not stored
     * on the output stream. Note that this is different from ignored
     * because ignored rows get saved but with an ignore flag. Deleted rows
     * don't get written to the output.
     * 
     * @return
     */
    public boolean isDeleteRow() {
        // Check IPs - needs to match exactly.
        if (cd.ipsToDelete().contains(getIp().toLowerCase())) {
            return true;
        }

        // Check URLs, needs to match the beginning of the URL
        for (String findUrl : cd.urlsToDelete()) {
            if (getUrl().toLowerCase().startsWith(findUrl)) {
                return true;
            }
        }

        // Check servers, needs to match the beginning of the server
        for (String findServer : cd.serversToDelete()) {
            if (getServer().toLowerCase().startsWith(findServer)) {
                return true;
            }
        }

        // Check the methods - uppercase on both sides for a consistent exact match
        if (cd.methodsToDelete().contains(getMethod().toUpperCase())) {
            return true;
        }

        // Otherwise, not a delete row
        return false;
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ToolsAndConstants.DATE_FORMAT_STRING);
        return dateTime.format(formatter);
    }

    private String toStringOrNull(Integer value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Return the line in a form appropriate for a CSV file, that is with escape
     * characters
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Add in the id
        sb.append("\"");
        sb.append(getId());
        sb.append("\"");

        // Add in everything within the array
        for (String rowColumn : rowColumns) {
            sb.append(",\"");
            sb.append(ToolsAndConstants.escapeCSV(rowColumn));
            sb.append('"');
        }

        // Add in the getIgnoreReason
        sb.append(",\"");
        sb.append(getIgnoreReason());
        sb.append("\"");

        return sb.toString();
    }

    /**
     * Returns an ID for the row which is a hash of all the data within it, or the
     * id
     * that it was read with if one exists.
     *
     * @return
     */
    public String getId() {
        if (existingId == null) {

            StringBuilder sb = new StringBuilder();

            // Put everything into 1 string
            for (String rowColumn : rowColumns) {
                sb.append(rowColumn);
            }

            // Return the hash
            return ToolsAndConstants.hash(sb.toString());

        } else {
            return existingId;
        }
    }

}
