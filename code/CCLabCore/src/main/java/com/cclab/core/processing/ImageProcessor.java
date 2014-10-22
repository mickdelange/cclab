package com.cclab.core.processing;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static org.imgscalr.Scalr.*;

/**
 * Created by ane on 10/15/14.
 */
public class ImageProcessor {
    final static String OUTPUT_TYPE = "JPEG";
    final static int MAX_BLUR_RADIUS = 200;
    final static int MAX_SIZE = 2000;

    public static void process(String inFilePath, String outFilePath, String operations) throws IOException {
        FileInputStream in = new FileInputStream(inFilePath);
        FileOutputStream out = new FileOutputStream(outFilePath);
        process(in, out, operations);
    }

    public static void process(InputStream input, OutputStream output, String operations) throws IOException {
        writeImage(process(readImage(input), operations), output);
    }

    private static BufferedImage process(BufferedImage img, String operations){
        String[] ops = operations.split(",");
        for (String op : ops) {
            if ("thumbnail".equals(op))
                img = createThumbnail(img);
            else if ("blur".equals(op))
                img = blurImage(img);
            else
                throw new RuntimeException("Unknown filter");
        }
        return img;
    }

    private static BufferedImage blurImage(BufferedImage img) {
        if (Math.max(img.getHeight(), img.getWidth()) > MAX_SIZE)
            img = resize(img, MAX_SIZE);
        long radius = Math.round(Math.sqrt(img.getHeight() * img.getWidth())) / 10;
        new GaussianFilter(Math.min(radius, MAX_BLUR_RADIUS)).filter(img, img);
        return img;
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

    private static BufferedImage readImage(String filePath) throws IOException {
        return readImage(new File(filePath));
    }

    private static BufferedImage readImage(File imageFile) throws IOException {
        return readImage(new FileInputStream(imageFile));
    }

    private static ByteArrayOutputStream writeImage(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeImage(img, out);
        return out;
    }

    private static void writeImage(BufferedImage img, File filePath) throws IOException {
        writeImage(img, new FileOutputStream(filePath));
    }

    private static void writeImage(BufferedImage img, OutputStream out) throws IOException {
        ImageIO.write(img, OUTPUT_TYPE, out);
    }
}
