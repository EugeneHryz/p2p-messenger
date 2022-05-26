package com.eugene.wc.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.eugene.wc.qrcode.ImageConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraView.class.getName();

    private static final long MIN_OUTPUT_SIZE_AREA = 200000;

    private String cameraId;
    private CameraDevice cameraDevice;

    private SurfaceHolder surfaceHolder;

    private int ratioWidth;
    private int ratioHeight;

    private Size previewSize;

    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;

    private ImageReader imageReader;
    private ImageConsumer imageConsumer;

    private HandlerThread bgThread;
    private Handler bgHandler;

    private final CameraDevice.StateCallback cameraCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
            Log.d(TAG, "Camera successfully opened");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            Log.d(TAG, "Camera disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            Log.d(TAG, "Error while opening the camera error code: " + error);
        }
    };

    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getHolder().addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getHolder().removeCallback(this);
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            setUpCameraOutputs();
            if (cameraId == null) {
                throw new RuntimeException("No connected cameras available");
            }
            cameraManager.openCamera(cameraId, cameraCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error while trying to open the camera", e);
        }
    }

    private void closeCamera() {
        Log.d(TAG, "Closing camera device...");
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        surfaceHolder = holder;
        closeCamera();
        openCamera();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        holder.getSurface().release();
        imageReader.getSurface().release();

        Log.d(TAG, "Closing CaptureSession");
        captureSession.close();
        closeCamera();
        if (bgThread != null) {
            stopBgThread();
        }
    }

    public void setImageConsumer(ImageConsumer consumer) {
        imageConsumer = consumer;
    }

    private void startBgThread() {
        bgThread = new HandlerThread("CameraBackground");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
    }

    private void stopBgThread() {
        bgThread.quitSafely();
        try {
            bgThread.join();
            bgThread = null;
            bgHandler = null;
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while waiting for thread to finish", e);
        }
    }

    private Size chooseOptimalOutputSize(Size[] supportedSizes, Size largest) {
        double aspectRatioRef = (double) largest.getWidth() / largest.getHeight();

        List<Size> suitableSizes = new ArrayList<>();
        for (Size s : supportedSizes) {
            double ratio = (double) s.getWidth() / s.getHeight();

            if (Double.compare(aspectRatioRef, ratio) == 0 && !s.equals(largest)) {
                suitableSizes.add(s);
            }
        }
//        Collections.sort(suitableSizes, new CompareSizesByArea());

        int listSize = suitableSizes.size();
        Size chosenSize = listSize != 0 ? suitableSizes.get(0) : null;
        for (Size size : suitableSizes) {
            long area = CompareSizesByArea.calculateArea(size);

            if (area >= MIN_OUTPUT_SIZE_AREA) {
                chosenSize = size;
            }
        }
        return chosenSize;
    }

    private void setUpCameraOutputs() {
        Context context = getContext();
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = chars.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size[] outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                Log.d(TAG, Arrays.toString(outputSizes));
                Size largest = Collections.max(Arrays.asList(outputSizes),
                        new CompareSizesByArea());
                Log.d(TAG, "Largest: " + largest);
                previewSize = largest;

                int sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION);
                int orientation = getResources().getConfiguration().orientation;

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                this.cameraId = cameraId;

                Size outputSize = chooseOptimalOutputSize(outputSizes, largest);
                Log.d(TAG, "Chosen outputSize: " + outputSize);
                if (outputSize == null) {
                    Log.d(TAG, "OutputSize is null");
                    outputSize = largest;
                }
                imageReader = ImageReader.newInstance(outputSize.getWidth(), outputSize.getHeight(),
                        ImageFormat.YUV_420_888, 2);
                if (imageConsumer != null) {
                    if (bgThread == null) {
                        startBgThread();
                    }
                    imageReader.setOnImageAvailableListener(imageConsumer, bgHandler);
                    imageConsumer.start(sensorOrientation);
                }
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(calculateArea(lhs) - calculateArea(rhs));
        }

        public static long calculateArea(Size s) {
            return (long) s.getHeight() * s.getWidth();
        }
    }

    private void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }

    private void createCameraPreviewSession() {
        try {
            surfaceHolder.setFixedSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = surfaceHolder.getSurface();

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface irSurface = imageReader.getSurface();
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(irSurface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, irSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }
                            captureSession = cameraCaptureSession;
                            try {
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN);
                                captureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE,
                                        CaptureRequest.CONTROL_SCENE_MODE_BARCODE);

                                previewRequest = captureRequestBuilder.build();
                                if (cameraDevice != null) {
                                    captureSession.setRepeatingRequest(previewRequest, null,
                                            null);
                                }

                            } catch (CameraAccessException e) {
                                Log.e(TAG, e.toString());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "CameraCaptureSession config failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to create camera preview session" + e);
        }
    }
}
