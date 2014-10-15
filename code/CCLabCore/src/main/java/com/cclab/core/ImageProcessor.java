package com.cclab.core;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static org.imgscalr.Scalr.*;

/**
 * Created by ane on 10/15/14.
 */
public class ImageProcessor {
    public static void createThumbnail(String inFilePath, String outFilePath, String type) throws FileNotFoundException {
        createThumbnail(inFilePath, outFilePath, type);

    }

    public static void createThumbnail(String inFilePath, String outFilePath) throws IOException {
        FileInputStream in = new FileInputStream(inFilePath);
        FileOutputStream out = new FileOutputStream(outFilePath);
        createThumbnail(in, out, "JPEG");

    }

    public static void createThumbnail(InputStream input, OutputStream output) throws IOException {
        createThumbnail(input, output, "JPEG");
    }

    public static void createThumbnail(InputStream input, OutputStream output, String type) throws IOException {
        writeImage(createThumbnail(readImage(input)), type, output);
    }

    private static BufferedImage createThumbnail(BufferedImage img) {
        // Create quickly, then smooth and brighten it.
        img = resize(img, Scalr.Method.SPEED, 125, OP_ANTIALIAS, OP_BRIGHTER);

        // Let's add a little border before we return result.
        return pad(img, 4);
    }

    private static BufferedImage readImage(InputStream inputStream) throws IOException {
        return ImageIO.read(inputStream);
    }

    public static BufferedImage readImage(String filePath) throws IOException {
        return readImage(new File(filePath));
    }

    public static BufferedImage readImage(File imageFile) throws IOException {
        return readImage(new FileInputStream(imageFile));
    }

    public static ByteArrayOutputStream writeImage(BufferedImage img, String type) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, type, out);
        return out;
    }

    public static void writeImage(BufferedImage img, String type, File filePath) throws IOException {
        writeImage(img, type, new FileOutputStream(filePath));
    }

    public static void writeImage(BufferedImage img, String type, OutputStream out) throws IOException {
        ImageIO.write(img, type, out);
    }
}
