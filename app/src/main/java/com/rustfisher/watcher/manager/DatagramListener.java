package com.rustfisher.watcher.manager;

import com.rustfisher.watcher.transfer.model.commond.BroadcastMsg;

import java.util.List;

/**
 * 数据包监听器
 * Created on 2019-7-2
 */
public abstract class DatagramListener {
    public void onDeviceList(List<BroadcastMsg> list) {

    }
}
