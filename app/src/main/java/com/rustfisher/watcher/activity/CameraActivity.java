package com.rustfisher.watcher.activity;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.views.CameraPreview;

import static com.rustfisher.watcher.activity.MainActivity.TAG;

/**
 * Camera view.
 * Created by Rust Fisher on 2017/3/6.
 */
public class CameraActivity extends Activity {

    private Camera mCamera;

    private FrameLayout mViewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_camera);
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }

    private void initUI() {
        mViewContainer = (FrameLayout) findViewById(R.id.cameraContainer);
        mCamera = getCamera();
        mViewContainer.addView(new CameraPreview(getApplicationContext(), mCamera));
    }

    private Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "getCamera: ", e);
            e.printStackTrace();
            finish();
        }
        return camera;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

}
