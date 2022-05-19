package com.eugene.wc.qrcode;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrCodeUtils {

    public static Bitmap createQrCode(DisplayMetrics dm, String data) throws WriterException {
        int edgeSize = Math.min(dm.heightPixels, dm.widthPixels);

        BitMatrix bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, edgeSize, edgeSize);
        return convertToBitmap(bitMatrix);
    }

    private static Bitmap convertToBitmap(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();

        int[] pixels = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[y * width + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        image.setPixels(pixels, 0, width, 0, 0, width, height);
        return image;
    }
}
