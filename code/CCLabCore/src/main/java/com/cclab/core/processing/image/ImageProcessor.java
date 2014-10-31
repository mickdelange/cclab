package com.cclab.core.processing.image;

import com.cclab.core.processing.ProcessController;
import com.cclab.core.processing.Processor;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.imgscalr.Scalr.*;

public class ImageProcessor extends Processor {
    final static String OUTPUT_TYPE = "JPEG";
    final static int MAX_BLUR_RADIUS = 200;
    final static int MAX_SIZE = 2000;
    final static String DEFAULT_OPERATIONS = "blur";
    private String operations = null;

    public ImageProcessor(String taskId, byte[] input, ProcessController controller) {
        super(taskId, input, controller);
    }

    public ImageProcessor(String taskId, byte[] input, String operations, ProcessController controller) {
        super(taskId, input, controller);
        this.operations = operations;
    }

    @Override
    public void process(InputStream input, OutputStream output) throws IOException {
        BufferedImage inImage = streamToImage(input);
        String operations = (this.operations == null) ? DEFAULT_OPERATIONS : this.operations;
        BufferedImage outImage = processImage(inImage, operations);
        imageToStream(outImage, output);
    }

    private static BufferedImage processImage(BufferedImage img, String operations) {
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

    private static BufferedImage streamToImage(InputStream inputStream) throws IOException {
        return ImageIO.read(inputStream);
    }

    private static void imageToStream(BufferedImage img, OutputStream out) throws IOException {
        ImageIO.write(img, OUTPUT_TYPE, out);
    }
}
