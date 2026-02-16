package com.s4apps.processlog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rebuilds the database by re-evaluating every stored log entry against the
 * current ignore/delete rules.  Rows that now match a delete rule are removed;
 * all other rows have their ignore flags updated in place.
 *
 * @author mat
 */
public class Rebuild {

    private static final Logger logger = Logger.getLogger(Rebuild.class.getName());

    private final JpaAccess jpaAccess;
    private final ConfigData config;

    /**
     * Constructor that takes all dependencies.
     *
     * @param jpaAccess the database access object to use for all queries
     * @param config    the current filtering rules to apply during rebuild
     */
    public Rebuild(JpaAccess jpaAccess, ConfigData config) {
        if (jpaAccess == null) {
            throw new IllegalArgumentException("JpaAccess cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("ConfigData cannot be null");
        }
        this.jpaAccess = jpaAccess;
        this.config = config;
    }

    /**
     * Main entry point - creates an instance and runs it.
     * This is the ONLY static method we need.
     *
     * @param args the command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Rebuilding the database ...\n");

        ConfigRepository configRepo = new ConfigRepository();
        ConfigData cd;

        try {
            cd = configRepo.load();
            logger.info("Configuration loaded: " + cd.toString());
        } catch (ConfigRepository.ConfigurationException ex) {
            System.err.println("FATAL: Unable to load configuration from database");
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
            return; // keeps compiler happy
        }

        JpaAccess jpa = new JpaAccess();
        try {
            new Rebuild(jpa, cd).run();
        } finally {
            jpa.close();
        }

        System.exit(0);
    }

    /**
     * Runs the full rebuild, then prints how long it took.
     */
    public void run() {
        LocalDateTime startDateTime = LocalDateTime.now();

        try {
            processRows();
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Error processing rows", ex);
        }

        Duration duration = Duration.between(startDateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        System.out.print("That took " + seconds / 60);
        System.out.print(" minutes and " + (seconds % 60) + " seconds\n");
    }

    private void processRows() {
        int rowCounter = 0;
        RowStringStorage rowStringStorage = jpaAccess.getAllRows(config);

        while (rowStringStorage != null) {
            applyRowChange(rowStringStorage);
            rowCounter++;

            if (rowCounter % ToolsAndConstants.COMMIT_FREQUENCY == 0) {
                jpaAccess.commit();
                System.out.print("*");
                System.out.flush();
                if (rowCounter % ToolsAndConstants.PROGRESS_FREQUENCY_SLOW == 0) {
                    System.out.print("\n");
                }
            }

            rowStringStorage = jpaAccess.getAllRows(config);
        }
        System.out.println();
    }

    private void applyRowChange(RowStringStorage rowStringStorage) {
        if (rowStringStorage.isDeleteRow()) {
            int rowsDeleted = jpaAccess.delRow(rowStringStorage);
            if (rowsDeleted != 1) {
                System.err.println("Error deleting row: " + rowStringStorage.toString());
            }
            return;
        }

        int rowsWritten = jpaAccess.updateRowFlags(rowStringStorage);
        if (rowsWritten != 1) {
            System.err.println("Error " + rowsWritten + " with row: " + rowStringStorage.toString());
        }
    }
}
