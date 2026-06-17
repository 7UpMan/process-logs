package com.s4apps.processlog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream in = Version.class.getResourceAsStream("/version.properties")) {
            if (in == null) {
                System.out.println("Version information not available");
                return;
            }
            props.load(in);
            System.out.println("Version: " + props.getProperty("build.version", "unknown"));
            System.out.println("Built:   " + props.getProperty("build.timestamp", "unknown"));
        } catch (IOException e) {
            System.out.println("Error reading version: " + e.getMessage());
        }
    }
}
