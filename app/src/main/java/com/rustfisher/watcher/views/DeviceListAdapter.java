package com.rustfisher.watcher.views;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rustfisher.watcher.R;

import java.util.ArrayList;
import java.util.Collection;

public class DeviceListAdapter extends BaseAdapter {
    private ArrayList<WifiP2pDevice> deviceList;
    private LayoutInflater mInflater;

    public DeviceListAdapter(LayoutInflater inflater) {
        super();
        deviceList = new ArrayList<>();
        mInflater = inflater;
    }

    public void setList(Collection<WifiP2pDevice> list) {
        deviceList = new ArrayList<>(list);
    }

    public WifiP2pDevice getDevice(int position) {
        return deviceList.get(position);
    }

    public void clear() {
        deviceList.clear();
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public WifiP2pDevice getItem(int i) {
        return deviceList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView) view.findViewById(R.id.nameTv);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        WifiP2pDevice device = deviceList.get(i);
        final String deviceName = device.deviceName;
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText("unknown");
        return view;
    }

    static class ViewHolder {
        TextView deviceName;
    }
}