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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Rebuilding the database ...\n");

        JpaAccess msa = new JpaAccess();

        // Store now for later so we can work out how long it took
        LocalDateTime startDateTime = LocalDateTime.now();

        // Get the ConfigData
        ConfigData cd = new ConfigData();

        try {
            processRows(msa, cd);
        } catch (RuntimeException ex) {
            Logger.getLogger(RebuildNew.class.getName()).log(Level.SEVERE, null, ex);
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
            if (rowCounter % 1000 == 0) {
                msa.commit();
                System.out.print("*");
                System.out.flush();
                if (rowCounter % 50000 == 0) {
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
