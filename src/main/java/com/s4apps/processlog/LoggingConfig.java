package com.s4apps.processlog;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public final class LoggingConfig {

    private static final String CONFIG_RESOURCE = "logging.properties";

    private LoggingConfig() {
    }

    public static void configure() {
        if (System.getProperty("java.util.logging.config.file") != null) {
            return;
        }

        try (InputStream input = LoggingConfig.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                return;
            }
            LogManager.getLogManager().readConfiguration(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load logging configuration", ex);
        }
    }
}
