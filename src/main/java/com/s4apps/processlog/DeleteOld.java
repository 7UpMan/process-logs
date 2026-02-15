package com.s4apps.processlog;

import java.time.LocalDateTime;

/**
 *
 * @author mat
 */
public class DeleteOld {
    private static final int MAX_AGE = 180;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Deleteing old rows ...\n");

        JpaAccess msa = new JpaAccess();

        // When is now
        LocalDateTime now = LocalDateTime.now();

        // Get the earliest rerocd allowed
        LocalDateTime earliestRecord = now.minusDays(MAX_AGE);
        int rowsDeleted;
        try {
            rowsDeleted = msa.delOld(earliestRecord);
            msa.commit();
            System.out.println("Deleted " + rowsDeleted + " rows.");
        } catch (RuntimeException ex) {
            System.err.println("Unable to run delete statement, got error: " + ex.getMessage());
        } finally {
            msa.close();
        }

    }

}
