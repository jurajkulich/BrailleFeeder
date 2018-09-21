package com.example.android.braillefeeder;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.util.Collections;


public class    CameraService {

    private static final int IMAGE_WIDTH = 1280;
    private static final int IMAGE_HEIGHT = 960;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;

    private ImageReader mImageReader;

    private static class InstanceHolder {
        private static CameraService sCameraService = new CameraService();
    }

    public static CameraService getInstance() {
        return InstanceHolder.sCameraService;
    }

    public void initializeCamera(Context context, Handler handler,
                                 ImageReader.OnImageAvailableListener onImageAvailableListener) {

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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

        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(onImageAvailableListener, handler);

        try {
            cameraManager.openCamera(camIds[0], mStateCallback, handler);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
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
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mCameraDevice = null;
        }
    };

    public void takePicture() {
        if( mCameraDevice == null) {
            return;
        }
        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    mStateCallbackCaptureSession, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.StateCallback mStateCallbackCaptureSession = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            if( mCameraDevice == null) {
                Log.e("mStateCallback", "mCameraDevice is null");
                return;
            }
            mCameraCaptureSession = cameraCaptureSession;
            imageCapture();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

        }
    };

    private void imageCapture() {
        try {
            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCameraCaptureSession.capture(builder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
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
                        session.close();
//                        mStateCallbackCaptureSession = null;
                    }
                }
    };

    public void shutdown() {
        if( mCameraDevice != null) {
            mCameraDevice.close();
            mCameraCaptureSession.close();
        }


    }
}
