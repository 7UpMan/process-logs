
package com.s4apps.processlog;

/**
 *
 * @author mat
 */
public class TestCli {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CliOptions clio = new CliOptions(args);

        if (clio.isWriteStdErr()) {
            System.out.println("'-e' flag was set");
        }

        if (clio.isWriteDatabase()) {
            System.out.println("'-d' flag was set");
        }

        if (clio.isWriteStdOut()) {
            System.out.println("'-o' flag was set");
        }

        if (clio.getOutFile() != null) {
            System.out.println("File was set to " + clio.getOutFile());
        }

        if (clio.getInFileNames().length > 0) {
            for (String s : clio.getInFileNames()) {
                System.out.println("File name=" + s);
            }
        }

        System.out.println("There were " + args.length + " arguemnts");
        System.out.println("Complete, no error");
    }

}
