package com.eugene.wc.protocol.api.util;

import static com.eugene.wc.protocol.api.util.LogUtils.logException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IoUtils {

    public static boolean isDirectoryEmpty(File dir) {
        boolean empty = true;
        String[] files = dir.list();
        if (files != null && files.length > 0) {
            empty = false;
        }
        return empty;
    }

    public static void tryToClose(Closeable c, Logger logger, Level level) {
        try {
            if (c != null) c.close();
        } catch (IOException e) {
            logException(logger, level, e);
        }
    }

    public static void tryToClose(Socket s, Logger logger,
                                  Level level) {
        try {
            if (s != null) s.close();
        } catch (IOException e) {
            logException(logger, level, e);
        }
    }

    public static void tryToClose(ServerSocket ss, Logger logger, Level level) {
        try {
            if (ss != null) ss.close();
        } catch (IOException e) {
            logException(logger, level, e);
        }
    }

}
