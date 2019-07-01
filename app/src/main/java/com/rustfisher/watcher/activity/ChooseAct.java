package com.rustfisher.watcher.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.manager.DatagramMgr;

/**
 * 选择自己的身份
 * Created on 2019-7-1
 */
public class ChooseAct extends AbsBaseActivity implements View.OnClickListener {
    private static final String TAG = "rustAppChooseAct";
    private static final int REQ_PER = 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mPageTag = TAG;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_choose);

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PER);
        } else {
            Log.d(TAG, "有定位权限");
        }
        setOnClickListeners(this, R.id.choose_as_server, R.id.choose_as_client);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PER) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "有定位权限");
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choose_as_server:
                DatagramMgr.restartServerBroadcastThread(getApplicationContext());
                break;
            case R.id.choose_as_client:
                DatagramMgr.restartDatagramReceiveThread(DatagramMgr.UDP_BROADCAST_PORT);
                break;
        }
    }
}
