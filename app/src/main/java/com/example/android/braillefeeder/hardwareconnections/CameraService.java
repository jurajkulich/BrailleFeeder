package com.example.android.braillefeeder.hardwareconnections;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import androidx.annotation.NonNull;

import android.os.HandlerThread;
import android.util.Log;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;


public class CameraService {

    // Size of taken photo
    private static final int IMAGE_WIDTH = 1280;
    private static final int IMAGE_HEIGHT = 720;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private ImageReader mImageReader;

    private CameraService() {
    }

    private static class InstanceHolder {
        private static CameraService sCameraService = new CameraService();
    }

    public static CameraService getInstance() {
        return InstanceHolder.sCameraService;
    }

    public void initializeCamera(Context context,
                                 ImageReader.OnImageAvailableListener onImageAvailableListener) {

        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if( camIds.length < 1) {
            Log.e("CameraService", "Camera not available.");
            return;
        }

        startBackgroundThread();

        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

        try {
            cameraManager.openCamera(camIds[0], mStateCallback, backgroundHandler);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
//            mCameraDevice = null;
        }
    };

    public void takePicture() {
        Log.d("CameraService", "takePicture()");
        if( mCameraDevice == null) {
            Log.d("CameraService", "Cannot take picture. Camera device is null.");
            return;
        }
        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if( mCameraDevice == null) {
                                Log.e("mStateCallback", " mStateCallbackCaptureSession configured");
                                return;
                            }
                            Log.d("CameraService", "imageCapture()");
                            mCameraCaptureSession = cameraCaptureSession;
                            imageCapture();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e("mStateCallback", "Configure failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void imageCapture() {
        Log.d("CameraService", "imageCapture()");
        try {
            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(builder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e("imagecapture()", "KOKOTKO");
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                    super.onCaptureProgressed(session, request, partialResult);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    if( session != null) {
//                        session.close();
//                        mStateCallbackCaptureSession = null;
                    }
                }
    };

    public void shutdown() {
        Log.d("CameraService", "shutdown()");
        if( mCameraDevice != null) {
            mCameraDevice.close();
        }
        if( mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
        }
        stopBackgroundThread();
    }
}
