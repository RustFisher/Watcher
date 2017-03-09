package com.rustfisher.watcher.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.manager.LocalDevice;
import com.rustfisher.watcher.service.CommunicationService;

import java.io.ByteArrayOutputStream;

import static com.rustfisher.watcher.activity.MainActivity.TAG;

/**
 * Transfer camera previews
 * Created by Rust Fisher on 2017/3/6.
 */
public class TransferCameraPicActivity extends Activity {

    private TextView mTransferStatusTv;
    private ImageView mPicIv;
    private Camera mCamera;
    private LocalDevice mLocalDevice = LocalDevice.getInstance();

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
        releaseCamera();
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void initUtils() {
        registerReceiver(mReceiver, new IntentFilter(CommunicationService.MSG_ONE_PIC));
    }

    private void initUI() {
        mPicIv = (ImageView) findViewById(R.id.picIv);
        mTransferStatusTv = (TextView) findViewById(R.id.transferStatusTv);
        setCamera();
        findViewById(R.id.sendPicBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDevice.setSendingOutCameraView(true);
                mCamera.startPreview();
                updateUI();
            }
        });

        findViewById(R.id.stopSendPicBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDevice.setSendingOutCameraView(false);
                mCamera.stopPreview();
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

        findViewById(R.id.sendImageBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    int mSendCount = 0; // slow down

    private void setCamera() {
        mCamera = getCamera();
        if (null != mCamera) {
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    mSendCount++;
                    if (mSendCount >= 100) {
                        Log.d(TAG, "mCamera onPreviewFrame: " + data.length);
                        mSendCount = 0;
                        if (LocalDevice.isSendingOutCameraView()) {
//                            mLocalDevice.sendPNGOut(data);
                        }
                    }
                }
            });
        } else {
            Log.e(TAG, "[TransferCameraPicActivity] setCamera: fail");
        }
    }

    private void updateUI() {
        if (LocalDevice.isSendingOutCameraView()) {
            mTransferStatusTv.setText("SendingOutCameraView");
        } else {
            mTransferStatusTv.setText("Not Sending");
        }
    }

    private Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
//            Camera.Parameters parameters = camera.getParameters();
//            parameters.setPreviewFormat(ImageFormat.JPEG);
//            parameters.setPictureSize(200, 200);
//            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "getCamera fail: ", e);
            e.printStackTrace();
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
//                try {
//                    YuvImage image = new YuvImage(picArr, ImageFormat.NV21, 200, 200, null);
//                    if (image != null) {
//                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                        image.compressToJpeg(new Rect(0, 0, 200, 200), 80, stream);
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//                        mPicIv.setImageBitmap(bitmap);
//                        stream.close();
//                    }
//                } catch (Exception ex) {
//                    Log.e("Sys", "Error:" + ex.getMessage());
//                }

            }
        }
    };

}
