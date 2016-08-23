package com.xiongdi.recognition.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.adapter.BluetoothDeviceAdapter;

import java.util.List;

/**
 * Created by moubiao on 2016/8/22.
 * 选择蓝牙设备的对话框
 */
public class ListDialogFragment extends DialogFragment {
    private ListView mDeviceLV;
    private SelectListener mSelectListener;
    private BluetoothDeviceAdapter mAdapter;
    private List<BluetoothDevice> mDeviceList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
        mAdapter = new BluetoothDeviceAdapter(getActivity(), mDeviceList);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bluetooth_layout, container, false);
        mDeviceLV = (ListView) view.findViewById(R.id.device_lv);
        mDeviceLV.setAdapter(mAdapter);
        setInnerListener();

        return view;
    }

    private void setInnerListener() {
        mDeviceLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectListener.selectItem(mDeviceList.get(position));
                dismiss();
            }
        });
    }

    public void setData(List<BluetoothDevice> data){
        mDeviceList = data;
    }

    public void setSelectListener(SelectListener listener) {
        mSelectListener = listener;
    }

    public interface SelectListener {
        void selectItem(BluetoothDevice device);
    }
}
