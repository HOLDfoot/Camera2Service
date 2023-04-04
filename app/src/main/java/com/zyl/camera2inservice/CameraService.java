package com.zyl.camera2inservice;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.Arrays;


public class CameraService extends Service {

    private static final String TAG = "CameraService";
    private CameraWindow cameraWindow;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams Params;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        showWindow();
        //camera2Controller = new Camera2Controller(cameraWindow.surfaceView, this);

        openCamera();
        createImageReader();
    }

    private CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            // 相机已经打开，可以开始预览
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // 相机被断开连接，需要释放资源
            releaseCamera();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // 发生错误，需要释放资源
            releaseCamera();
        }
    };

    private CameraCaptureSession.StateCallback mCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mCaptureSession = session;
            try {
                // 设置捕获请求
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mCaptureRequestBuilder.addTarget(mImageReader.getSurface());

                // 开始捕获
                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            // 配置会话失败，需要释放资源
            releaseCamera();
        }
    };

    CameraDevice mCameraDevice;
    CaptureRequest.Builder mCaptureRequestBuilder;
    ImageReader mImageReader;
    CameraCaptureSession mCaptureSession;
    Size mPreviewSize;
    String mCameraId;
    private void createImageReader() {
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                // 获取最新的帧
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    // 处理帧
                    processImage(image);

                    // 释放帧
                    image.close();
                }
            }
        }, null);
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取可用的相机列表
            String[] cameraIds = cameraManager.getCameraIdList();
            CameraCharacteristics characteristics = null;
            for (String cameraId : cameraIds) {
                characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    // 找到后置相机
                    mCameraId = cameraId;
                    break;
                }
            }
            if (characteristics == null) {
                Log.e(TAG, "openCamera can not find");
                return;
            }

            // 获取预览尺寸
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // 打开相机
            cameraManager.openCamera(mCameraId, mCameraDeviceCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        try {
            // 创建捕获会话
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), mCaptureSessionCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void releaseCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void processImage(Image image) {
        // 处理相机帧
        Log.e(TAG, "processImage");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        if (cameraWindow != null && cameraWindow.isAttachedToWindow()) {
            mWindowManager.removeView(cameraWindow);
        }
    }

    private void showWindow() {
        //创建MyWindow的实例
        cameraWindow = new CameraWindow(getApplicationContext());

        //窗口管理者
        mWindowManager = (WindowManager) getSystemService(Service.WINDOW_SERVICE);
        //窗口布局参数
        Params = new WindowManager.LayoutParams();
        //布局坐标,以屏幕左上角为(0,0)
        Params.x = 0;
        Params.y = 0;

        Params.type = WindowManager.LayoutParams.TYPE_PHONE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            Params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //布局flags
        Params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; // 不能抢占聚焦点
        Params.flags = Params.flags | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        Params.flags = Params.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS; // 排版不受限制
        Params.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        //布局的gravity
        Params.gravity = Gravity.LEFT | Gravity.TOP;

        //布局的宽和高
        Params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        Params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        cameraWindow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                if (cameraWindow.isAttachedToWindow()) {
                    mWindowManager.removeView(cameraWindow);
                }
                stopService(new Intent(CameraService.this, CameraService.class));
            }
        });
        if (!cameraWindow.isAttachedToWindow()) {
            mWindowManager.addView(cameraWindow, Params);
        }
    }
}
