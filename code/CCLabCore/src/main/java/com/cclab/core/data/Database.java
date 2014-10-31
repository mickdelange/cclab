package com.cclab.core.data;

import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.File;

public class Database {
    private static Database ourInstance = new Database();
    private static final String rootInDir = "/input";
    private static final String rootOutDir = "/output";
    private File inputDir = null;
    private File outputDir = null;

    public static Database getInstance() {
        return ourInstance;
    }

    private Database() {
        String currentDir = System.getProperty("user.dir");
        NodeLogger.get().info("Database initializing under " + currentDir);

        String inputDirPath = currentDir + rootInDir;
        String outputDirPath = currentDir + rootOutDir;
        this.inputDir = new File(inputDirPath);
        this.outputDir = new File(outputDirPath);
        if (!inputDir.exists()) {
            NodeLogger.get().warn("Input directory not found. Creating it...");
            if (!inputDir.mkdirs()) {
                NodeLogger.get().error("Error creating input directory at " + inputDirPath);
                return;
            }
        }
        if (!outputDir.exists()) {
            NodeLogger.get().info("Output directory not found. Creating it...");
            if (!outputDir.mkdirs()) {
                NodeLogger.get().error("Error creating output directory at " + outputDirPath);
            }
        }
    }

    private String getInputPathFromId(String inputId) {
        return inputDir.getAbsolutePath() + "/" + inputId;
    }

    private String getOutputPathFromOriginalId(String originalId) {
        return outputDir.getAbsolutePath() + "/processed_" + originalId;
    }

    public byte[] getInput(String inputId) {
        String inputName = getInputPathFromId(inputId);
        try {
            return NodeUtils.readDataFromFile(new File(inputName));
        } catch (Exception e) {
            NodeLogger.get().error("Error reading input " + inputName, e);
        }
        return null;
    }

    public void storeOutput(byte[] output, String originalId) {
        String outputName = getOutputPathFromOriginalId(originalId);
        try {
            NodeUtils.writeDataToFile(output, new File(outputName));
        } catch (Exception e) {
            NodeLogger.get().error("Error storing output " + outputName, e);
        }
    }
}
