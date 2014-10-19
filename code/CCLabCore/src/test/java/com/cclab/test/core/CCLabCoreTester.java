package com.cclab.test.core;

import com.cclab.core.processing.ImageProcessor;

import java.io.IOException;

/**
 * Created by ane on 10/15/14.
 */
public class CCLabCoreTester {
    public static void main (String[] args) {
        try {
            ImageProcessor.process("/Users/ane/Downloads/strawberry.jpg", "/Users/ane/Downloads/strawberry_small.jpg", "blur");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
