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
    private static Database ourInstance = new Database();
    private static final String rootInDir = "/input";
    private static final String rootOutDir = "/output";
    private static final String rootTmpDir = "/tmp";
    private File inputDir = null;
    private File outputDir = null;
    private File tmpDir = null;
    private String[] lastPolled = null;
    private int nextUp = -1;

    public static Database getInstance() {
        return ourInstance;
    }

    private Database() {
        String currentDir = System.getProperty("user.dir");
        NodeLogger.get().info("Database initializing under " + currentDir);

        String inputDirPath = currentDir + rootInDir;
        String outputDirPath = currentDir + rootOutDir;
        String tmpDirPath = currentDir + rootTmpDir;
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
    public String getNextRecordId() {
        if (nextUp < 0 || nextUp >= lastPolled.length)
            pollNew();
        if (nextUp < 0) {
            return null;
        }
        return lastPolled[nextUp++];
    }

    /**
     * Transforms an input record id to the associated filename.
     *
     * @param inputId the record's id
     * @return the input record's filename
     */
    private String getInputPathFromId(String inputId) {
        return tmpDir.getAbsolutePath() + "/" + inputId;
    }

    /**
     * Constructs an output filename based on the original input record's id.
     *
     * @param originalId the original input record's id
     * @return the output record's filename
     */
    private String getOutputPathFromOriginalId(String originalId) {
        return outputDir.getAbsolutePath() + "/processed_" + originalId;
    }

    /**
     * Gets a record from its id.
     *
     * @param inputId the record's id
     * @return the record as a byte array
     */
    public byte[] getRecord(String inputId) {
        String inputName = getInputPathFromId(inputId);
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
}
