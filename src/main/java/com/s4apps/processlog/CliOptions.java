package com.s4apps.processlog;

import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author mat
 */
public class CliOptions {

    private boolean writeStdErr = false;
    private String outFile = null;
    private boolean writeStdOut = false;
    private boolean writeDatabase = false;
    private boolean writeNull = false;
    private String[] inFileNames;
    private boolean verbose = false;

    /**
     * Parse the CLI options and set flags and variables that can be quickly
     * polled to get the status. This also enforces rules around options
     * that can be used together.
     * 
     * @param args
     */
    public CliOptions(String[] args) {
        // Prepare the options that are available
        Options options = new Options();
        Option opt;
        OptionGroup outputOptions = new OptionGroup();
        outputOptions.setRequired(true); // Can only have one destination

        // Help message
        opt = new Option("?", "help", false, "display this help message");
        opt.setRequired(false);
        options.addOption(opt);

        // Verbost output
        // Help message
        opt = new Option("v", "verbose", false, "display verbose output");
        opt.setRequired(false);
        options.addOption(opt);

        // ** Build an option group so that we get one output **

        // Output to Standard err
        opt = new Option("e", "err", false, "write the output in CSV format to Std Err");
        opt.setRequired(false);
        outputOptions.addOption(opt);

        // Output to Standard out
        opt = new Option("o", "out", false, "write the output in CSV format to Std Out");
        opt.setRequired(false);
        outputOptions.addOption(opt);

        // Output to null, i.e. nowhere
        opt = new Option("n", "null", false, "write the output to null, i.e. nowhere");
        opt.setRequired(false);
        outputOptions.addOption(opt);

        // Output to a database
        opt = new Option("d", "database", false, "write the output to the database");
        opt.setRequired(false);
        outputOptions.addOption(opt);

        // Output to file
        opt = new Option("f", "outfile", true, "write the output in CSV format to specified file");
        opt.setArgName("output file");
        outputOptions.addOption(opt);

        // Add the group to the options
        options.addOptionGroup(outputOptions);

        // Prepare what we have
        CommandLineParser parser = new DefaultParser();

        // Now parse the output
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            System.err.println("Error parsing command line");
            System.err.println("Error was:" + ex.getMessage());
            displayHelpMessage(options);

            // Now exit with error
            System.exit(1);
        }

        // Set some internal flags based on the output of the parse
        if (cmd.hasOption("e")) {
            writeStdErr = true;
        }

        // Set some internal flags based on the output of the parse
        if (cmd.hasOption("o")) {
            writeStdOut = true;
        }

        // Set some internal flags based on the output of the parse
        if (cmd.hasOption("n")) {
            writeNull = true;
        }

        // Set some internal flags based on the outpu of the parse
        if (cmd.hasOption("d")) {
            writeDatabase = true;
        }

        // Set some internal flags based on the outpu of the parse
        if (cmd.hasOption("v")) {
            verbose = true;
        }

        // Check for help message
        if (cmd.hasOption("?")) {
            displayHelpMessage(options);

            // Exit success
            System.exit(0);
        }

        // See if a file name was specified
        if (cmd.hasOption("f")) {
            outFile = cmd.getOptionValue("outfile");
        }

        // Process what is left which should be one or more input file names
        inFileNames = cmd.getArgs();
        if (inFileNames.length < 1) {
            System.err.println("Missing <input file>.");
            displayHelpMessage(options);

            // Exit success
            System.exit(1);
        }
    }

    private void displayHelpMessage(Options options) {
        // automatically generate the help statement
        HelpFormatter formatter = HelpFormatter.builder().get();
        try {
            formatter.printHelp(
                "ProcessLog [options] <one output specifier> <input file1> [input file2 [input file3 ...]]\n"
                    + "ProcessLog --help",
                null,
                options,
                null,
                true);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to render help output", ex);
        }

    }

    /**
     * @return the writeStdErr
     */
    public boolean isWriteStdErr() {
        return writeStdErr;
    }

    /**
     * @return the writeStdOut
     */
    public boolean isWriteStdOut() {
        return writeStdOut;
    }

    /**
     * @return the writeDatabase
     */
    public boolean isWriteDatabase() {
        return writeDatabase;
    }

    /**
     * @return the outFile
     */
    public String getOutFile() {
        return outFile;
    }

    /**
     * @return the inFileNames
     */
    public String[] getInFileNames() {
        return inFileNames;
    }

    /**
     * @return the writeNull
     */
    public boolean isWriteNull() {
        return writeNull;
    }

    /**
     * @return the verbose
     */
    public boolean isVerbose() {
        return verbose;
    }
}
