package com.s4apps.processlog;

/**
 *
 * @author mat
 */
public class Check {

    private static final String ERROR_PREFIX = "\n**There were ";
    private static final String CHECK1_MISMATCH = " results in check 1, but ";
    private static final String CHECK2_MISMATCH = " in check 2.  They should be the same";
    private static final String MATCHED_PREFIX = "Ok - Matched on ";
    private static final String ROWS_SUFFIX = " rows";
    private static final String OK = "Ok";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Checking database sanity");

        JpaAccess msa = new JpaAccess();

        try {
            runDeleteChecks(msa);
            runIgnoreChecks(msa);
        } finally {
            msa.close();
        }

    }

    private static void runDeleteChecks(JpaAccess msa) {
        System.out.println("\n\nChecking: Do we have any records that should have been deleted?");
        printDeleteCheck("IPs", msa.countDeleteIpMatches());
        printDeleteCheck("URLs", msa.countDeleteUrlMatches());
        printDeleteCheck("Servers", msa.countDeleteServerMatches());
        printDeleteCheck("Methods", msa.countDeleteMethodMatches());
    }

    private static void runIgnoreChecks(JpaAccess msa) {
        System.out.println("\n\nChecking: Do we have any ignore flags set wrong?");
        printMatchCheck("IPs", msa.countIgnoreIpMatches(), msa.countIgnoreIpFlagged());
        printMatchCheck("URLs", msa.countIgnoreUrlMatches(), msa.countIgnoreUrlFlagged());
        printMatchCheck("Servers", msa.countIgnoreServerMatches(), msa.countIgnoreServerFlagged());
        printMatchCheck("Methods", msa.countIgnoreMethodMatches(), msa.countIgnoreMethodFlagged());

        System.out.print("Checking ignoreReason adds up ... ");
        long mismatch = msa.countIgnoreReasonMismatch();
        if (mismatch != 0) {
            System.err.println("\n**There are " + mismatch + " ignoreReasons that don't add up");
        } else {
            System.out.println(OK);
        }
    }

    private static void printDeleteCheck(String label, long count) {
        System.out.print("- Checking " + label + " ... ");
        if (count != 0) {
            System.err.println(ERROR_PREFIX + count + " " + label + " that should have been deleted.");
        } else {
            System.out.println(OK);
        }
    }

    private static void printMatchCheck(String label, long expected, long actual) {
        System.out.print("- Checking " + label + " ... ");
        if (expected != actual) {
            System.err.println(ERROR_PREFIX + expected + CHECK1_MISMATCH + actual + CHECK2_MISMATCH);
        } else {
            System.out.println(MATCHED_PREFIX + expected + ROWS_SUFFIX);
        }
    }

}
