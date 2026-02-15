package com.s4apps.processlog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mat
 */
public class ProcessLog {

    static JpaAccess msa = null;
    static BufferedWriter outFile = null;

    /**
     * @param args the command line arguments
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     */
    public static void main(String[] args) {

        // Process the commanfd line options
        CliOptions clio = new CliOptions(args);
        
        // Get the config data
        ConfigData cd = new ConfigData();

        // Get the destination opened up
        openOutput(clio);

        // Loop through all of the input files
        for (String inFile : clio.getInFileNames()) {
            processFile(inFile, cd, clio);
        }

        // Close everything down
        closeOutput();

    }

    private static void processFile(String inFile, ConfigData cd, CliOptions clio) {
        // Display the input file that we are processing
        System.out.print("Processing file: " + inFile + " ");

        // Capture date range
        String lowDate = null;
        String hiDate = null;

        int inLineCounter = 0;
        int outLineCounter = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                RowStringStorage rowStringStorage = new RowStringStorage(cd);
                inLineCounter++;

                // Process 1 line into a RowStringStorage
                process1Line(rowStringStorage, line);

                // Keep track of the date range if we are in verbose mode
                if (lowDate == null || rowStringStorage.getDate().compareTo(lowDate) < 0) {
                    lowDate = rowStringStorage.getDate();
                }
                if (hiDate == null || rowStringStorage.getDate().compareTo(hiDate) > 0) {
                    hiDate = rowStringStorage.getDate();
                }

                // Store it
                outLineCounter += writeRow(rowStringStorage, clio);

                // Display Progress
                if (inLineCounter % 1000 == 0) {
                    System.out.print("*");
                }
            }
        } catch (IOException ex) {
            System.err.format("%nException occurred trying to read '%s'.%n", inFile);
            System.err.println("Error message:" + ex.getMessage());
            return;
        }

        // Commit everything form a single inputfile to the database.
        if (clio.isWriteDatabase()) {
            msa.commit();
        }

        System.out.printf("%nThere were %,d records read from file.%n", inLineCounter);
        System.out.printf("There were %,d records writen to output.%n", outLineCounter);
        if (clio.isVerbose()) {
            System.out.printf("Date range from %s to %s.%n", lowDate, hiDate);
        }
        System.out.println();
    }

    /**
     * Write a row to the appropriate place returning the number of rows
     * written. If the destination is a file then the row is always written. If
     * a database then only INSERTs count.
     *
    * @param rowStringStorage
     * @param clio
     * @return
     */
    private static int writeRow(RowStringStorage rowStringStorage, CliOptions clio) {
        int retVal = 0;

        // If the row is to be deleted don't write it to the output
        if (rowStringStorage.isDeleteRow()) {
            return retVal;
        }
        
        try {
            if (clio.isWriteDatabase()) {
                // Was it a database
                retVal = msa.insertRow(rowStringStorage);
            } else if (clio.isWriteNull()) {
                // Do nothing
            } else {
                // Must have been a file
                outFile.write(rowStringStorage.toString());
                outFile.newLine();

                // We always write a row to a file
                retVal = 1;
            }
        } catch (IOException | RuntimeException ex) {
            Logger.getLogger(ProcessLog.class.getName()).log(Level.SEVERE, null, ex);
            closeOutput();
            System.exit(1);
        }

        // Return the value
        return retVal;
    }

    /**
     * Open up the output by looking at the command line options.
     *
     * @param clio
     */
    private static void openOutput(CliOptions clio) {

        // Get access to the database if needed
        if (clio.isWriteDatabase()) {
            msa = new JpaAccess();
        }

        // Now process the different file options, only one can be picked, this 
        // is taken care of by the CliOptions class
        // Standard Err
        if (clio.isWriteStdErr()) {
            outFile = new BufferedWriter(new OutputStreamWriter(System.err));
        }

        // Standard Out
        if (clio.isWriteStdOut()) {
            outFile = new BufferedWriter(new OutputStreamWriter(System.out));
        }

        // Sepcified file
        if (clio.getOutFile() != null) {

            try {
                //Specify the file name and path here
                File file = new File(clio.getOutFile());

                // This logic will make sure that the file gets created if it is not present at the
                // specified location
                if (!file.exists()) {
                    boolean created = file.createNewFile();
                    if (!created) {
                        System.err.println("Unable to create file: " + file.getAbsolutePath());
                    }
                }

                outFile = new BufferedWriter(new FileWriter(file));
            } catch (IOException ex) {
                Logger.getLogger(ProcessLog.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Error message: " + ex.getMessage());
            }

        }
        
        // Write to null
        if (clio.isWriteNull()) {
            // Do Nothing
        }
    }

    /**
     * Close the output down. Flush everything and close the outputs.
     */
    private static void closeOutput() {
        if (msa != null) {
            msa.close();
        }

        if (outFile != null) {
            try {
                outFile.flush();
                outFile.close();
            } catch (IOException ex) {
                Logger.getLogger(ProcessLog.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Error message: " + ex.getMessage());
            }
        }
    }

    /**
     * Populate the RowStringStorage object by parsing 1 line
     *
     * @param rowStringStorage the passed in object that will be populated.
     * @param line The String to process
     * @return A CSV formatted line or Null. Null means that the line should be
     * skipped.
     */
    private static void process1Line(RowStringStorage rowStringStorage, String line) {
        boolean inBracket = false;
        boolean inQuote = false;
        int fieldNumber = 0;
        boolean escaped = false;

        StringBuilder field = new StringBuilder();

        // Loop through the line character at a time
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (escaped) {
                field.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == ' ' && !inBracket && !inQuote) {
                fieldNumber++;
                process1Field(rowStringStorage, field.toString(), fieldNumber);
                field = new StringBuilder();
            } else if (ch == '[') {
                inBracket = true;
            } else if (ch == ']') {
                inBracket = false;
            } else if (ch == '"') {
                inQuote = !inQuote;
            } else {
                field.append(ch);
            }
        }

        // Finished processing, so close last parameter.
        fieldNumber++;
        process1Field(rowStringStorage, field.toString(), fieldNumber);

    }

    /**
     * Processes a single filed
     *
     * @param param the text to be processed
     * @param fieldNumber the field number for additional processing. Field 4
     * gets converted into a date and field 5 converted into 2 fields (method
     * and URL), with filtering on some URLs
     * @return null if the line is to be skipped or a line formatted in CSV
     * format
     */
    private static void process1Field(RowStringStorage rowStringStorage, String param, int fieldNumber) {

        // Field 4 is a date
        if (fieldNumber == 4) {
            String[] monthNames = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
            String dom = param.substring(0, 2);
            String monName = param.substring(3, 6);
            String year = param.substring(7, 11);
            String time = param.substring(12, 20);
            String monNumber = "00";
            for (int i = 0; i < monthNames.length; i++) {
                if (monName.equalsIgnoreCase(monthNames[i])) {
                    // Format month number with leading 0
                    monNumber = ((100 + i + 1) + "").substring(1);
                    break;
                }
            }
            param = year + "-" + monNumber + "-" + dom + " " + time;
        }

        // Field 5 is the URL which we want in 4 parts
        if (fieldNumber == 5) {
            // Format if: METHOD URL HTTP_Version
            // Find the space
            int spcLoc = param.indexOf(' ');
            // Get the METHOD
            String firstStr = param.substring(0, spcLoc);
            // Get the URL with HTTP
            String secondStr = param.substring(spcLoc + 1);
            // Get the HTTP
            String fourthStr = "";
            spcLoc = secondStr.indexOf(' ');
            if (spcLoc > 0) {
                fourthStr = secondStr.substring(spcLoc + 1);
                secondStr = secondStr.substring(0, spcLoc);
            }
            // Now break the URL into URL and query string
            String thirdStr = "";
            spcLoc = secondStr.indexOf('?');
            if (spcLoc > 0) {
                thirdStr = secondStr.substring(spcLoc + 1);
                secondStr = secondStr.substring(0, spcLoc);
            }

            // Store the extra bits of information that we worked out
            rowStringStorage.pushCol(firstStr);
            rowStringStorage.pushCol(secondStr);
            rowStringStorage.pushCol(thirdStr);
            rowStringStorage.pushCol(fourthStr);
        } else {
            // Store the data
            rowStringStorage.pushCol(param);
        }
    }
}
