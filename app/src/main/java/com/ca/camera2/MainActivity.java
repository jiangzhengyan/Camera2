package com.ca.camera2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    private CameraView camera_view;

    // 处理权限请求的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        camera_view.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        camera_view = findViewById(R.id.camera_view);
        camera_view.initCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera_view.onPause();
    }

}