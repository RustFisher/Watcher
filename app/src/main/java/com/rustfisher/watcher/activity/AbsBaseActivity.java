package com.rustfisher.watcher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础类
 * Create on 2019-1-7
 */
public abstract class AbsBaseActivity extends FragmentActivity {
    protected Activity mSelfAct;
    protected String mPageTag = "rustAct";

    public AbsBaseActivity() {
        mSelfAct = this;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (enableStatusBarColor()) {
                Window window = getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(customStatusBarColor());
            }
        }
        mSelfAct = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // 是否使用自定义状态栏颜色
    protected boolean enableStatusBarColor() {
        return false;
    }

    protected int customStatusBarColor() {
        return Color.BLACK;
    }

    void hideSoftKeyboard() {
        try {
            InputMethodManager inputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMgr.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            Log.e(mPageTag, "hideSoftKeyboard: ", e);
        }
    }

    // 设置点击监听器
    protected void setOnClickListeners(View.OnClickListener l, View... views) {
        for (View v : views) {
            v.setOnClickListener(l);
        }
    }

    protected void setOnClickListeners(View.OnClickListener l, int... resIds) {
        for (int r : resIds) {
            findViewById(r).setOnClickListener(l);
        }
    }

    protected void checkPermission(String[] permissions, final int reqCode) {
        List<String> perList = new ArrayList<>();
        for (String p : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, p)) {
                Log.e(mPageTag, "没有权限: " + p);
                perList.add(p);
            }
        }
        if (!perList.isEmpty()) {
            String[] per = new String[perList.size()];
            for (int i = 0; i < per.length; i++) {
                per[i] = perList.get(i);
            }
            ActivityCompat.requestPermissions(this, per, reqCode);
        }
    }

    protected void showShort(Context context, CharSequence message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    protected void showShort(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // 判断网络是否有连接
    protected boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo anInfo : info) {
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean appInstalledOrNot(String uri) {
        try {
            PackageManager pm = getPackageManager();
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            Log.e(mPageTag, "appInstalledOrNot: " + uri, e);
        }
        return false;
    }

}
