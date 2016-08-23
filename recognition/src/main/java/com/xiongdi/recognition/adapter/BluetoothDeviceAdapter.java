package com.xiongdi.recognition.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xiongdi.recognition.R;

import java.util.List;

/**
 * Created by moubiao on 2016/8/22.
 * 蓝牙设备列表的适配器
 */
public class BluetoothDeviceAdapter extends BaseAdapter {
    private List<BluetoothDevice> mDeviceList;
    private Context mContext;
    private LayoutInflater mInflater;

    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> deviceList) {
        mContext = context;
        mDeviceList = deviceList;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.bluetooth_item, parent, false);
            viewHolder.mNameTV = (TextView) convertView.findViewById(R.id.bluetooth_name_tv);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mNameTV.setText(mDeviceList.get(position).getName());

        return convertView;
    }

    private class ViewHolder {
        TextView mNameTV;
    }
}
