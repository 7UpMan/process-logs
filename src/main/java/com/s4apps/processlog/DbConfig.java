package com.s4apps.processlog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {

    private final Properties properties = new Properties();

    public DbConfig() {
        try (InputStream input = DbConfig.class.getClassLoader().getResourceAsStream("secrets.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing secrets.properties on the classpath");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load secrets.properties", ex);
        }
    }

    public String getUrl() {
        return getValue("db.url", "DB_URL");
    }

    public String getUser() {
        return getValue("db.user", "DB_USER");
    }

    public String getPassword() {
        return getValue("db.password", "DB_PASSWORD");
    }

    private String getValue(String key, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing value for " + key + " in secrets.properties");
        }

        return value;
    }
}
