package com.s4apps.processlog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mat
 */
public class RebuildNew {
    private static final Logger logger = Logger.getLogger(RebuildNew.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Rebuilding the database ...\n");

        JpaAccess msa = new JpaAccess();

        // Store now for later so we can work out how long it took
        LocalDateTime startDateTime = LocalDateTime.now();

        // CHANGE: Use the ConfigRepository to load config data
        ConfigRepository configRepo = new ConfigRepository();
        ConfigData cd;
        
        try {
            // Load configuration - can pass verbose flag from CLI options
            cd = configRepo.load();
            
            // Optional: Log the configuration summary
            logger.info("Configuration loaded: " + cd.toString());
            
        } catch (ConfigRepository.ConfigurationException ex) {
            // Handle configuration loading errors
            System.err.println("FATAL: Unable to load configuration from database");
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
            return; // Won't reach here, but keeps compiler happy
        }

        try {
            processRows(msa, cd);
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Error processing rows", ex);
        }

        Duration duration = Duration.between(startDateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        System.out.print("That took " + seconds / 60);
        System.out.print(" minutes and " + (seconds % 60) + " seconds\n");

        msa.close();

        System.exit(0);
    }

    private static void processRows(JpaAccess msa, ConfigData cd) {
        int rowCounter = 0;
        RowStringStorage rowStringStorage = msa.getAllRows(cd);

        // Iterate through all of the log entries, processing each one as we go
        while (rowStringStorage != null) {
            applyRowChange(msa, rowStringStorage);
            rowCounter++;

            // Commit every 1,000 rows
            if (rowCounter % ToolsAndConstants.COMMIT_FREQUENCY == 0) {
                msa.commit();
                System.out.print("*");
                System.out.flush();
                if (rowCounter % (ToolsAndConstants.PROGRESS_FREQUENCY_SLOW)  == 0) {
                    System.out.print("\n");
                }
            }

            rowStringStorage = msa.getAllRows(cd);
        }
        System.out.println();
    }

    private static void applyRowChange(JpaAccess msa, RowStringStorage rowStringStorage) {
        if (rowStringStorage.isDeleteRow()) {
            int rowsDeleted = msa.delRow(rowStringStorage);
            if (rowsDeleted != 1) {
                System.err.println("Error deleting row: " + rowStringStorage.toString());
            }
            return;
        }

        int rowsWritten = msa.updateRowFlags(rowStringStorage);
        if (rowsWritten != 1) {
            System.err.println("Error " + rowsWritten + " with row: " + rowStringStorage.toString());
        }
    }

}
