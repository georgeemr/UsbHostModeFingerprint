package com.futronictech;

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

public class UsbDeviceDataExchangeImpl {
    public static final int MESSAGE_ALLOW_DEVICE = 255;
    public static final int MESSAGE_DENY_DEVICE = 256;
    private static final int transfer_buffer_size = 1024 * 64;
    private static final int transfer_buffer_size_2 = 1024 * 16;
    private static final String log_tag = "FUTRONICFTR_J";
    private static final String ACTION_USB_PERMISSION = "com.futronictech.FtrScanDemoActivity.USB_PERMISSION";

    private UsbManager mUsbManager;
    private FTR_USB_DEVICE_INTERNAL usb_ctx = null;
    private Context context = null;
    private Handler mHandler = null;
    private final PendingIntent mPermissionIntent;
    private boolean pending_open = false;
    private byte[] max_transfer_buffer = new byte[transfer_buffer_size];
    private final BroadcastReceiver mUsbReceiver;

    public UsbDeviceDataExchangeImpl(Context context, Handler handler) {
        this.context = context;
        this.mHandler = handler;

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
                            UsbDeviceDataExchangeImpl.this.mHandler.obtainMessage(MESSAGE_ALLOW_DEVICE).sendToTarget();
                        } else {
                            UsbDeviceDataExchangeImpl.this.mHandler.obtainMessage(MESSAGE_DENY_DEVICE).sendToTarget();
                        }

                    }

                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        this.context.registerReceiver(mUsbReceiver, filter);
    }

    public void releaseResource() {
        context.unregisterReceiver(mUsbReceiver);
    }

    /**
     * 搜索所有的设备，并找到正确的设备打开
     */
    public boolean OpenDevice(int instance, boolean is_activity_thread) {
        synchronized (this) {
            if (usb_ctx == null) {
                pending_open = false;
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
                                pending_open = false;
                                synchronized (mPermissionIntent) {
                                    try {
                                        mPermissionIntent.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                pending_open = true;
                                return false;
                            }
                        }

                        if (mUsbManager.hasPermission(device)) {
                            usb_ctx = OpenDevice(device);
                        } else {
                            Log.e(log_tag, "device not allow");
                        }
                    }
                }
            }

            return usb_ctx != null;
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

    public boolean IsPendingOpen() {
        return pending_open;
    }

    public boolean DataExchange(byte[] out_data, byte[] in_data, int in_time_out, int out_time_out, boolean keep_open, boolean use_max_end_point_size, int trace_level) {
        synchronized (this) {

            boolean res = false;

            try {

                boolean check_res = false;

                if (usb_ctx != null) {
                    check_res =
                            usb_ctx.mUsbInterface != null &&
                                    usb_ctx.mUsbDeviceConnection != null &&
                                    usb_ctx.mReadPoint != null &&
                                    usb_ctx.mWritePoint != null;

                }

                if (!check_res) {
                    return res;
                }


                //if( (trace_level & (int)0x20) !=0 )
                //{
                //	Log.i( log_tag , String.format( "Java DataExchange S: %d R: %d Use max: %d", out_data.length, in_data.length, use_max_end_point_size ? 1 : 0  ) );
                //}

                if (!usb_ctx.mHandleClaimed) {
                    usb_ctx.mUsbDeviceConnection.claimInterface(usb_ctx.mUsbInterface, false);
                    usb_ctx.mHandleClaimed = true;
                }

                int transfer_bytes = 0;

                if (out_data.length > 0) {
                    transfer_bytes = usb_ctx.mUsbDeviceConnection.bulkTransfer(usb_ctx.mWritePoint, out_data, out_data.length, out_time_out);

                    if (transfer_bytes == -1) {
                        Log.e(log_tag, String.format("Send %d bytes failed", out_data.length));
                        return res;
                    }
                }

                int to_read_size = in_data.length;
                int copy_pos = 0;

                while (to_read_size >= getTransferBuffer().length) {
                    transfer_bytes = usb_ctx.mUsbDeviceConnection.bulkTransfer(usb_ctx.mReadPoint, getTransferBuffer(), getTransferBuffer().length, in_time_out);

                    if (transfer_bytes == -1) {
                        Log.e(log_tag, String.format("Receive %d bytes failed", getTransferBuffer().length));
                        return res;
                    }

                    if (copy_pos + transfer_bytes > in_data.length) {
                        Log.e(log_tag, String.format("Small receive buffer. Need %d bytes", copy_pos + transfer_bytes - in_data.length));
                        return res;
                    }

                    System.arraycopy(getTransferBuffer(), 0, in_data, copy_pos, transfer_bytes);

                    to_read_size -= transfer_bytes;
                    copy_pos += transfer_bytes;
                }

                while (to_read_size >= transfer_buffer_size_2) {
                    transfer_bytes = usb_ctx.mUsbDeviceConnection.bulkTransfer(usb_ctx.mReadPoint, getTransferBuffer(), transfer_buffer_size_2, in_time_out);

                    if (transfer_bytes == -1) {
                        Log.e(log_tag, String.format("Receive %d bytes failed", getTransferBuffer().length));
                        return res;
                    }

                    if (copy_pos + transfer_bytes > in_data.length) {
                        Log.e(log_tag, String.format("Small receive buffer. Need %d bytes", copy_pos + transfer_bytes - in_data.length));
                        return res;
                    }

                    System.arraycopy(getTransferBuffer(), 0, in_data, copy_pos, transfer_bytes);

                    to_read_size -= transfer_bytes;
                    copy_pos += transfer_bytes;
                }

                if (to_read_size > usb_ctx.mReadPoint.getMaxPacketSize()) {
                    int data_left = to_read_size - (to_read_size % usb_ctx.mReadPoint.getMaxPacketSize());

                    if (data_left > 0) {
                        transfer_bytes = usb_ctx.mUsbDeviceConnection.bulkTransfer(usb_ctx.mReadPoint, getTransferBuffer(), data_left, in_time_out);

                        if (transfer_bytes == -1) {
                            Log.e(log_tag, String.format("Receive(1) %d bytes failed", data_left));
                            return res;
                        }

                        if (copy_pos + transfer_bytes > in_data.length) {
                            Log.e(log_tag, String.format("Small receive buffer. Need %d bytes", copy_pos + transfer_bytes - in_data.length));
                            return res;
                        }

                        System.arraycopy(getTransferBuffer(), 0, in_data, copy_pos, transfer_bytes);

                        to_read_size -= transfer_bytes;
                        copy_pos += transfer_bytes;
                    }
                }

                while (to_read_size > 0) {
                    transfer_bytes = usb_ctx.mUsbDeviceConnection.bulkTransfer(usb_ctx.mReadPoint, getTransferBuffer(), use_max_end_point_size ? usb_ctx.mReadPoint.getMaxPacketSize() : to_read_size, in_time_out);

                    if (transfer_bytes == -1) {
                        Log.e(log_tag, String.format("Receive(1) %d bytes failed", to_read_size));
                        return res;
                    }

                    int real_read = to_read_size > usb_ctx.mReadPoint.getMaxPacketSize() ? usb_ctx.mReadPoint.getMaxPacketSize() : to_read_size;

                    if (copy_pos + real_read > in_data.length) {
                        Log.e(log_tag, String.format("Small receive buffer. Need %d bytes", copy_pos + real_read - in_data.length));
                        return res;
                    }

                    System.arraycopy(getTransferBuffer(), 0, in_data, copy_pos, real_read);

                    to_read_size -= real_read;
                    copy_pos += real_read;
                }

                if (!keep_open) {
                    usb_ctx.mUsbDeviceConnection.releaseInterface(usb_ctx.mUsbInterface);
                    usb_ctx.mHandleClaimed = false;
                }

                res = true;
            } catch (Exception e) {
                Log.e(log_tag, String.format("Data exchange fail %s", e.toString()));
            }

            return res;
        }
    }

    public boolean ValidateContext() {
        synchronized (this) {
            boolean res = false;

            if (usb_ctx != null) {
                res =
                        usb_ctx.mUsbInterface != null &&
                                usb_ctx.mUsbDeviceConnection != null &&
                                usb_ctx.mReadPoint != null &&
                                usb_ctx.mWritePoint != null;

            }

            return res;
        }
    }

    public void DataExchangeEnd() {
        synchronized (this) {
            if (usb_ctx != null) {
                if (usb_ctx.mHandleClaimed) {
                    usb_ctx.mUsbDeviceConnection.releaseInterface(usb_ctx.mUsbInterface);
                    usb_ctx.mHandleClaimed = false;
                }
            }
        }
    }

    public boolean GetDeviceInfo(byte[] pack_data) {
        boolean res = false;

        synchronized (this) {
            if (usb_ctx != null) {

                try {

                    int pack_data_index = 0;

                    int vendorId = usb_ctx.mUsbDevice.getVendorId();

                    pack_data[pack_data_index++] = (byte) (vendorId /*>> 0*/);
                    pack_data[pack_data_index++] = (byte) (vendorId >> 8);
                    pack_data[pack_data_index++] = (byte) (vendorId >> 16);
                    pack_data[pack_data_index++] = (byte) (vendorId >> 24);

                    int productId = usb_ctx.mUsbDevice.getProductId();

                    pack_data[pack_data_index++] = (byte) (productId /*>> 0*/);
                    pack_data[pack_data_index++] = (byte) (productId >> 8);
                    pack_data[pack_data_index++] = (byte) (productId >> 16);
                    pack_data[pack_data_index++] = (byte) (productId >> 24);

                    String sn = usb_ctx.mUsbDeviceConnection.getSerial();

                    if (null != sn) {
                        pack_data[pack_data_index++] = 1;

                        byte[] string_bytes = sn.getBytes();

                        int sn_size = string_bytes.length;

                        pack_data[pack_data_index++] = (byte) (sn_size /*>> 0*/);
                        pack_data[pack_data_index++] = (byte) (sn_size >> 8);
                        pack_data[pack_data_index++] = (byte) (sn_size >> 16);
                        pack_data[pack_data_index++] = (byte) (sn_size >> 24);

                        System.arraycopy(string_bytes, 0, pack_data, pack_data_index, string_bytes.length);
                        pack_data_index += string_bytes.length;
                    } else {
                        pack_data[pack_data_index++] = 0;
                    }

					/*String out_put_res = "";

					for(int i = 0; i < pack_data_index; i++)
					{
						String byte_str;
						byte_str = String.format("%X ",pack_data[i]);
						out_put_res += byte_str;
					}
					
					Log.i(log_tag , "Device info blob: " + out_put_res );*/

                    res = true;
                } catch (Exception e) {
                    Log.e(log_tag, "Get device info failed: " + e.toString());
                }
            }
        }

        return res;

    }

    /**
     * 判断是否是需要的设备
     *
     * @param idVendor  厂商ID
     * @param idProduct 产品ID
     */
    public static boolean IsFutronicDevice(int idVendor, int idProduct) {
        boolean res = false;

        if (
                (idVendor == 0x0834 && idProduct == 0x0020) ||
                        (idVendor == 0x0958 && idProduct == 0x0307) ||
                        (idVendor == 0x1491 &&
                                (idProduct == 0x0020 ||
                                        idProduct == 0x0025 ||
                                        idProduct == 0x0088 ||
                                        idProduct == 0x0090 ||
                                        idProduct == 0x0050 ||
                                        idProduct == 0x0060 ||
                                        idProduct == 0x0098 ||
                                        idProduct == 0x8098 ||
                                        idProduct == 0x9860)) ||
                        (idVendor == 0x1FBA && (idProduct == 0x0013 || idProduct == 0x0012))
                ) {
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
                    Log.i(log_tag, "Open device: " + device);
                    res = new FTR_USB_DEVICE_INTERNAL(device, intf, readpoint, writepoint, connection);
                } else {
                    Log.e(log_tag, "open device failed: " + device);
                }
            } else {
                Log.e(log_tag, "End points not found in device: " + device);
            }
        } else {
            Log.e(log_tag, "Get interface failed failed in device: " + device);
        }

        return res;
    }

    public byte[] getTransferBuffer() {
        return max_transfer_buffer;
    }

    public void setTransferBuffer(byte[] max_transfer_buffer) {
        this.max_transfer_buffer = max_transfer_buffer;
    }

	/*public static void GetInterfaces(Context ctx, byte[] pInterfaceList)
    {
		UsbManager DevManager = (UsbManager)ctx.getSystemService(Context.USB_SERVICE);
		
		HashMap<String, UsbDevice> usb_devs =  DevManager.getDeviceList();
    	
    	Iterator<UsbDevice> deviceIterator = usb_devs.values().iterator();
    	
    	int index = 0;
    	
    	for( index = 0; index < Scanner.FTR_MAX_INTERFACE_NUMBER; index++ )
    	{
    		pInterfaceList[index] = Scanner.FTRSCAN_INTERFACE_STATUS_DISCONNECTED;
    	}
    	
    	index = 0;
       
    	while(deviceIterator.hasNext())
    	{
    	    UsbDevice device = deviceIterator.next();
    	    
    	    if( IsFutronicDevice(device.getVendorId(), device.getProductId()))
    	    {
    	    	pInterfaceList[index] = Scanner.FTRSCAN_INTERFACE_STATUS_CONNECTED;
    	    }
    	}
 	
	}*/

    public class FTR_USB_DEVICE_INTERNAL {
        public UsbDevice mUsbDevice;
        public UsbInterface mUsbInterface;
        public UsbEndpoint mReadPoint;
        public UsbEndpoint mWritePoint;
        public UsbDeviceConnection mUsbDeviceConnection;
        public boolean mHandleClaimed;

        public FTR_USB_DEVICE_INTERNAL(
                UsbDevice mDev,
                UsbInterface mIntf,
                UsbEndpoint mReadPoint,
                UsbEndpoint mWritePoint,
                UsbDeviceConnection mDevConnetion) {
            mUsbDevice = mDev;
            mUsbInterface = mIntf;
            this.mReadPoint = mReadPoint;
            this.mWritePoint = mWritePoint;
            mUsbDeviceConnection = mDevConnetion;
            mHandleClaimed = false;
        }
    }
}