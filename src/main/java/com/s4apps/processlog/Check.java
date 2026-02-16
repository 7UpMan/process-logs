package com.s4apps.processlog;

/**
 * Validates database integrity by checking that:
 * - No records exist that should have been deleted.
 * - Ignore flags match the data they describe.
 * - The ignoreReason bitmask is consistent with the individual flags.
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

    private final JpaAccess jpaAccess;

    /**
     * Constructor that takes all dependencies.
     *
     * @param jpaAccess the database access object to use for all queries
     */
    public Check(JpaAccess jpaAccess) {
        if (jpaAccess == null) {
            throw new IllegalArgumentException("JpaAccess cannot be null");
        }
        this.jpaAccess = jpaAccess;
    }

    /**
     * Main entry point - creates an instance and runs it.
     * This is the ONLY static method we need.
     *
     * @param args the command line arguments (not used)
     */
    public static void main(String[] args) {
        JpaAccess jpa = new JpaAccess();
        try {
            new Check(jpa).run();
        } finally {
            jpa.close();
        }
    }

    /**
     * Runs all integrity checks against the database.
     */
    public void run() {
        System.out.println("Checking database sanity");
        runDeleteChecks();
        runIgnoreChecks();
    }

    private void runDeleteChecks() {
        System.out.println("\n\nChecking: Do we have any records that should have been deleted?");
        printDeleteCheck("IPs", jpaAccess.countDeleteIpMatches());
        printDeleteCheck("URLs", jpaAccess.countDeleteUrlMatches());
        printDeleteCheck("Servers", jpaAccess.countDeleteServerMatches());
        printDeleteCheck("Methods", jpaAccess.countDeleteMethodMatches());
    }

    private void runIgnoreChecks() {
        System.out.println("\n\nChecking: Do we have any ignore flags set wrong?");
        printMatchCheck("IPs", jpaAccess.countIgnoreIpMatches(), jpaAccess.countIgnoreIpFlagged());
        printMatchCheck("URLs", jpaAccess.countIgnoreUrlMatches(), jpaAccess.countIgnoreUrlFlagged());
        printMatchCheck("Servers", jpaAccess.countIgnoreServerMatches(), jpaAccess.countIgnoreServerFlagged());
        printMatchCheck("Methods", jpaAccess.countIgnoreMethodMatches(), jpaAccess.countIgnoreMethodFlagged());

        System.out.print("Checking ignoreReason adds up ... ");
        long mismatch = jpaAccess.countIgnoreReasonMismatch();
        if (mismatch != 0) {
            System.err.println("\n**There are " + mismatch + " ignoreReasons that don't add up");
        } else {
            System.out.println(OK);
        }
    }

    private void printDeleteCheck(String label, long count) {
        System.out.print("- Checking " + label + " ... ");
        if (count != 0) {
            System.err.println(ERROR_PREFIX + count + " " + label + " that should have been deleted.");
        } else {
            System.out.println(OK);
        }
    }

    private void printMatchCheck(String label, long expected, long actual) {
        System.out.print("- Checking " + label + " ... ");
        if (expected != actual) {
            System.err.println(ERROR_PREFIX + expected + CHECK1_MISMATCH + actual + CHECK2_MISMATCH);
        } else {
            System.out.println(MATCHED_PREFIX + expected + ROWS_SUFFIX);
        }
    }
}
