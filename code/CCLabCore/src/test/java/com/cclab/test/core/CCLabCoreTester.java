package com.cclab.test.core;

import com.cclab.core.ImageProcessor;

import java.io.IOException;

/**
 * Created by ane on 10/15/14.
 */
public class CCLabCoreTester {
    //some comment to check VCS
    public static void main (String[] args) {
        try {
            ImageProcessor.createThumbnail("/Users/ane/Downloads/strawberry.jpg", "/Users/ane/Downloads/strawberry_small.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
