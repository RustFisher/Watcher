package com.rustfisher.watcher.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.manager.DatagramListener;
import com.rustfisher.watcher.manager.DatagramMgr;
import com.rustfisher.watcher.transfer.model.commond.BroadcastMsg;
import com.rustfisher.watcher.views.DatagramDeviceReAdapter;

import java.util.List;

/**
 * 选择自己的身份
 * Created on 2019-7-1
 */
public class BroadcastAct extends AbsBaseActivity implements View.OnClickListener {
    private static final String TAG = "rustAppBroadcastAct";
    private static final int REQ_PER = 1000;
    DatagramDeviceReAdapter mDatagramDeviceReAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mPageTag = TAG;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_broadcast);

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PER);
        } else {
            Log.d(TAG, "有定位权限");
        }

        mDatagramDeviceReAdapter = new DatagramDeviceReAdapter();
        RecyclerView deviceRv = findViewById(R.id.broadcast_page_device_rv);
        deviceRv.setAdapter(mDatagramDeviceReAdapter);
        deviceRv.setLayoutManager(new LinearLayoutManager(this));
        mDatagramDeviceReAdapter.setOnItemClickListener(new DatagramDeviceReAdapter.OnItemClickListener() {
            @Override
            public void onClick(BroadcastMsg msg) {
                Log.d(TAG, "onClick: " + msg);
            }
        });
        setOnClickListeners(this, R.id.restart_broadcast);
        DatagramMgr.addListener(mDatagramListener);
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
    protected void onDestroy() {
        super.onDestroy();
        DatagramMgr.removeListener(mDatagramListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.restart_broadcast:
                DatagramMgr.restartBroadcastThread(getApplicationContext());
                break;
        }
    }

    private DatagramListener mDatagramListener = new DatagramListener() {
        @Override
        public void onDeviceList(List<BroadcastMsg> list) {
            super.onDeviceList(list);
        }
    };
}
