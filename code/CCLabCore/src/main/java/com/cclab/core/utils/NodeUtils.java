package com.cclab.core.utils;

import java.io.*;

/**
 * Created by ane on 10/22/14.
 */
public class NodeUtils {
    public static String join(Object[] parts, int skip, String delimiter) {
        StringBuilder ret = new StringBuilder();
        for (int i = skip; i < parts.length - 1; i++)
            ret.append(parts[i]).append(delimiter);
        ret.append(parts[parts.length - 1]);
        return ret.toString();
    }

    public static String join(Object[] parts, String delimiter) {
        return join(parts, 0, delimiter);
    }

    public static byte[] readDataFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            throw new IOException("Could not completely read file " + file.getName() + " as it is too long (" + length + " bytes, max supported " + Integer.MAX_VALUE + ")");
        }

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
    }

    public static void writeDataToFile(byte[] data, File file) throws IOException {
        BufferedOutputStream bos = null;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
        } finally {
            if (bos != null) {
                try {
                    //flush and close the BufferedOutputStream
                    bos.flush();
                    bos.close();
                } catch (Exception e) {
                }
            }
        }
    }

}
