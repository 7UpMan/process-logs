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
 * Object-oriented refactoring of ProcessLog.
 * 
 * Key improvements:
 * - No static state - all dependencies are instance fields
 * - Each instance is independent and can be tested
 * - Clear dependencies visible in constructor
 * - Thread-safe (each thread gets its own instance)
 * - Can be mocked for testing
 * 
 * @author mat
 */
public class ProcessLog {
    
    private static final Logger logger = Logger.getLogger(ProcessLog.class.getName());
    
    // Instance fields instead of static fields
    private final CliOptions options;
    private final ConfigData config;
    private JpaAccess jpaAccess;      // Created when needed
    private BufferedWriter outFile;    // Created when needed
    
    /**
     * Constructor that takes all dependencies.
     * This makes it clear what this class needs to work.
     * 
     * @param options Command-line options
     * @param config Configuration data (filtering rules)
     */
    public ProcessLog(CliOptions options, ConfigData config) {
        if (options == null) {
            throw new IllegalArgumentException("CliOptions cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("ConfigData cannot be null");
        }
        
        this.options = options;
        this.config = config;
    }
    
    /**
     * Main entry point - creates an instance and runs it.
     * This is the ONLY static method we need!
     */
    public static void main(String[] args) {
        // Parse command-line options
        CliOptions options = new CliOptions(args);
        
        // Load configuration
        ConfigRepository configRepo = new ConfigRepository();
        ConfigData config;
        
        try {
            config = configRepo.load(options.isVerbose());
            logger.info("Configuration loaded: " + config.toString());
        } catch (ConfigRepository.ConfigurationException ex) {
            System.err.println("FATAL: Unable to load configuration from database");
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
            return;
        }
        
        // Create an instance and run it
        ProcessLog processor = new ProcessLog(options, config);
        processor.run();
    }
    
    /**
     * Main processing method - instance method, not static!
     * This is where the actual work happens.
     */
    public void run() {
        try {
            openOutput();
            
            for (String inFile : options.getInFileNames()) {
                processFile(inFile);
            }
            
        } finally {
            // Always clean up, even if there's an error
            closeOutput();
        }
    }
    
    /**
     * Process a single file.
     * Note: This is now an instance method with no parameters for config!
     * It uses the instance fields instead.
     */
    private void processFile(String inFile) {
        System.out.print("Processing file: " + inFile + " ");
        
        String lowDate = null;
        String hiDate = null;
        int inLineCounter = 0;
        int outLineCounter = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Use instance field 'config' instead of parameter
                RowStringStorage rowStringStorage = new RowStringStorage(config);
                inLineCounter++;
                
                process1Line(rowStringStorage, line);
                
                // Track date range
                if (lowDate == null || rowStringStorage.getDate().compareTo(lowDate) < 0) {
                    lowDate = rowStringStorage.getDate();
                }
                if (hiDate == null || rowStringStorage.getDate().compareTo(hiDate) > 0) {
                    hiDate = rowStringStorage.getDate();
                }
                
                outLineCounter += writeRow(rowStringStorage);
                
                // Display progress
                if (inLineCounter % ToolsAndConstants.PROGRESS_FREQUENCY == 0) {
                    System.out.print("*");
                }
            }
        } catch (IOException ex) {
            System.err.format("%nException occurred trying to read '%s'.%n", inFile);
            System.err.println("Error message:" + ex.getMessage());
            return;
        }
        
        // Commit if writing to database
        if (options.isWriteDatabase() && jpaAccess != null) {
            jpaAccess.commit();
        }
        
