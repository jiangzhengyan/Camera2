package com.ca.camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 本类的主要功能是 :   开启摄像头
 * <p>
 * 1,初始化,{@link CameraView#initCamera()}
 * 2,权限,{@link CameraView#onRequestPermissionsResult(int, String[], int[])}
 * 3,生命周期,{@link CameraView#onPause()}
 *
 * @author koko  2023/10/12 11:38
 */
public class CameraView extends SurfaceView {

    private static final String TAG = "CameraView";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 22;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewBuilder;

    public CameraView(Context context) {
        super(context);
        initView();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {

    }

    public void initCamera() {
        changeCameraOri(getResources().getConfiguration().orientation);

        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // 初始化相机
                // 在需要使用相机的地方进行权限检查
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // 如果权限未被授予，则请求相机权限
                    ActivityCompat.requestPermissions((Activity) getContext(),
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    // 如果权限已被授予，则直接打开相机
                    openCamera();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // 更新相机预览尺寸
                updatePreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 释放相机资源
                closeCamera();
            }
        });
    }

    // 处理权限请求的回调
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 相机权限已被授予，打开相机
                openCamera();
            } else {
                // 相机权限被拒绝，显示一个提示消息或执行其他操作
            }
        }
    }


    protected void onPause() {
        closeCamera();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changeCameraOri(newConfig.orientation);
    }

    private void changeCameraOri(int orientation) {
        float ratioScreen = 0.28f;
        float ratioCamera = 16 / 9f;
        //以窄边为标准
        //竖屏
        int screenWidthPortrait = (int) (PhoneHelper.getScreenWidthReal(getContext()) * ratioScreen);
        int screenHeightPortrait = (int) (screenWidthPortrait * ratioCamera);
        //横屏
        int screenHeightLandScape = (int) (PhoneHelper.getScreenHeightReal(getContext()) * ratioScreen);
        int screenWidthLandScape = (int) (screenHeightLandScape * ratioCamera);

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                Log.i(TAG, "onConfigurationChanged: " + "竖屏");
                // 竖屏1080-1920
                layoutParams.width = screenWidthPortrait;
                layoutParams.height = screenHeightPortrait;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                Log.i(TAG, "onConfigurationChanged: " + "横屏");
                // 横屏1920-1080
                layoutParams.width = screenWidthLandScape;
                layoutParams.height = screenHeightLandScape;
                break;
        }
    }


    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        getHolder().setFixedSize(9999, 9999);
        try {
            String cameraId = getFrontCameraId(cameraManager);

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    mCameraDevice = camera;
                    Log.e(TAG, "onOpened: ");
                    createCaptureSession();

                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void updatePreview() {
        if (mCameraDevice == null) {
            return;
        }
        try {
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String getFrontCameraId(CameraManager cameraManager) throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }
        return cameraIds[0];
    }

    private void createCaptureSession() {
        try {
            SurfaceHolder surfaceHolder = getHolder();
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(surfaceHolder.getSurface());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surfaceHolder.getSurface());

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mCaptureSession = session;
                    Log.e(TAG, "onConfigured: ");
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // 配置会话失败
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
