package com.s4apps.processlog;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the private process1Line() parser inside ProcessLog.
 *
 * Because the parser is a private method (it owns no instance state and could
 * be a utility, but it's currently private), we use reflection to call it
 * directly.  This lets us exercise the parsing logic in isolation without
 * needing a real file, database, or output destination.
 */
class ProcessLogParserTest {

    private Method parse;
    private ProcessLog processor;

    @BeforeEach
    void setUp() throws Exception {
        // CliOptions requires exactly one output specifier and at least one
        // input file name.  "-n" (null output) + a dummy filename satisfies
        // both constraints without touching the filesystem.
        processor = new ProcessLog(
                new CliOptions(new String[]{"-n", "dummy.log"}),
                ConfigData.empty());

        parse = ProcessLog.class.getDeclaredMethod(
                "process1Line", RowStringStorage.class, String.class);
        parse.setAccessible(true);
    }

    /** Convenience wrapper that invokes the private parser and returns the row. */
    private RowStringStorage parseLine(String logLine) throws Exception {
        RowStringStorage row = new RowStringStorage(ConfigData.empty());
        parse.invoke(processor, row, logLine);
        return row;
    }

    // -------------------------------------------------------------------------
    // Basic field extraction
    // -------------------------------------------------------------------------

    @Test
    void basicLogLine_ipExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "192.168.1.100 - - [01/Jan/2024:12:00:00 +0000] " +
                "\"GET /index.html HTTP/1.1\" 200 1234 \"-\" \"Mozilla/5.0\"");
        assertEquals("192.168.1.100", row.getIp());
    }

    @Test
    void basicLogLine_methodExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [15/Mar/2024:08:00:00 +0000] " +
                "\"POST /submit HTTP/1.1\" 201 512 \"-\" \"curl/7.68.0\"");
        assertEquals("POST", row.getMethod());
    }

    @Test
    void basicLogLine_urlExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [15/Mar/2024:08:00:00 +0000] " +
                "\"GET /about/us HTTP/1.1\" 200 800 \"-\" \"-\"");
        assertEquals("/about/us", row.getUrl());
    }

    @Test
    void basicLogLine_httpVersionExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jun/2024:10:00:00 +0000] " +
                "\"GET / HTTP/2\" 200 100 \"-\" \"-\"");
        assertEquals("HTTP/2", row.getHttpVer());
    }

    @Test
    void basicLogLine_responseCodeExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jun/2024:10:00:00 +0000] " +
                "\"GET / HTTP/1.1\" 404 0 \"-\" \"-\"");
        assertEquals("404", row.getResponse());
    }

    @Test
    void basicLogLine_sizeExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jun/2024:10:00:00 +0000] " +
                "\"GET / HTTP/1.1\" 200 98765 \"-\" \"-\"");
        assertEquals("98765", row.getSize());
    }

    @Test
    void basicLogLine_refererExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jun/2024:10:00:00 +0000] " +
                "\"GET /page HTTP/1.1\" 200 500 \"http://example.com/ref\" \"-\"");
        assertEquals("http://example.com/ref", row.getServer());
    }

    @Test
    void basicLogLine_userAgentExtractedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jun/2024:10:00:00 +0000] " +
                "\"GET / HTTP/1.1\" 200 100 \"-\" \"MyAgent/1.0\"");
        assertEquals("MyAgent/1.0", row.getServer2());
    }

    // -------------------------------------------------------------------------
    // Date parsing — Apache format to yyyy-MM-dd HH:mm:ss
    // -------------------------------------------------------------------------

    @Test
    void dateParsing_formattedCorrectly() throws Exception {
        RowStringStorage row = parseLine(
                "1.2.3.4 - - [01/Jan/2024:12:34:56 +0000] " +
                "\"GET / HTTP/1.1\" 200 0 \"-\" \"-\"");
        assertEquals("2024-01-01 12:34:56", row.getDate());
    }

    @Test
    void dateParsing_allTwelveMonthsConvertCorrectly() throws Exception {
        String[] months  = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        String[] numbers = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};

        for (int i = 0; i < months.length; i++) {
            String line = "1.2.3.4 - - [15/" + months[i] + "/2024:10:20:30 +0000] " +
                          "\"GET / HTTP/1.1\" 200 0 \"-\" \"-\"";
            RowStringStorage row = parseLine(line);
            assertEquals("2024-" + numbers[i] + "-15 10:20:30", row.getDate(),
                    "Month conversion failed for " + months[i]);
        }
    }

    // -------------------------------------------------------------------------
    // URL field splitting — method, path, query string, HTTP version
    // -------------------------------------------------------------------------

    @Test
    void urlWithQueryString_pathAndQueryExtractedSeparately() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jan/2024:08:00:00 +0000] " +
                "\"GET /search?q=hello&page=2 HTTP/1.1\" 200 1000 \"-\" \"-\"");
        assertEquals("/search", row.getUrl());
        assertEquals("q=hello&page=2", row.getQueryString());
    }

    @Test
    void urlWithoutQueryString_queryStringIsEmpty() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Jan/2024:08:00:00 +0000] " +
                "\"GET /plain HTTP/1.1\" 200 1000 \"-\" \"-\"");
        assertEquals("/plain", row.getUrl());
        assertEquals("", row.getQueryString());
    }

    // -------------------------------------------------------------------------
    // Quoted fields — spaces inside quotes treated as part of the field
    // -------------------------------------------------------------------------

    @Test
    void userAgentWithSpaces_parsedAsOneField() throws Exception {
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Mar/2024:09:00:00 +0000] " +
                "\"GET / HTTP/1.1\" 200 500 \"-\" " +
                "\"Mozilla/5.0 (Windows NT 10.0; Win64; x64)\"");
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", row.getServer2());
    }

    // -------------------------------------------------------------------------
    // Escape character handling — backslash before a character includes it raw
    // -------------------------------------------------------------------------

    @Test
    void escapedQuoteInsideUserAgent_includedLiterally() throws Exception {
        // Apache log line: ... "Agent\"Quoted\""
        // The backslash escapes the inner quote so it is appended as a literal "
        RowStringStorage row = parseLine(
                "10.0.0.1 - - [01/Mar/2024:09:00:00 +0000] " +
                "\"GET / HTTP/1.1\" 200 500 \"-\" \"Agent\\\"Quoted\\\"\"");
        assertEquals("Agent\"Quoted\"", row.getServer2());
    }
}
