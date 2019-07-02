package com.rustfisher.watcher.views;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * 设备的列表适配器
 * Created on 2019-7-2
 */
public class DatagramDeviceReAdapter extends RecyclerView.Adapter<DatagramDeviceReAdapter.DVH> {

    @NonNull
    @Override
    public DVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull DVH dvh, int i) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    static class DVH extends RecyclerView.ViewHolder {

        public DVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}
