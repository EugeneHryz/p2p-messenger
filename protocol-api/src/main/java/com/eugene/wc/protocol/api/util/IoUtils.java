package com.eugene.wc.protocol.api.util;

import java.io.File;

public class IoUtils {

    public static boolean isDirectoryEmpty(File dir) {
        boolean empty = true;
        String[] files = dir.list();
        if (files != null && files.length > 0) {
            empty = false;
        }
        return empty;
    }
}
