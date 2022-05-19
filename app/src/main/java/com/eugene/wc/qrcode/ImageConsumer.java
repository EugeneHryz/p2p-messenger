package com.eugene.wc.qrcode;

import android.media.ImageReader;

public interface ImageConsumer extends ImageReader.OnImageAvailableListener {

    void start(int sensorOrientation);
}
