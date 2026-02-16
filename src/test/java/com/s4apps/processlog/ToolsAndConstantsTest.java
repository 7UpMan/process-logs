package com.s4apps.processlog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolsAndConstantsTest {

    // --- hash() ---

    @Test
    void hash_null_returnsNull() {
        assertNull(ToolsAndConstants.hash(null));
    }

    @Test
    void hash_knownValue_returnsCorrectSha256() {
        // SHA-256("hello") is a well-known value
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        assertEquals(expected, ToolsAndConstants.hash("hello"));
    }

    @Test
    void hash_emptyString_returnsHash() {
        // SHA-256("") should produce a consistent non-null result
        String result = ToolsAndConstants.hash("");
        assertNotNull(result);
        assertEquals(64, result.length()); // SHA-256 hex is always 64 chars
    }

    @Test
    void hash_sameInput_returnsSameHash() {
        String a = ToolsAndConstants.hash("some data");
        String b = ToolsAndConstants.hash("some data");
        assertEquals(a, b);
    }

    @Test
    void hash_differentInputs_returnDifferentHashes() {
        assertNotEquals(ToolsAndConstants.hash("foo"), ToolsAndConstants.hash("bar"));
    }

    // --- escapeCSV() ---

    @Test
    void escapeCSV_null_returnsEmpty() {
        assertEquals("", ToolsAndConstants.escapeCSV(null));
    }

    @Test
    void escapeCSV_dash_returnsEmpty() {
        // A bare "-" in Apache logs means "no value"; should be treated as empty
        assertEquals("", ToolsAndConstants.escapeCSV("-"));
    }

    @Test
    void escapeCSV_plainString_returnsUnchanged() {
        assertEquals("hello world", ToolsAndConstants.escapeCSV("hello world"));
    }

    @Test
    void escapeCSV_stringWithDoubleQuote_doublesTheQuote() {
        // CSV escaping requires " to become ""
        assertEquals("say \"\"hello\"\"", ToolsAndConstants.escapeCSV("say \"hello\""));
    }

    @Test
    void escapeCSV_onlyDoubleQuote_returnsDoubledQuote() {
        assertEquals("\"\"", ToolsAndConstants.escapeCSV("\""));
    }

    // --- nullToEmpty() ---

    @Test
    void nullToEmpty_null_returnsEmpty() {
        assertEquals("", ToolsAndConstants.nullToEmpty(null));
    }

    @Test
    void nullToEmpty_emptyString_returnsEmpty() {
        assertEquals("", ToolsAndConstants.nullToEmpty(""));
    }

    @Test
    void nullToEmpty_value_returnsValue() {
        assertEquals("foo", ToolsAndConstants.nullToEmpty("foo"));
    }
}
