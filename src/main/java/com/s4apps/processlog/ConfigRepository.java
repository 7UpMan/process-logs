package com.s4apps.processlog;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository class responsible for loading configuration data from the database.
 * This separates the concern of data loading from the ConfigData class itself,
 * making the code more testable and following the Single Responsibility Principle.
 * 
 * @author mat
 */
public class ConfigRepository {
    
    private static final Logger logger = Logger.getLogger(ConfigRepository.class.getName());
    
    /**
     * Loads all configuration data from the database.
     * This method opens a database connection, retrieves all ignore and delete
     * rules, and then closes the connection before returning the data.
     * 
     * @return ConfigData object containing all configuration rules
     * @throws ConfigurationException if there's an error loading the configuration
     */
    public ConfigData load() {
        logger.info("Loading configuration from database...");
        
        JpaAccess jpa = null;
        try {
            jpa = new JpaAccess();
            
            // Load all the ignore rules. Methods are normalised to uppercase so
            // comparisons in RowStringStorage can use a simple equals() check.
            List<String> ipsToIgnore = jpa.getIgnoreIps();
            List<String> methodsToIgnore = jpa.getIgnoreMethods().stream().map(String::toUpperCase).toList();
            List<String> serversToIgnore = jpa.getIgnoreServers();
            List<String> urlsToIgnore = jpa.getIgnoreUrls();

            // Load all the delete rules. Methods are normalised to uppercase here too.
            List<String> ipsToDelete = jpa.getDeleteIps();
            List<String> methodsToDelete = jpa.getDeleteMethods().stream().map(String::toUpperCase).toList();
            List<String> serversToDelete = jpa.getDeleteServers();
            List<String> urlsToDelete = jpa.getDeleteUrls();

            logger.info(String.format("Configuration loaded successfully: %d ignore rules, %d delete rules",
                    ipsToIgnore.size() + methodsToIgnore.size() + serversToIgnore.size() + urlsToIgnore.size(),
                    ipsToDelete.size() + methodsToDelete.size() + serversToDelete.size() + urlsToDelete.size()));
            
            // Create and return the configuration data object
            return new ConfigData(
                    ipsToIgnore,
                    methodsToIgnore,
                    serversToIgnore,
                    urlsToIgnore,
                    ipsToDelete,
                    methodsToDelete,
                    serversToDelete,
                    urlsToDelete
            );
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load configuration from database", ex);
            throw new ConfigurationException("Unable to load configuration from database", ex);
        } finally {
            // Ensure the database connection is always closed
            if (jpa != null) {
                try {
                    jpa.close();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error closing JPA connection", ex);
                }
            }
        }
    }
    
    /**
     * Loads configuration with verbose logging for debugging purposes.
     * 
     * @param verbose if true, logs detailed information about each rule loaded
     * @return ConfigData object containing all configuration rules
     */
    public ConfigData load(boolean verbose) {
        if (!verbose) {
            return load();
        }
        
        logger.info("Loading configuration from database (verbose mode)...");
        
        JpaAccess jpa = null;
        try {
            jpa = new JpaAccess();
            
            // Load all the ignore rules
            List<String> ipsToIgnore = jpa.getIgnoreIps();
            if (verbose) {
                logger.info("Loaded " + ipsToIgnore.size() + " IPs to ignore: " + ipsToIgnore);
            }
            
            List<String> methodsToIgnore = jpa.getIgnoreMethods().stream().map(String::toUpperCase).toList();
            if (verbose) {
                logger.info("Loaded " + methodsToIgnore.size() + " methods to ignore: " + methodsToIgnore);
            }
            
            List<String> serversToIgnore = jpa.getIgnoreServers();
            if (verbose) {
                logger.info("Loaded " + serversToIgnore.size() + " servers to ignore: " + serversToIgnore);
            }
            
            List<String> urlsToIgnore = jpa.getIgnoreUrls();
            if (verbose) {
                logger.info("Loaded " + urlsToIgnore.size() + " URLs to ignore: " + urlsToIgnore);
            }
            
            // Load all the delete rules
            List<String> ipsToDelete = jpa.getDeleteIps();
            if (verbose) {
                logger.info("Loaded " + ipsToDelete.size() + " IPs to delete: " + ipsToDelete);
            }
            
            List<String> methodsToDelete = jpa.getDeleteMethods().stream().map(String::toUpperCase).toList();
            if (verbose) {
                logger.info("Loaded " + methodsToDelete.size() + " methods to delete: " + methodsToDelete);
            }
            
            List<String> serversToDelete = jpa.getDeleteServers();
            if (verbose) {
                logger.info("Loaded " + serversToDelete.size() + " servers to delete: " + serversToDelete);
            }
            
            List<String> urlsToDelete = jpa.getDeleteUrls();
            if (verbose) {
                logger.info("Loaded " + urlsToDelete.size() + " URLs to delete: " + urlsToDelete);
            }
            
            // Create and return the configuration data object
            return new ConfigData(
                    ipsToIgnore,
                    methodsToIgnore,
                    serversToIgnore,
                    urlsToIgnore,
                    ipsToDelete,
                    methodsToDelete,
                    serversToDelete,
                    urlsToDelete
            );
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load configuration from database", ex);
            throw new ConfigurationException("Unable to load configuration from database", ex);
        } finally {
            // Ensure the database connection is always closed
            if (jpa != null) {
                try {
                    jpa.close();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error closing JPA connection", ex);
                }
            }
        }
    }
    
    /**
     * Exception thrown when configuration cannot be loaded.
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}