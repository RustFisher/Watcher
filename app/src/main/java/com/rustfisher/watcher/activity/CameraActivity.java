package com.rustfisher.watcher.activity;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.utils.LocalUtils;

import java.io.IOException;

/**
 * Camera view.
 * Created by Rust Fisher on 2017/3/6.
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    private Camera mCamera;

    private SurfaceView mCameraPreview;
    private SurfaceHolder mHolder;
    private int mCameraId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_camera);
        initUI();
        mCameraId = Camera.getNumberOfCameras() - 1;
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }

    private void initUI() {
        mCameraPreview = (SurfaceView) findViewById(R.id.cameraSView);
        mHolder = mCameraPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (null == mCamera) {
            mCamera = Camera.open();
            try {
                mCamera.setPreviewDisplay(holder);
                Camera.Parameters param = mCamera.getParameters();
                param.setPictureFormat(ImageFormat.JPEG);
                mCamera.setParameters(param);
                LocalUtils.setCameraDisplayOrientation(this, mCameraId, mCamera);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        mCameraPreview = null;
    }
}
