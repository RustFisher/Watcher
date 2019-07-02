package com.rustfisher.watcher.views;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.transfer.model.commond.BroadcastMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备的列表适配器
 * Created on 2019-7-2
 */
public class DatagramDeviceReAdapter extends RecyclerView.Adapter<DatagramDeviceReAdapter.DVH> {

    private List<BroadcastMsg> dataList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public void updateDataList(List<BroadcastMsg> list){
        dataList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new DVH(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_datagram_device, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DVH dvh, int i) {
        final BroadcastMsg device = dataList.get(i);
        dvh.tv1.setText(device.getNickname());
        dvh.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != onItemClickListener) {
                    onItemClickListener.onClick(device);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class DVH extends RecyclerView.ViewHolder {
        View root;
        TextView tv1;

        DVH(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            tv1 = itemView.findViewById(R.id.item_tv1);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(BroadcastMsg msg);
    }
}
