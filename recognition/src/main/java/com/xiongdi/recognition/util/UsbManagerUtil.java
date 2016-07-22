package com.xiongdi.recognition.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by moubiao on 2016/6/23.
 * USB设备管理的工具类
 */
public class UsbManagerUtil {
    private final String TAG = "moubiao";
    public static final int MESSAGE_ALLOW_DEVICE = 255;
    public static final int MESSAGE_DENY_DEVICE = 256;
    private static final String ACTION_USB_PERMISSION = "com.futronictech.FtrScanDemoActivity.USB_PERMISSION";

    private Context mContext;
    private Handler mHandler = null;
    private UsbManager mUsbManager;
    private final PendingIntent mPermissionIntent;
    private BroadcastReceiver mUsbReceiver;
    private FTR_USB_DEVICE_INTERNAL usb_ctx = null;

    public UsbManagerUtil(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (mPermissionIntent) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                usb_ctx = OpenDevice(device);
                            }
                            UsbManagerUtil.this.mHandler.obtainMessage(MESSAGE_ALLOW_DEVICE).sendToTarget();
                        } else {
                            UsbManagerUtil.this.mHandler.obtainMessage(MESSAGE_DENY_DEVICE).sendToTarget();
                        }

                    }

                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
    }

    /**
     * 搜索所有的设备，并找到正确的设备打开
     */
    public boolean OpenDevice(int instance, boolean is_activity_thread) {
        synchronized (this) {
            if (usb_ctx == null) {
                HashMap<String, UsbDevice> usb_devices = mUsbManager.getDeviceList();
                Iterator<UsbDevice> deviceIterator = usb_devices.values().iterator();
                int index = 0;
                while (deviceIterator.hasNext()) {
                    UsbDevice device = deviceIterator.next();
                    if (IsFutronicDevice(device.getVendorId(), device.getProductId())) {
                        if (index < instance) {
                            index++;
                            continue;
                        }
                        if (!mUsbManager.hasPermission(device)) {
                            mUsbManager.requestPermission(device, mPermissionIntent);
                            if (!is_activity_thread) {
                                synchronized (mPermissionIntent) {
                                    try {
                                        mPermissionIntent.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                return false;
                            }
                        }

                        if (mUsbManager.hasPermission(device)) {
                            usb_ctx = OpenDevice(device);
                        } else {
                            Log.e(TAG, "device not allow");
                        }
                    }
                }
            }

            return usb_ctx != null;
        }
    }

    /**
     * 判断是否是需要的设备
     *
     * @param idVendor  厂商ID
     * @param idProduct 产品ID
     */
    public static boolean IsFutronicDevice(int idVendor, int idProduct) {
        boolean res = false;
        if ((idVendor == 0x0834 && idProduct == 0x0020)
                || (idVendor == 0x0958 && idProduct == 0x0307)
                || (idVendor == 0x1491
                && (idProduct == 0x0020 || idProduct == 0x0025
                || idProduct == 0x0088 || idProduct == 0x0090
                || idProduct == 0x0050 || idProduct == 0x0060
                || idProduct == 0x0098 || idProduct == 0x8098
                || idProduct == 0x9860))
                || (idVendor == 0x1FBA && (idProduct == 0x0013 || idProduct == 0x0012))) {
            res = true;
        }

        return res;
    }

    /**
     * 真正的打开设备的方法
     *
     * @param device usb设备
     * @return
     */
    private FTR_USB_DEVICE_INTERNAL OpenDevice(UsbDevice device) {
        FTR_USB_DEVICE_INTERNAL res = null;
        UsbInterface intf = device.getInterface(0);
        if (intf != null) {
            UsbEndpoint readpoint = null;
            UsbEndpoint writepoint = null;
            for (int i = 0; i < intf.getEndpointCount(); i++) {
                if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT) {
                    writepoint = intf.getEndpoint(i);
                }
                if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
                    readpoint = intf.getEndpoint(i);
                }
            }
            if (readpoint != null && writepoint != null) {
                UsbDeviceConnection connection = mUsbManager.openDevice(device);
                if (connection != null) {
                    Log.i(TAG, "Open device: " + device);
                    res = new FTR_USB_DEVICE_INTERNAL(device, intf, readpoint, writepoint, connection);
                } else {
                    Log.e(TAG, "open device failed: " + device);
                }
            } else {
                Log.e(TAG, "End points not found in device: " + device);
            }
        } else {
            Log.e(TAG, "Get interface failed failed in device: " + device);
        }

        return res;
    }

    public class FTR_USB_DEVICE_INTERNAL {
        public UsbDevice mUsbDevice;
        public UsbInterface mUsbInterface;
        public UsbEndpoint mReadPoint;
        public UsbEndpoint mWritePoint;
        public UsbDeviceConnection mUsbDeviceConnection;
        public boolean mHandleClaimed;

        public FTR_USB_DEVICE_INTERNAL(UsbDevice mDev, UsbInterface mIntf, UsbEndpoint readPoint, UsbEndpoint writePoint, UsbDeviceConnection mDevConnetion) {
            mUsbDevice = mDev;
            mUsbInterface = mIntf;
            mReadPoint = readPoint;
            mWritePoint = writePoint;
            mUsbDeviceConnection = mDevConnetion;
            mHandleClaimed = false;
        }
    }

    public void closeDevice() {
        synchronized (this) {
            if (usb_ctx != null) {
                if (usb_ctx.mUsbDeviceConnection != null) {
                    usb_ctx.mUsbDeviceConnection.releaseInterface(usb_ctx.mUsbInterface);
                    usb_ctx.mUsbDeviceConnection.close();
                }
            }

            usb_ctx = null;
        }
    }

    public void releaseResource() {
        mContext.unregisterReceiver(mUsbReceiver);
    }
}
