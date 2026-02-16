package com.s4apps.processlog;

import java.util.Collections;
import java.util.List;

/**
 * Immutable record that holds configuration for filtering and deleting log entries.
 * 
 * Using a record instead of a class gives us:
 * - Automatic constructor
 * - Automatic getters (ipsToIgnore(), not getIpsToIgnore())
 * - Automatic equals(), hashCode(), toString()
 * - Guaranteed immutability
 * - Much less boilerplate code
 * 
 * Use ConfigRepository to load an instance of this record from the database.
 * 
 * Note: Requires Java 16+ for records
 * 
 * @author mat
 */
public record ConfigData(
        List<String> ipsToIgnore,
        List<String> methodsToIgnore,
        List<String> serversToIgnore,
        List<String> urlsToIgnore,
        List<String> ipsToDelete,
        List<String> methodsToDelete,
        List<String> serversToDelete,
        List<String> urlsToDelete) {

    /**
     * Canonical constructor that makes all lists immutable.
     * This constructor is called automatically when you create a ConfigData.
     */
    public ConfigData {
        // Compact constructor - no need to assign fields, Java does it automatically
        // Just validate and make immutable
        ipsToIgnore = Collections.unmodifiableList(ipsToIgnore);
        methodsToIgnore = Collections.unmodifiableList(methodsToIgnore);
        serversToIgnore = Collections.unmodifiableList(serversToIgnore);
        urlsToIgnore = Collections.unmodifiableList(urlsToIgnore);
        ipsToDelete = Collections.unmodifiableList(ipsToDelete);
        methodsToDelete = Collections.unmodifiableList(methodsToDelete);
        serversToDelete = Collections.unmodifiableList(serversToDelete);
        urlsToDelete = Collections.unmodifiableList(urlsToDelete);
    }
    
    /**
     * Factory method for creating an empty configuration (useful for testing).
     */
    public static ConfigData empty() {
        return new ConfigData(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
    
    /**
     * Override the default toString() to provide a more concise summary.
     * Records give us a default toString(), but we can customize it.
     */
    @Override
    public String toString() {
        return String.format("ConfigData{ignore: %d IPs, %d methods, %d servers, %d URLs; " +
                           "delete: %d IPs, %d methods, %d servers, %d URLs}",
                ipsToIgnore.size(), methodsToIgnore.size(), 
                serversToIgnore.size(), urlsToIgnore.size(),
                ipsToDelete.size(), methodsToDelete.size(), 
                serversToDelete.size(), urlsToDelete.size());
    }
}