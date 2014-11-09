package com.cclab.core.data;

import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.File;

/**
 * Abstracts the file-based storage scheme.
 * <p/>
 * By default, it initializes in the current running directory and expects to
 * find its input data in the input/ directory and writes its output to the
 * output/ directory. If these are not found, they are created. File names are
 * used as identifiers.
 * <p/>
 * Created on 10/31/14 for CCLabCore.
 *
 * @author an3m0na
 */

public class Database {
    private static Database ourInstance = null;
    private static final String rootInDir = "/input";
    private static final String rootOutDir = "/output";
    private static final String rootTmpDir = "/tmp";
    private File inputDir = null;
    private File outputDir = null;
    private File tmpDir = null;
    private String[] lastPolled = null;
    private int nextUp = -1;

    public static Database getInstance() {
        if (ourInstance == null)
            ourInstance = new Database();
        return ourInstance;
    }

    public static boolean isBackup = false;

    private Database() {

        String currentDir = System.getProperty("user.dir");
        NodeLogger.get().info("Database initializing under " + currentDir + (isBackup ? " as backup" : ""));

        String inputDirPath = currentDir + rootInDir + (isBackup ? "_backup" : "");
        String outputDirPath = currentDir + rootOutDir + (isBackup ? "_backup" : "");
        String tmpDirPath = currentDir + rootTmpDir + (isBackup ? "_backup" : "");
        inputDir = new File(inputDirPath);
        outputDir = new File(outputDirPath);
        tmpDir = new File(tmpDirPath);
        if (!inputDir.exists()) {
            NodeLogger.get().warn("Input directory not found. Creating it...");
            if (!inputDir.mkdirs()) {
                NodeLogger.get().error("Error creating input directory at " + inputDirPath);
                return;
            }
        }
        if (!tmpDir.exists()) {
            NodeLogger.get().info("Temporary directory not found. Creating it...");
            if (!tmpDir.mkdirs()) {
                NodeLogger.get().error("Error creating output directory at " + outputDirPath);
            }
        }
        if (!outputDir.exists()) {
            NodeLogger.get().info("Output directory not found. Creating it...");
            if (!outputDir.mkdirs()) {
                NodeLogger.get().error("Error creating output directory at " + outputDirPath);
            }
        }
        pollNew();
        if (nextUp < 0)
            NodeLogger.get().warn("No initial input in database");
    }

    /**
     * Checks for records that have been added/modified since the last poll.
     * To be called only if the record processing queue is external.
     *
     * @return the list of record ids
     */
    public String[] pollNew() {
        lastPolled = inputDir.list();
        for (String name : lastPolled) {
            File oldFile = new File(inputDir.getAbsolutePath() + "/" + name);
            File newFile = new File(tmpDir.getAbsolutePath() + "/" + name);
            boolean result = oldFile.renameTo(newFile);
            if (!result)
                NodeLogger.get().error("Could not move file " + name + " to " + tmpDir.getAbsolutePath());
        }
        if (lastPolled.length > 0)
            nextUp = 0;
        else
            nextUp = -1;
        return lastPolled;
    }

    /**
     * Gets the next record to be processed
     *
     * @return the record's id
     */
    public synchronized String getNextRecordId() {
        String id = peekNextRecordId();
        if (id != null)
            nextUp++;
        return id;
    }

    /**
     * Shows the next record to be processed without removing it
     *
     * @return the record's id
     */
    public String peekNextRecordId() {
        if (nextUp < 0 || nextUp >= lastPolled.length)
            pollNew();
        if (nextUp < 0) {
            return null;
        }
        return lastPolled[nextUp];
    }


    /**
     * Transforms a record id to the associated filename in the input directory.
     *
     * @param inputId the record's id
     * @return the input record's filename
     */
    private String getInputPathFromId(String inputId) {
        return inputDir.getAbsolutePath() + "/" + inputId;
    }

    /**
     * Transforms a record id to the associated filename in the temporary directory.
     *
     * @param inputId the record's id
     * @return the input record's filename
     */
    private String getTempPathFromId(String inputId) {
        return tmpDir.getAbsolutePath() + "/" + inputId;
    }


    /**
     * Constructs an output filename based on the original input record's id.
     *
     * @param originalId the original input record's id
     * @return the output record's filename
     */
    private String getOutputPathFromOriginalId(String originalId) {
        return outputDir.getAbsolutePath() + "/processed_" + System.currentTimeMillis() + "_" + originalId;
    }

    /**
     * Extracts the original input record's id from an output filename.
     *
     * @param filename the output record's filename
     * @return the original input record's id
     */
    private String getIdFromOutputName(String filename) {
        filename = filename.replace("processed_", "");
        return filename.substring(filename.indexOf("_") + 1);
    }

    /**
     * Gets a record from its id.
     *
     * @param inputId the record's id
     * @return the record as a byte array
     */
    public byte[] getRecord(String inputId) {
        String inputName = getTempPathFromId(inputId);
        try {
            return NodeUtils.readDataFromFile(new File(inputName));
        } catch (Exception e) {
            NodeLogger.get().error("Error reading input " + inputName, e);
        }
        return null;
    }

    /**
     * Writes a record to storage
     *
     * @param output the new record as a byte array
     */
    public void storeRecord(byte[] output, String originalId) {
        String outputName = getOutputPathFromOriginalId(originalId);
        try {
            NodeUtils.writeDataToFile(output, new File(outputName));
        } catch (Exception e) {
            NodeLogger.get().error("Error storing output " + outputName, e);
        }
    }

    /**
     * Writes a record to storage as an input record
     *
     * @param output the new record as a byte array
     */
    public void storeInputRecord(byte[] output, String inputId) {
        String outputName = getInputPathFromId(inputId);
        try {
            NodeUtils.writeDataToFile(output, new File(outputName));
        } catch (Exception e) {
            NodeLogger.get().error("Error storing output " + outputName, e);
        }
    }

    public String[] getProcessedRecords() {
        String[] stored = outputDir.list();

        for (int i = 0; i < stored.length; i++)
            stored[i] = getIdFromOutputName(stored[i]);
        return stored;
    }

    public String[] getTemporaryRecords() {
        return tmpDir.list();
    }

    public void removeInputRecord(String inputId) {
        String inputName = getInputPathFromId(inputId);
        File file = new File(inputName);
        boolean done = file.delete();
        if (!done) {
            NodeLogger.get().info("No input to remove for " + inputName);
        }
    }
}
