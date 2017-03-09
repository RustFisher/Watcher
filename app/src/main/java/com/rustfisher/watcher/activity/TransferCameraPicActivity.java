package com.rustfisher.watcher.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.manager.LocalDevice;
import com.rustfisher.watcher.service.CommunicationService;
import com.rustfisher.watcher.utils.LocalUtils;

import java.io.ByteArrayOutputStream;

import static com.rustfisher.watcher.activity.MainActivity.TAG;

/**
 * Transfer camera previews
 * Created by Rust Fisher on 2017/3/6.
 */
public class TransferCameraPicActivity extends Activity implements SurfaceHolder.Callback {

    private TextView mTransferStatusTv;
    private ImageView mPicIv;
    private LocalDevice mLocalDevice = LocalDevice.getInstance();

    private Camera mCamera;
    private SurfaceView mCameraPreview;
    private SurfaceHolder mHolder;
    private int mCameraId = 0;
    private boolean mSafeToTakePicture = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_transfer_camera_preview);
        initUI();
        initUtils();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        unregisterReceiver(mReceiver);
    }

    private void initUI() {
        mCameraPreview = (SurfaceView) findViewById(R.id.cameraView);
        mHolder = mCameraPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPicIv = (ImageView) findViewById(R.id.picIv);
        mTransferStatusTv = (TextView) findViewById(R.id.transferStatusTv);
        findViewById(R.id.sendPicBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mCamera && mSafeToTakePicture) {
                    mCamera.takePicture(mShutterCallback, null, mJPEGCallback);
                    mSafeToTakePicture = false;
                }
            }
        });

        findViewById(R.id.stopSendPicBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDevice.setSendingOutCameraView(false);
                updateUI();
            }
        });

        findViewById(R.id.sendBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapDrawable drawable = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_launcher);
                Bitmap bt = drawable.getBitmap();
                ByteArrayOutputStream bAos = new ByteArrayOutputStream();
                bt.compress(Bitmap.CompressFormat.PNG, 100, bAos);
                mLocalDevice.sendPNGOut(bAos.toByteArray());
            }
        });

    }

    private void initUtils() {
        mCameraId = Camera.getNumberOfCameras() - 1;
        registerReceiver(mReceiver, new IntentFilter(CommunicationService.MSG_ONE_PIC));
        registerReceiver(mReceiver, new IntentFilter(CommunicationService.MSG_ONE_CAMERA));
    }

    private void updateUI() {
        if (LocalDevice.isSendingOutCameraView()) {
            mTransferStatusTv.setText("SendingOutCameraView");
        } else {
            mTransferStatusTv.setText("Not Sending");
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CommunicationService.MSG_ONE_PIC.equals(action)) {
                byte[] picArr = LocalDevice.getOnePicData();
                if (null != picArr) {
                    Log.d(TAG, "onReceive one pic, len=" + picArr.length);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(picArr, 0, picArr.length);
                    mPicIv.setImageBitmap(bitmap);
                }
            } else if (CommunicationService.MSG_ONE_CAMERA.equals(action)) {
                MsgBean preview = (MsgBean) intent.getSerializableExtra(CommunicationService.MSG_ONE_CAMERA);
                byte[] jpegBytes = preview.getCameraBytes();
                Log.d(TAG, "[act] onReceive: one JPEG.  len=" + jpegBytes.length);
                
            }
        }
    };

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.PictureCallback mJPEGCallback = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            Camera.Parameters ps = camera.getParameters();
            if (ps.getPictureFormat() == PixelFormat.JPEG) {
                Log.d(TAG, "onPictureTaken: " + data.length);
                mLocalDevice.sendCameraJPEG(data);
            }
            camera.startPreview();
            mSafeToTakePicture = true;
        }
    };

    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.d(TAG, "onShutter");
        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (null == mCamera) {
            mCamera = Camera.open();
            try {
                mCamera.setPreviewDisplay(holder);
                // NV21 Constant Value: 17 (0x00000011)
                Log.d(TAG, "Camera.getParameters().getPreviewFormat() = " + mCamera.getParameters().getPreviewFormat());
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                Log.d(TAG, "previewSize " + previewSize.width + ", " + previewSize.height);
                LocalUtils.setCameraDisplayOrientation(this, mCameraId, mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
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
