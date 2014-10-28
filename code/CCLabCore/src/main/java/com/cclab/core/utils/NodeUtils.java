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

    public static void writeDataToFile(byte[] data, String filename) throws IOException {
        File out = new File(filename);
        BufferedOutputStream bos = null;
        try {
            FileOutputStream fos = new FileOutputStream(out);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
        }finally {
            if(bos != null) {
                try  {
                    //flush and close the BufferedOutputStream
                    bos.flush();
                    bos.close();
                } catch(Exception e){}
            }
        }
    }
}
