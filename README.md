
# android-使用Camera2+SurfaceView打开摄像头(二)
   <br>
           
   使用Camera2打开相机,所有逻辑封装在CameraView自定义控件,使用方式直接调用即可。
   过时的Camera打开相机可以参考文章([android-使用Camera+SurfaceView打开摄像头(一)](https://blog.csdn.net/qq_33275672/article/details/137225763?spm=1001.2014.3001.5502))
   
  ### 1,效果图如下,支持横竖屏自适应
   
   ![横屏](https://img-blog.csdnimg.cn/direct/88f07d8257574535b473a4987431ec1f.jpeg#pic_center)
![竖屏](https://img-blog.csdnimg.cn/direct/97ef88a80f5540a6b78785cd4ad851c2.jpeg#pic_center)
### 2,使用步骤

2.1主布局
```java
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity"
    android:id="@+id/rl">
    <com.smart.camera2.CameraView
        android:layout_alignParentEnd="true"
        android:layout_width="108dp"
        android:id="@+id/camera_view"
        android:layout_alignParentBottom="true"
        android:layout_height="192dp"/>
</RelativeLayout>
   ```
   2.2代码逻辑,初始化
```java
camera_view = findViewById(R.id.camera_view);
        camera_view.initCamera();
```
2.3生命周期
```java
   @Override
    protected void onPause() {
        super.onPause();
        camera_view.onPause();
    }
```
2.4权限的获取和处理

```java
  // 处理权限请求的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        camera_view.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
```
### 3,CamerView分析
CameraView继承SurfaceView,在SurfaceView的基础上进行的封装,源码如下:

```java
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
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.e(TAG, "surfaceCreated: ");
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
                Log.e(TAG, "surfaceChanged: ");
                // 更新相机预览尺寸
                updatePreview();
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.e(TAG, "surfaceDestroyed: ");
                // 释放相机资源
                closeCamera();
            }
        });
    }
    /**
     * 初始化相机
     */
    public void initCamera() {
        changeCameraOri(getResources().getConfiguration().orientation);
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
    /**
     * 暂停
     */
    protected void onPause() {
        closeCamera();
    }
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changeCameraOri(newConfig.orientation);
    }
    /**
     * 横竖屏处理
     * @param orientation 横竖屏
     */
    private void changeCameraOri(int orientation) {
        float ratioScreen = 0.28f;
        float ratioCamera = 16 / 9f;
        //以窄边为标准
        //竖屏
        int screenWidthPortrait = (int) (com.smart.camera2.PhoneHelper.getScreenWidthReal(getContext()) * ratioScreen);
        int screenHeightPortrait = (int) (screenWidthPortrait * ratioCamera);
        //横屏
        int screenHeightLandScape = (int) (com.smart.camera2.PhoneHelper.getScreenHeightReal(getContext()) * ratioScreen);
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
        setLayoutParams(layoutParams);
    }
    /**
     * 打开相机
     */
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
                    Log.e(TAG, "onDisconnected: ");
                }
                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "onError: ");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 关闭相机
     */
    private void closeCamera() {
        Log.e(TAG, "closeCamera: ");
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    /**
     * 刷新
     */
    private void updatePreview() {
        if (mCameraDevice == null) {
            return;
        }
        if (mCaptureSession == null) {
            return;
        }
        try {
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取相机
     * @param cameraManager cameraManager
     * @return 相机id
     * @throws CameraAccessException 异常
     */
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
    /**
     * 创建CaptureSession
     */
    private void createCaptureSession() {
        try {
            SurfaceHolder surfaceHolder = getHolder();
            surfaceHolder.setFixedSize(getWidth(), getHeight());
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
```

### 4,demo地址
附上[github上demo地址](https://github.com/jiangzhengyan/Camera2),有需要的可以下载指正。

