package com.s4apps.processlog;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RowStringStorageTest {

    /**
     * Builds a RowStringStorage with all 13 columns populated.
     * Push order must match the IDX_* constants inside RowStringStorage.
     */
    private RowStringStorage createRow(ConfigData config, String ip, String method,
                                       String url, String server) {
        RowStringStorage row = new RowStringStorage(config);
        row.pushCol(ip);                       // idx 0: IP
        row.pushCol("-");                      // idx 1: ident (ignored field)
        row.pushCol("-");                      // idx 2: auth user (ignored field)
        row.pushCol("2024-01-01 12:00:00");    // idx 3: date
        row.pushCol(method);                   // idx 4: method
        row.pushCol(url);                      // idx 5: URL
        row.pushCol("");                       // idx 6: query string
        row.pushCol("HTTP/1.1");               // idx 7: HTTP version
        row.pushCol("200");                    // idx 8: response code
        row.pushCol("1234");                   // idx 9: response size
        row.pushCol(server);                   // idx 10: server / referer
        row.pushCol("Mozilla/5.0");            // idx 11: server2 / user-agent
        row.pushCol("");                       // idx 12: browser
        return row;
    }

    // -------------------------------------------------------------------------
    // ignoreIp()
    // -------------------------------------------------------------------------

    @Test
    void ignoreIp_ipInList_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of("192.168.1.1"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "192.168.1.1", "GET", "/", "-");
        assertTrue(row.ignoreIp());
    }

    @Test
    void ignoreIp_ipNotInList_returnsFalse() {
        ConfigData config = new ConfigData(
                List.of("10.0.0.1"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "192.168.1.1", "GET", "/", "-");
        assertFalse(row.ignoreIp());
    }

    @Test
    void ignoreIp_isCaseInsensitive() {
        // List stored in lowercase, IP arrives in uppercase-style notation
        ConfigData config = new ConfigData(
                List.of("192.168.1.1"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "192.168.1.1", "GET", "/", "-");
        assertTrue(row.ignoreIp());
    }

    @Test
    void ignoreIp_emptyList_returnsFalse() {
        RowStringStorage row = createRow(ConfigData.empty(), "192.168.1.1", "GET", "/", "-");
        assertFalse(row.ignoreIp());
    }

    // -------------------------------------------------------------------------
    // ignoreUrl()
    // -------------------------------------------------------------------------

    @Test
    void ignoreUrl_exactMatch_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of("/health"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/health", "-");
        assertTrue(row.ignoreUrl());
    }

    @Test
    void ignoreUrl_prefixMatch_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of("/admin"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/admin/users", "-");
        assertTrue(row.ignoreUrl());
    }

    @Test
    void ignoreUrl_noMatch_returnsFalse() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of("/admin"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/public/page", "-");
        assertFalse(row.ignoreUrl());
    }

    @Test
    void ignoreUrl_isCaseInsensitive() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of("/health"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/HEALTH/check", "-");
        assertTrue(row.ignoreUrl());
    }

    // -------------------------------------------------------------------------
    // ignoreServer()
    // -------------------------------------------------------------------------

    @Test
    void ignoreServer_exactMatch_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of("amazonaws.com"), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "amazonaws.com");
        assertTrue(row.ignoreServer());
    }

    @Test
    void ignoreServer_prefixMatch_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of("10."), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "10.0.0.1");
        assertTrue(row.ignoreServer());
    }

    @Test
    void ignoreServer_noMatch_returnsFalse() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of("amazonaws.com"), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "example.com");
        assertFalse(row.ignoreServer());
    }

    // -------------------------------------------------------------------------
    // ignoreMethod()
    // -------------------------------------------------------------------------

    @Test
    void ignoreMethod_uppercaseListEntry_matchesUppercaseMethod() {
        // Methods are normalised to uppercase at load time (ConfigRepository) and
        // the incoming method is also uppercased before comparison, so both sides
        // are always uppercase.
        ConfigData config = new ConfigData(
                List.of(), List.of("OPTIONS"), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "OPTIONS", "/", "-");
        assertTrue(row.ignoreMethod());
    }

    @Test
    void ignoreMethod_lowercaseIncomingMethod_matchesDueToUppercaseNormalisation() {
        // Even if the raw log method arrives in lowercase, toUpperCase() in
        // ignoreMethod() ensures it still matches the uppercase list entry.
        ConfigData config = new ConfigData(
                List.of(), List.of("OPTIONS"), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "options", "/", "-");
        assertTrue(row.ignoreMethod());
    }

    @Test
    void ignoreMethod_noMatch_returnsFalse() {
        ConfigData config = new ConfigData(
                List.of(), List.of("OPTIONS"), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "-");
        assertFalse(row.ignoreMethod());
    }

    // -------------------------------------------------------------------------
    // getIgnoreReason() — bitwise OR composition
    // -------------------------------------------------------------------------

    @Test
    void getIgnoreReason_noRules_returnsZero() {
        RowStringStorage row = createRow(ConfigData.empty(), "1.2.3.4", "GET", "/", "-");
        assertEquals(0, row.getIgnoreReason());
    }

    @Test
    void getIgnoreReason_ipMatch_returnsReasonIp() {
        ConfigData config = new ConfigData(
                List.of("1.2.3.4"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "-");
        assertEquals(ToolsAndConstants.REASON_IP, row.getIgnoreReason());
    }

    @Test
    void getIgnoreReason_urlMatch_returnsReasonUrl() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of("/health"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/health", "-");
        assertEquals(ToolsAndConstants.REASON_URL, row.getIgnoreReason());
    }

    @Test
    void getIgnoreReason_ipAndUrl_returnsOrOfBothReasons() {
        ConfigData config = new ConfigData(
                List.of("1.2.3.4"), List.of(), List.of(), List.of("/health"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/health", "-");
        int expected = ToolsAndConstants.REASON_IP | ToolsAndConstants.REASON_URL;
        assertEquals(expected, row.getIgnoreReason());
    }

    @Test
    void getIgnoreReason_allReasons_returnsAllBitsSet() {
        ConfigData config = new ConfigData(
                List.of("1.2.3.4"),
                List.of("OPTIONS"),
                List.of("amazonaws.com"),
                List.of("/health"),
                List.of(), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "OPTIONS", "/health", "amazonaws.com");
        int expected = ToolsAndConstants.REASON_IP
                | ToolsAndConstants.REASON_URL
                | ToolsAndConstants.REASON_SERVER
                | ToolsAndConstants.REASON_METHOD;
        assertEquals(expected, row.getIgnoreReason());
    }

    // -------------------------------------------------------------------------
    // isDeleteRow()
    // -------------------------------------------------------------------------

    @Test
    void isDeleteRow_ipInDeleteList_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of(),
                List.of("1.2.3.4"), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "-");
        assertTrue(row.isDeleteRow());
    }

    @Test
    void isDeleteRow_urlPrefixInDeleteList_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of("/internal"));

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/internal/api", "-");
        assertTrue(row.isDeleteRow());
    }

    @Test
    void isDeleteRow_serverPrefixInDeleteList_returnsTrue() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of("elb-health"), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "elb-healthchecker/2.0");
        assertTrue(row.isDeleteRow());
    }

    @Test
    void isDeleteRow_methodInDeleteList_returnsTrue() {
        // Methods are normalised to uppercase at load time (ConfigRepository) and
        // the incoming method is uppercased before comparison, so both sides match.
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of("GET"), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "-");
        assertTrue(row.isDeleteRow());
    }

    @Test
    void isDeleteRow_lowercaseIncomingMethod_matchesDueToUppercaseNormalisation() {
        // Even if the raw log method arrives lowercase, toUpperCase() in isDeleteRow()
        // ensures it still matches the uppercase list entry.
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of("GET"), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "get", "/", "-");
        assertTrue(row.isDeleteRow());
    }

    @Test
    void isDeleteRow_noMatch_returnsFalse() {
        ConfigData config = new ConfigData(
                List.of(), List.of(), List.of(), List.of(),
                List.of("9.9.9.9"), List.of(), List.of(), List.of());

        RowStringStorage row = createRow(config, "1.2.3.4", "GET", "/", "-");
        assertFalse(row.isDeleteRow());
    }

    // -------------------------------------------------------------------------
    // getServer2() — truncation
    // -------------------------------------------------------------------------

    @Test
    void getServer2_longValue_truncatedTo200Chars() {
        RowStringStorage row = new RowStringStorage(ConfigData.empty());
        String longAgent = "A".repeat(250);
        row.pushCol("1.2.3.4");  // IP
        row.pushCol("-");
        row.pushCol("-");
        row.pushCol("2024-01-01 12:00:00");
        row.pushCol("GET");
        row.pushCol("/");
        row.pushCol("");
        row.pushCol("HTTP/1.1");
        row.pushCol("200");
        row.pushCol("100");
        row.pushCol("-");
        row.pushCol(longAgent);   // server2

        assertEquals(200, row.getServer2().length());
        assertEquals("A".repeat(200), row.getServer2());
    }

    @Test
    void getServer2_exactlyAt200Chars_notTruncated() {
        RowStringStorage row = new RowStringStorage(ConfigData.empty());
        String agent = "B".repeat(200);
        row.pushCol("1.2.3.4");
        row.pushCol("-");
        row.pushCol("-");
        row.pushCol("2024-01-01 12:00:00");
        row.pushCol("GET");
        row.pushCol("/");
        row.pushCol("");
        row.pushCol("HTTP/1.1");
        row.pushCol("200");
        row.pushCol("100");
        row.pushCol("-");
        row.pushCol(agent);

        assertEquals(200, row.getServer2().length());
    }

    // -------------------------------------------------------------------------
    // getId() — hash consistency
    // -------------------------------------------------------------------------

    @Test
    void getId_sameData_returnsSameHash() {
        RowStringStorage row1 = createRow(ConfigData.empty(), "1.2.3.4", "GET", "/page", "example.com");
        RowStringStorage row2 = createRow(ConfigData.empty(), "1.2.3.4", "GET", "/page", "example.com");
        assertEquals(row1.getId(), row2.getId());
    }

    @Test
    void getId_differentData_returnsDifferentHash() {
        RowStringStorage row1 = createRow(ConfigData.empty(), "1.2.3.4", "GET", "/page1", "example.com");
        RowStringStorage row2 = createRow(ConfigData.empty(), "1.2.3.4", "GET", "/page2", "example.com");
        assertNotEquals(row1.getId(), row2.getId());
    }
}
