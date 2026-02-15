package com.s4apps.processlog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tools {
    
    
    /**
     * Hash the value using SHA-256 and return the hex string. If the value is
     * null then null is returned. If SHA-256 is not available then a message is
     * returned to say so.
     * @param value
     * @return
     */
    public static String hash(String value) {
        if (value == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get its hash
            byte[] encodedhash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
            return "SHA-256 not available";
        }
    }

    /**
     * Apply escape characters to a field suitable for a CSV around the use of
     * the double quote mark (shift 2).
     *
     * @param field
     * @return
     */
    public static String escapeCSV(String field) {
        if (field == null) {
            return "";
        }
        if (field.equals("-")) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char ch = field.charAt(i);
            if (ch == '"') {
                sb.append('"');
                sb.append('"');
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    /**
     * Ensure a string is never returned null.
     *
     * @param input
     * @return
     */
    public static String nullToEmpty(String input) {
        if (input == null) {
            return "";
        }
        return input;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