        // Print summary
        System.out.printf("%nThere were %,d records read from file.%n", inLineCounter);
        System.out.printf("There were %,d records written to output.%n", outLineCounter);
        if (options.isVerbose()) {
            System.out.printf("Date range from %s to %s.%n", lowDate, hiDate);
        }
        System.out.println();
    }
    
    /**
     * Write a row to the output.
     * Uses instance fields instead of static fields.
     */
    private int writeRow(RowStringStorage rowStringStorage) {
        if (rowStringStorage.isDeleteRow()) {
            return 0;
        }
        
        try {
            if (options.isWriteDatabase()) {
                return jpaAccess.insertRow(rowStringStorage);
            } else if (options.isWriteNull()) {
                return 0;
            } else {
                outFile.write(rowStringStorage.toString());
                outFile.newLine();
                return 1;
            }
        } catch (IOException | RuntimeException ex) {
            logger.log(Level.SEVERE, "Error writing row", ex);
            closeOutput();
            throw new RuntimeException("Failed to write row", ex);
        }
    }
    
    /**
     * Open the output destination based on options.
     * Uses instance fields instead of static fields.
     */
    private void openOutput() {
        // Open database if needed
        if (options.isWriteDatabase()) {
            jpaAccess = new JpaAccess();
        }
        
        // Open file output if needed
        try {
            if (options.isWriteStdErr()) {
                outFile = new BufferedWriter(new OutputStreamWriter(System.err));
            } else if (options.isWriteStdOut()) {
                outFile = new BufferedWriter(new OutputStreamWriter(System.out));
            } else if (options.getOutFile() != null) {
                File file = new File(options.getOutFile());
                if (!file.exists()) {
                    file.createNewFile();
                }
                outFile = new BufferedWriter(new FileWriter(file));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to open output", ex);
            throw new RuntimeException("Failed to open output", ex);
        }
    }
    
    /**
     * Close all outputs and clean up resources.
     * Uses instance fields instead of static fields.
     */
    private void closeOutput() {
        if (jpaAccess != null) {
            try {
                jpaAccess.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error closing database connection", ex);
            }
        }
        
        if (outFile != null) {
            try {
                outFile.flush();
                outFile.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error closing output file", ex);
            }
        }
    }
    
    /**
     * Parse one line from the log file.
     * Same implementation as before, but now an instance method.
     */
    private void process1Line(RowStringStorage rowStringStorage, String line) {
        boolean inBracket = false;
        boolean inQuote = false;
        int fieldNumber = 0;
        boolean escaped = false;
        
        StringBuilder field = new StringBuilder();
        
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
        
        fieldNumber++;
        process1Field(rowStringStorage, field.toString(), fieldNumber);
    }
    
    /**
     * Process one field from the log line.
     * Same implementation as before, but now an instance method.
     */
    private void process1Field(RowStringStorage rowStringStorage, String param, int fieldNumber) {
        // Field 4 is a date
        if (fieldNumber == 4) {
            String[] monthNames = {"jan", "feb", "mar", "apr", "may", "jun", 
                                  "jul", "aug", "sep", "oct", "nov", "dec"};
            String dom = param.substring(0, 2);
            String monName = param.substring(3, 6);
            String year = param.substring(7, 11);
            String time = param.substring(12, 20);
            String monNumber = "00";
            for (int i = 0; i < monthNames.length; i++) {
                if (monName.equalsIgnoreCase(monthNames[i])) {
                    monNumber = ((100 + i + 1) + "").substring(1);
                    break;
                }
            }
            param = year + "-" + monNumber + "-" + dom + " " + time;
        }
        
        // Field 5 is the URL which we want in 4 parts
        if (fieldNumber == 5) {
            int spcLoc = param.indexOf(' ');
            String firstStr = param.substring(0, spcLoc);
            String secondStr = param.substring(spcLoc + 1);
            String fourthStr = "";
            spcLoc = secondStr.indexOf(' ');
            if (spcLoc > 0) {
                fourthStr = secondStr.substring(spcLoc + 1);
                secondStr = secondStr.substring(0, spcLoc);
            }
            String thirdStr = "";
            spcLoc = secondStr.indexOf('?');
            if (spcLoc > 0) {
                thirdStr = secondStr.substring(spcLoc + 1);
                secondStr = secondStr.substring(0, spcLoc);
            }
            
            rowStringStorage.pushCol(firstStr);
            rowStringStorage.pushCol(secondStr);
            rowStringStorage.pushCol(thirdStr);
            rowStringStorage.pushCol(fourthStr);
        } else {
            rowStringStorage.pushCol(param);
        }
    }
}
