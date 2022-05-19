package com.eugene.wc.qrcode;

import static com.google.zxing.DecodeHintType.CHARACTER_SET;
import static java.util.Collections.singletonMap;

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;

import com.eugene.wc.protocol.api.io.IoExecutor;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;

public class QrCodeDecoder implements ImageConsumer {

    private static final Logger logger = Logger.getLogger(QrCodeDecoder.class.getName());

    private final Reader reader;

    private int sensorOrientation;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private Callback callback;

    public QrCodeDecoder(Callback callback) {
        this.callback = callback;
        reader = new QRCodeReader();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (started.get()) {
            try (Image image = reader.acquireLatestImage()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                decode(data, image.getWidth(), image.getHeight(), sensorOrientation);
            }
        }
    }

    @Override
    public void start(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
        started.set(true);
        logger.info("QrCodeDecoder started");
    }

    private void decode(byte[] data, int width, int height, int orientation) {
        BinaryBitmap bitmap = binarize(data, width, height, orientation);
        Result result;
        try {
            result = reader.decode(bitmap, singletonMap(CHARACTER_SET, "ISO8859_1"));

            callback.onQrCodeDecoded(result.getText());
            logger.info("Decoded result: " + result.getText());
        } catch (ReaderException e) {
            // unable to decode
        } finally {
            reader.reset();
        }
    }

    private static BinaryBitmap binarize(byte[] data, int width, int height,
                                         int orientation) {

        int crop = Math.min(width, height);
        int left = orientation >= 180 ? width - crop : 0;
        int top = orientation >= 180 ? height - crop : 0;
        LuminanceSource src = new PlanarYUVLuminanceSource(data, width, height,
                left, top, crop, crop, false);
        return new BinaryBitmap(new HybridBinarizer(src));
    }

    public interface Callback {

        void onQrCodeDecoded(String decodedQr);
    }
}
