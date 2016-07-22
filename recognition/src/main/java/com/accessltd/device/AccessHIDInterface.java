package com.accessltd.device;

/* AccessHIDInterface
 *
 * Created on 02 September 2011
 *
 * Searches for and opens an Access HID device when it connects
 * Requires a minimum of Android Version 3.1
 *
 * This is sample code only, and isn't supported by Access IS
 * 
 */

import android.app.Activity;
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
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.apache.http.util.ByteArrayBuffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;


public class AccessHIDInterface implements Runnable
{
	// The following variables are used to ensure that the correct device is connected to
	// Each of the ADIIP constants refer to the index in the array that stores the relevant
	// information
	private static final char ADIIP_VENDOR_ID = 0;
	private static final char ADIIP_PRODUCT_ID = 1;
	private static final char ADIIP_READ_MI = 2;
	private static final char ADIIP_WRITE_MI = 3;
	private static final char ADIIP_WRITE_END_POINT_INDEX = 4;
	private static final char ADIIP_DEVICE_TYPE = 5;
	
	private boolean controlTransferFlag = false;

	private static final char ACCESS_DEVICE_INTERFACE_INFO[][] =
	{
		// VID,   PID,   R_Mi, W_Mi, WriteEndPointIndex DeviceType
		// Access
		{ 0x0DB5, 0x120, 0x00, 0x01, 2, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// OCR/MSR HID
		{ 0x0DB5, 0x122, 0x02, 0x03, 2, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// OCR/MSR HID - AKB422
		{ 0x0DB5, 0x130, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// LSR130 HID
		{ 0x0DB5, 0x12A, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_BARCODE },			// LSR110 HID
		{ 0x0DB5, 0x13B, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_BARCODE },			// LSR116 HID
		{ 0x0DB5, 0x12B, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_BARCODE },			// LSR1xx HID
		{ 0x0DB5, 0x111, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// Verizon Discovery Keyboard
		// { 0x0DB5, 0x211, 0x00, 0x00, 0, "MegaPOS Keyboard" },
		// { 0x0DB5, 0x211, 0x01, 0x01, 0, "Mouse" },
		// { 0x0DB5, 0x12C, 0x00, 0x00, 0, "HID Custom Keyboard" },		// 1 is mouse, 3 is softprog
		{ 0x0DB5, 0x12C, 0x02, 0x02, 0, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// HID Custom Keyboard - 1 is mouse, 3 is softprog
		{ 0x0DB5, 0x127, 0x02, 0x02, 0, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// OCR/MSR HID - 0 is keyboard, 1 is mouse, 3 is softprog
		{ 0x0DB5, 0x137, 0x00, 0x01, 2, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// OCR312 HID OCR/MSR
		
		{ 0x0DB5, 0x13E, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// OCR312 HID OCR/MSR ***EXPERIMENTAL***
		
		// Default values
		{ 0x0000, 0x000, 0x00, 0x01, 2, AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR },			// Default
		{ 0x0000, 0x000, 0x00, 0x00, 0, AccessDeviceStatusListener.DEVICE_TYPE_BARCODE }			// Default
	};

	char[] CurrentAccessDeviceInterface = null;
    private AccessBarcodeDataListener hostBarcodeDataListener = null;
    private AccessMSRDataListener hostMSRDataListener = null;
    private AccessOCRDataListener hostOCRDataListener = null;
    private AccessDeviceStatusListener hostDeviceStatusListener = null;

	ByteArrayBuffer byteDataReceivedBuffer = new ByteArrayBuffer(0);

	private boolean barcodeOnlyDevice = false;
    private char barcodeID = AccessBarcodeDataListener.BARCODE_ID_UNKNOWN;
    private Activity hostDisplayActivity = null;
    private PendingIntent permissionIntent = null;
    private boolean BarcodeEnabled = false;
    private boolean MSREnabled = false;
    private boolean OCREnabled = false;
	// private volatile ByteBuffer receiveBarcodeBuffer = null;
    // private final int BUFFER_SIZE = 1024;

    private Handler handlerFindDevice = null;
	private UsbDevice currentDevice = null;
	private UsbManager usbManager = null;
	private UsbDeviceConnection connectionRead = null;
	private UsbDeviceConnection connectionWrite = null;
	private Thread threadRead = null;
	private UsbInterface usbInterfaceRead = null;
	private UsbInterface usbInterfaceWrite = null;
	private UsbEndpoint usbEndpointRead = null;
	private UsbEndpoint usbEndpointWrite = null;
	private Object lockStopping = new Object();
	private Object lockStopped = new Object();
	private boolean connected = false;
	private boolean stopping = false;
	private boolean stopped = true;
	private boolean finished = false;
	private boolean applicationStateStopped = false;

	private static final String ACTION_USB_PERMISSION = "com.access.device.USB_PERMISSION";

	/*
	 * Receives a requested broadcast from the operating system.
	 * In this case the following actions are handled:
	 * 		USB_PERMISSION
	 * 		UsbManager.ACTION_USB_DEVICE_DETACHED
	 * 		UsbManager.ACTION_USB_DEVICE_ATTACHED
	 */
	private final BroadcastReceiver usbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action))
			{
				// A permission response has been received, validate if the user has
				// GRANTED, or DENIED permission
				synchronized (this)
				{
					UsbDevice deviceConnected = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						// Permission has been granted, so connect to the device
						// If this fails, then keep looking
						if (deviceConnected != null)
						{
							// call method to setup device communication
							currentDevice = deviceConnected;
							if (!ConnectToDeviceInterface(currentDevice))
							{
								if (handlerFindDevice == null)
								{
									handlerFindDevice = new Handler();
								}
								handlerFindDevice.removeCallbacks(findDeviceTimeTask);
								handlerFindDevice.postDelayed(findDeviceTimeTask, 100);
							}
							sendEnabledMessage();
			        		if (hostDeviceStatusListener != null)
			        		{
			        			hostDeviceStatusListener.AccessDeviceConnected(CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE], Integer.toString(currentDevice.getVendorId()), Integer.toString(currentDevice.getProductId()));
			        		}
						}
						else
						{
							// Permission has not been granted, so keep looking for another
							// device to be attached....
							currentDevice = null;
							if (handlerFindDevice == null)
							{
								handlerFindDevice = new Handler();
							}
							handlerFindDevice.removeCallbacks(findDeviceTimeTask);
							handlerFindDevice.postDelayed(findDeviceTimeTask, 1000);
						}
					}
				}
			}
			else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				// A device has been detached from the device, so close all the connections
				// and restart the search for a new device being attached
		        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		        if ((device != null) && (currentDevice != null))
		        {
		        	if (device.equals(currentDevice))
		        	{
		        		// call your method that cleans up and closes communication with the device
		        		CleanUpAndClose();
		        		if (hostDeviceStatusListener != null)
		        		{
		        			try
		        			{
			        			hostDeviceStatusListener.AccessDeviceDisconnected();
							}
							catch (Exception ex)
							{
								// An exception has occured
							}
		        		}
		        	}
			    }
			}
			else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
			{
				// A device has been attached. If not already connected to a device,
				// validate if this device is supported
		        UsbDevice searchDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		        if ((searchDevice != null) && (currentDevice == null))
		        {
		            // call your method that cleans up and closes communication with the device
		        	ValidateFoundDevice(searchDevice);
			    }
			}
		}
	};


	private Runnable findDeviceTimeTask = new Runnable()
	{
		public void run()
		{
			if (handlerFindDevice != null)
			{
				handlerFindDevice.removeCallbacks(findDeviceTimeTask);
				handlerFindDevice = null;
			}
			FindDevice();
		}
	};


	private Handler handlerDataReceived = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			// Get the value from the message
			byte[] byteBuffer = msg.getData().getByteArray("ByteArray");
			int lenBuffer = msg.arg1;
			int startPos = 0;
			int lenData = lenBuffer;
			int Counter = 0;
			char BarcodeChar = 0;
			boolean finalBarcodePacket = false;

			if (CurrentAccessDeviceInterface == null)
			{
				// no device to handle
				return;
			}

			try
			{
				if (((byteBuffer[0] == 2) && (lenBuffer == 64)) || barcodeOnlyDevice)
				{
					// HHP device
					startPos = 5;
					lenData = byteBuffer[1]; // + 5;
					BarcodeChar = (char)byteBuffer[61];
					finalBarcodePacket = (boolean)(((char)byteBuffer[63] & 0x01) == 0);
					for (Counter = 0; Counter < AccessBarcodeDataListener.BARCODE_ID_MAP.length; Counter++)
					{
						if (BarcodeChar == AccessBarcodeDataListener.BARCODE_ID_MAP[Counter][1])
						{
							barcodeID = AccessBarcodeDataListener.BARCODE_ID_MAP[Counter][0];
							break;
						}
					}
				}
				else if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR) > 0)
				{
					// Handle the MSR/OCR data
					lenData = byteBuffer[0];
	                if (lenData < 0)
	                {
	                	lenData += 128;			// Remove the CTS flag
	                }
					startPos = 1;
				}

				if (lenData > 0)
				{
					byteDataReceivedBuffer.append(byteBuffer, startPos, lenData);
				}

				// Validate data received
				lenData = byteDataReceivedBuffer.length();
				if (lenData > 4)
				{
					if ((byteDataReceivedBuffer.byteAt(0) == 0x1e) && (byteDataReceivedBuffer.byteAt(1) == 0x02))
					{
						// Barcode data
						if ((byteDataReceivedBuffer.byteAt(lenData - 1) == 0x1f) && (byteDataReceivedBuffer.byteAt(lenData - 2) == 0x03))
						{
							if (BarcodeEnabled && (hostBarcodeDataListener != null))
							{
								ByteArrayBuffer newBuffer = new ByteArrayBuffer(lenData - 5);	// 2 - start, 2 - end and 1 - barcode id
								newBuffer.append(byteDataReceivedBuffer.toByteArray(), 3, lenData - 5);
								try
								{
									hostBarcodeDataListener.AccessDataBarcodeRx(barcodeID, ByteBuffer.wrap(newBuffer.toByteArray()), lenData - 5);
								}
								catch (Exception ex)
								{
									// An exception has occured
								}
							}
							// Reset the stored data for the next pointer
							byteDataReceivedBuffer = new ByteArrayBuffer(0);
							barcodeID = AccessBarcodeDataListener.BARCODE_ID_UNKNOWN;
						}
					}
					else if (barcodeOnlyDevice || finalBarcodePacket)
					{
						if (byteDataReceivedBuffer.byteAt(0) == 0x02)
						{
							// Barcode data
							if (byteDataReceivedBuffer.byteAt(lenData - 1) == 0x03)
							{
								if (BarcodeEnabled && (hostBarcodeDataListener != null))
								{
									ByteArrayBuffer newBuffer = new ByteArrayBuffer(lenData - 3);	// 1 - start, 1 - end and 1 - barcode id
									newBuffer.append(byteDataReceivedBuffer.toByteArray(), 2, lenData - 3);
									try
									{
										hostBarcodeDataListener.AccessDataBarcodeRx(barcodeID, ByteBuffer.wrap(newBuffer.toByteArray()), lenData - 3);
									}
									catch (Exception ex)
									{
										// An exception has occured
									}
								}
								// Reset the stored data for the next pointer
								byteDataReceivedBuffer = new ByteArrayBuffer(0);
								barcodeID = AccessBarcodeDataListener.BARCODE_ID_UNKNOWN;
							}
						}
						else if (finalBarcodePacket)		// No formatting
						{
							if (BarcodeEnabled && (hostBarcodeDataListener != null))
							{
								ByteArrayBuffer newBuffer = new ByteArrayBuffer(lenData);
								newBuffer.append(byteDataReceivedBuffer.toByteArray(), 0, lenData);
								try
								{
									hostBarcodeDataListener.AccessDataBarcodeRx(barcodeID, ByteBuffer.wrap(newBuffer.toByteArray()), lenData);
								}
								catch (Exception ex)
								{
									// An exception has occured
								}
							}
							// Reset the stored data for the next pointer
							byteDataReceivedBuffer = new ByteArrayBuffer(0);
							barcodeID = AccessBarcodeDataListener.BARCODE_ID_UNKNOWN;
						}
					}
					else if ((byteDataReceivedBuffer.byteAt(0) == 0x0e) && (byteDataReceivedBuffer.byteAt(1) == 0x02))
					{
						// MSR data
						if ((byteDataReceivedBuffer.byteAt(lenData - 1) == 0x0f) && (byteDataReceivedBuffer.byteAt(lenData - 2) == 0x03))
						{
							if (MSREnabled && (hostMSRDataListener != null))
							{
								// Full data first
								int MaxLines = 4;
								ByteBuffer[] tempByteBuffers = new ByteBuffer[MaxLines];
								int[] tempLengths = new int[MaxLines];
								ByteArrayBuffer[] newBufferLines = new ByteArrayBuffer[MaxLines];
								char bufferIndex = 0;
								ByteArrayBuffer newBuffer = new ByteArrayBuffer(lenData - 5);	// 2 - start, 2 - end, 1 - CardType
								newBuffer.append(byteDataReceivedBuffer.toByteArray(), 3, lenData - 5);
								try
								{
									hostMSRDataListener.AccessDataMSRRx((char)byteDataReceivedBuffer.byteAt(2), ByteBuffer.wrap(newBuffer.toByteArray()), lenData - 4);
								}
								catch (Exception ex)
								{
									// An exception has occured
								}

								// Line by line data
								startPos = 3;
								for (Counter = startPos; Counter < lenData - 2; Counter++)	// 2 - start, 2 - end
								{
									if (byteDataReceivedBuffer.byteAt(Counter) == 0x0D)
									{
										bufferIndex = (char)((byteDataReceivedBuffer.byteAt(startPos) >> 4) & 0x0F);
										startPos++;
										bufferIndex--;
										if (bufferIndex < MaxLines)
										{
											if (Counter - startPos > 0)
											{
												newBufferLines[bufferIndex] = new ByteArrayBuffer(Counter - startPos);
												newBufferLines[bufferIndex].append(byteDataReceivedBuffer.toByteArray(), startPos, Counter - startPos);
											}
											startPos = Counter + 1;
											bufferIndex++;
										}
									}
								}
								if (startPos < Counter)
								{
									bufferIndex = (char)((byteDataReceivedBuffer.byteAt(startPos) >> 4) & 0x0F);
									startPos++;
									bufferIndex--;
									if ((Counter - startPos > 0) && (bufferIndex < MaxLines))
									{
										newBufferLines[bufferIndex] = new ByteArrayBuffer(Counter - startPos);
										newBufferLines[bufferIndex].append(byteDataReceivedBuffer.toByteArray(), startPos, Counter - startPos);
									}
								}
								for (Counter = 0; Counter < MaxLines; Counter++)
								{
									if (newBufferLines[Counter] == null)
									{
										newBufferLines[Counter] = new ByteArrayBuffer(0);
									}
									tempByteBuffers[Counter] = ByteBuffer.wrap(newBufferLines[Counter].toByteArray());
									tempLengths[Counter] = newBufferLines[Counter].length();
								}
								try
								{
									hostMSRDataListener.AccessDataMSRTracksRx((char)byteDataReceivedBuffer.byteAt(2), tempByteBuffers, tempLengths);
								}
								catch (Exception ex)
								{
									// An exception has occured
								}
							}
							// Reset the stored data for the next pointer
							byteDataReceivedBuffer = new ByteArrayBuffer(0);
						}
					}
					else if ((byteDataReceivedBuffer.byteAt(0) == 0x1c) && (byteDataReceivedBuffer.byteAt(1) == 0x02))
					{
						// OCR data
						if ((byteDataReceivedBuffer.byteAt(lenData - 1) == 0x1d) && (byteDataReceivedBuffer.byteAt(lenData - 2) == 0x03))
						{
							if (OCREnabled && (hostOCRDataListener != null))
							{
								// Full data first
								int MaxLines = 3;
								ByteBuffer[] tempByteBuffers = new ByteBuffer[MaxLines];
								int[] tempLengths = new int[MaxLines];
								ByteArrayBuffer[] newBufferLines = new ByteArrayBuffer[MaxLines];
								char bufferIndex = 0;
								ByteArrayBuffer newBuffer = new ByteArrayBuffer(lenData - 4);	// 2 - start, 2 - end
								newBuffer.append(byteDataReceivedBuffer.toByteArray(), 2, lenData - 4);
								try
								{
									hostOCRDataListener.AccessDataOCRRx(ByteBuffer.wrap(newBuffer.toByteArray()), lenData - 4);
								}
								catch (Exception ex)
								{
									// An exception has occured
								}

								// Line by line data
								startPos = 2;
								for (Counter = startPos; Counter < lenData - 2; Counter++)	// 2 - start, 2 - end
								{
									if (byteDataReceivedBuffer.byteAt(Counter) == 0x0D)
									{
										if (bufferIndex < MaxLines)
										{
											if (Counter - startPos > 0)
											{
												newBufferLines[bufferIndex] = new ByteArrayBuffer(Counter - startPos);
												newBufferLines[bufferIndex].append(byteDataReceivedBuffer.toByteArray(), startPos, Counter - startPos);
											}
											startPos = Counter + 1;
											bufferIndex++;
										}
									}
								}
								if ((bufferIndex < MaxLines) && (startPos < Counter))
								{
									if (Counter - startPos > 0)
									{
										newBufferLines[bufferIndex] = new ByteArrayBuffer(Counter - startPos);
										newBufferLines[bufferIndex].append(byteDataReceivedBuffer.toByteArray(), startPos, Counter - startPos);
									}
								}
								for (Counter = 0; Counter < MaxLines; Counter++)
								{
									if (newBufferLines[Counter] == null)
									{
										newBufferLines[Counter] = new ByteArrayBuffer(0);
									}
									tempByteBuffers[Counter] = ByteBuffer.wrap(newBufferLines[Counter].toByteArray());
									tempLengths[Counter] = newBufferLines[Counter].length();
								}
								try
								{
									hostOCRDataListener.AccessDataOCRLinesRx(tempByteBuffers, tempLengths);
								}
								catch (Exception ex)
								{
									// An exception has occured
								}
							}
							// Reset the stored data for the next pointer
							byteDataReceivedBuffer = new ByteArrayBuffer(0);
						}
					}
					else
					{
						// Unsupported framing characters
						byteDataReceivedBuffer = new ByteArrayBuffer(0);
					}
				}
			}
			catch (Exception ex)
			{
				// An exception has occured
				byteDataReceivedBuffer = new ByteArrayBuffer(0);
			}
		}
	};


    public void Stop()
    {
    	// Disconnect from the device, and shut down
    	applicationStateStopped = true;
    	// Close and clean up if necessary
    	CleanUpAndClose();
    }


    public void ReStart()
    {
    	if (applicationStateStopped)
    	{
        	// Find any connected devices
        	applicationStateStopped = false;
        	if ((currentDevice == null) && (handlerFindDevice == null))
        	{
    	    	if ((hostDisplayActivity != null) && (usbReceiver != null))
    	    	{
    				IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
    				hostDisplayActivity.registerReceiver(usbReceiver, permissionFilter);

    				IntentFilter deviceAttachedFilter = new IntentFilter();
    				deviceAttachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    				deviceAttachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    				hostDisplayActivity.registerReceiver(usbReceiver, deviceAttachedFilter);
    		    }
    	    	if ((BarcodeEnabled || OCREnabled || MSREnabled) && (handlerFindDevice == null))
    	    	{
    	    		FindDevice();
    	    	}
    	    	else
    	    	{
    				handlerFindDevice = new Handler();
    				handlerFindDevice.postDelayed(findDeviceTimeTask, 100);
    	    	}
        	}
    	}
    	else if (hostDisplayActivity != null)
    	{
	        Intent intent = hostDisplayActivity.getIntent();
	        String action = intent.getAction();

	        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
	        {
		        UsbDevice searchDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		        if ((searchDevice != null) && (currentDevice == null))
		        {
		            // call your method that cleans up and closes communication with the device
		        	ValidateFoundDevice(searchDevice);
			    }
	        }
	        else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
	        {
		        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		        if ((device != null) && (currentDevice != null))
		        {
		        	if (device.equals(currentDevice))
		        	{
		        		// call your method that cleans up and closes communication with the device
		        		CleanUpAndClose();
		        		if (hostDeviceStatusListener != null)
		        		{
		        			try
		        			{
			        			hostDeviceStatusListener.AccessDeviceDisconnected();
							}
							catch (Exception ex)
							{
								// An exception has occured
							}
		        		}
		        	}
			    }
	        }
    	}
    }


	public AccessHIDInterface(Activity newHostActivity)
	{
		hostDisplayActivity = newHostActivity;

		if (hostDisplayActivity != null)
		{
			usbManager = (UsbManager) hostDisplayActivity.getSystemService(Context.USB_SERVICE);
			permissionIntent = PendingIntent.getBroadcast(hostDisplayActivity, 0, new Intent(ACTION_USB_PERMISSION), 0);
			IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
			hostDisplayActivity.registerReceiver(usbReceiver, permissionFilter);

			IntentFilter deviceAttachedFilter = new IntentFilter();
			deviceAttachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			deviceAttachedFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			hostDisplayActivity.registerReceiver(usbReceiver, deviceAttachedFilter);
		}
	}


    /**
     * Clean up
     */
    protected void finalize() throws Throwable
    {
    	finished = true;
    	if ((hostDisplayActivity != null) && (usbReceiver != null))
    	{
			try
			{
	    		hostDisplayActivity.unregisterReceiver(usbReceiver);
			}
			catch (Exception ex)
			{
				// An exception has occured
			}
    	}
    	// Close and clean up if necessary
    	CleanUpAndClose();
    }


    public void setOnBarcodeRxDataListener(AccessBarcodeDataListener newListener)
    {
    	hostBarcodeDataListener = newListener;
    }


    public void setOnOCRRxDataListener(AccessOCRDataListener newListener)
    {
    	hostOCRDataListener = newListener;
    }


    public void setOnMSRRxDataListener(AccessMSRDataListener newListener)
    {
    	hostMSRDataListener = newListener;
    }


    public void setDeviceStatusListener(AccessDeviceStatusListener newListener)
    {
    	hostDeviceStatusListener = newListener;
    }


    public boolean FindDevice()
    {
        // device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        // Search for a connected device
        if ((currentDevice == null) && (hostDisplayActivity != null))
        {
        	UsbManager manager = (UsbManager) hostDisplayActivity.getApplicationContext().getSystemService(Context.USB_SERVICE);
            UsbDevice searchDevice = null;

        	HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        	// device = deviceList.get("deviceName");
        	Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        	while(deviceIterator.hasNext())
        	{
        		searchDevice = deviceIterator.next();
        		if (ValidateFoundDevice(searchDevice))
        		{
        			return true;
        		}
        	}
			if (handlerFindDevice == null)
			{
				handlerFindDevice = new Handler();
			}
			handlerFindDevice.removeCallbacks(findDeviceTimeTask);
			handlerFindDevice.postDelayed(findDeviceTimeTask, 100);
        }
        return false;
    }


    private boolean ValidateFoundDevice(UsbDevice searchDevice)
    {
    	int tempVendorId = 0;

    	// Don't continue without a device
    	if (hostDisplayActivity == null)
    	{
    		return false;
    	}

    	// Validate device
		tempVendorId = searchDevice.getVendorId();
		if ((tempVendorId == 0xDB5) || (tempVendorId == 0x483))
		{
			// Access
			currentDevice = searchDevice;
			usbManager.requestPermission(currentDevice, permissionIntent);
			return true;
		}
		else if (tempVendorId == 0x536)
		{
			// HHP Barcode Imager
			currentDevice = searchDevice;
			usbManager.requestPermission(currentDevice, permissionIntent);
			return true;
		}

		return false;
    }


    private boolean sendEnabledMessage()
    {
    	if (((usbEndpointWrite != null) || controlTransferFlag) && (CurrentAccessDeviceInterface != null))
    	{
    		byte[] DataToSend = new byte[2];

    		if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR) > 0)
			{
        		if (CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] == AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR)
    			{
					DataToSend[0] = (byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX];
					DataToSend[1] = (byte)((OCREnabled | MSREnabled) ? 0x80 : 0x00);
					return send(DataToSend);
    			}
        		else if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_OCR) > 0)
				{
					DataToSend[0] = (byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX];
					DataToSend[1] = (byte)(OCREnabled ? 0x80 : 0x00);
					return send(DataToSend);
				}
				else if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_MSR) > 0)
				{
					DataToSend[0] = (byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX];
					DataToSend[1] = (byte)(MSREnabled ? 0x80 : 0x00);
					return send(DataToSend);
				}
			}
    	}

    	return false;
    }


    public boolean send(byte[] DataToSend)
    {
    	if(!controlTransferFlag)
    	{
    		//behave as normal for older devices with more than 1 endpoint e.g. OCR313
    		
    		
	    	if ((usbEndpointWrite != null) && (CurrentAccessDeviceInterface != null) && (DataToSend.length > 0))
	    	{
				int bufferDataLength = usbEndpointWrite.getMaxPacketSize();
				ByteBuffer buffer = ByteBuffer.allocate(bufferDataLength + 1);
				UsbRequest request = new UsbRequest();
	
				if (barcodeOnlyDevice)
				{
					if (DataToSend[0] != (byte)0xfd)
					{
						// Assume that formatting must be added
				    	buffer.put((byte)0xfd);
				    	buffer.put((byte)DataToSend.length);
					}
				}
				else if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR) > 0)
				{
					if ((DataToSend[0] != (byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX]) && (((DataToSend[1] & 0x80) > 0) || (DataToSend[1] == 0)))
					{
						// Valid start of packet, so don't add a packet header to the data
					}
					else if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_OCR) > 0)
					{
						buffer.put((byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX]);
						buffer.put((byte)(OCREnabled ? 0x80 + DataToSend.length : DataToSend.length));
					}
					else if ((CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] & AccessDeviceStatusListener.DEVICE_TYPE_MSR) > 0)
					{
						buffer.put((byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX]);
						buffer.put((byte)(MSREnabled ? 0x80 + DataToSend.length : DataToSend.length));
					}
				}
				buffer.put(DataToSend);
	
				request.initialize(connectionWrite, usbEndpointWrite);
				request.queue(buffer, bufferDataLength);
				try
				{
					if (request.equals(connectionWrite.requestWait()))
					{
						return true;
					}
				}
				catch (Exception ex)
				{
					// An exception has occured
				}
	    	}
	
	    	return false;
    	}
    	else
    	{
    		//new behaviour for newer devices, e.g. OCR315MKII
    		//utilises control transfer set report for sending data
			int bufferDataLength = 8; // set this to 8 for now usbEndpointWrite.getMaxPacketSize();
			byte[] buffer = new byte[bufferDataLength];
			//UsbRequest request = new UsbRequest();

			if ((DataToSend[0] != (byte)CurrentAccessDeviceInterface[ADIIP_WRITE_END_POINT_INDEX]) && (((DataToSend[1] & 0x80) > 0) || (DataToSend[1] == 0)))
			{
				// Valid start of packet, so don't add a packet header
				for (int i = 0; i< bufferDataLength; i++)
				{
					buffer[i] = DataToSend[i+1];
				}
			}
			else
			{
				buffer[0] = (byte)(OCREnabled ? 0x80 + DataToSend.length : DataToSend.length);
				// only copy the amount of data we have
				if (DataToSend.length < (bufferDataLength - 1))
				{
					bufferDataLength = DataToSend.length + 1;
				}
				
				for (int i = 1; i< bufferDataLength; i++)
				{
					buffer[i] = DataToSend[i-1];
				}
			}
			// Set Output report
    		int transfer = connectionWrite.controlTransfer(0x21, 0x09, 0x0200, CurrentAccessDeviceInterface[ADIIP_WRITE_MI], buffer, buffer.length, 0);
    		
    		if(transfer >= 0)
    		{
    			return true;
    		}
    		else
    		{
    			return false;
    		}
    		
    	}
    }


    private boolean ConnectToDeviceInterface(UsbDevice connectDevice)
    {
    	int Counter = 0;
    	char[] AccessDeviceInterface = null;

    	if ((threadRead == null) && (connectDevice.getInterfaceCount() > 0))
    	{
    		for (Counter = 0; Counter < ACCESS_DEVICE_INTERFACE_INFO.length; Counter++)
    		{
    			if ((ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_VENDOR_ID] == connectDevice.getVendorId()) &&
    					(ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_PRODUCT_ID] == connectDevice.getProductId()))
    			{
    				AccessDeviceInterface = ACCESS_DEVICE_INTERFACE_INFO[Counter];
    				break;
    			}
    			else if ((ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_VENDOR_ID] == 0x00) &&
    					(ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_PRODUCT_ID] == 0x00))
    			{
    				// Default instance
    				if ((connectDevice.getInterfaceCount() == 1) && (ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_READ_MI] == ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_WRITE_MI]))
    				{
        				AccessDeviceInterface = ACCESS_DEVICE_INTERFACE_INFO[Counter];
        				break;
    				}
    				else if ((connectDevice.getInterfaceCount() > 1) && (ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_READ_MI] != ACCESS_DEVICE_INTERFACE_INFO[Counter][ADIIP_WRITE_MI]))
    				{
        				AccessDeviceInterface = ACCESS_DEVICE_INTERFACE_INFO[Counter];
        				break;
    				}
    			}
    		}

    		// Haven't found the device, or a default
    		if (AccessDeviceInterface == null)
    		{
    			return false;
    		}

    		UsbEndpoint ep1 = null;
    		UsbEndpoint ep2 = null;

	    	if (AccessDeviceInterface[ADIIP_READ_MI] == AccessDeviceInterface[ADIIP_WRITE_MI])
	    	{
		    	usbInterfaceRead = connectDevice.getInterface(AccessDeviceInterface[ADIIP_READ_MI]);
		    	usbInterfaceWrite = usbInterfaceRead;
		    	if (usbInterfaceRead.getEndpointCount() == 2)
		    	{
		    		ep1 = usbInterfaceRead.getEndpoint(0);
		    		ep2 = usbInterfaceRead.getEndpoint(1);
		    	}
		    	else if (usbInterfaceRead.getEndpointCount() == 1)
		    	{
		    		ep1 = usbInterfaceRead.getEndpoint(0);
		    		ep2 = usbInterfaceWrite.getEndpoint(0);
		    		controlTransferFlag = true;
		    	}

	    	}
	    	else // if (AccessDeviceInterface[ADIIP_READ_MI] != AccessDeviceInterface[ADIIP_WRITE_MI])
	    	{
		    	usbInterfaceRead = connectDevice.getInterface(AccessDeviceInterface[ADIIP_READ_MI]);
	    		usbInterfaceWrite = connectDevice.getInterface(AccessDeviceInterface[ADIIP_WRITE_MI]);
		    	if ((usbInterfaceRead.getEndpointCount() == 1) && (usbInterfaceWrite.getEndpointCount() == 1))
		    	{
		    		ep1 = usbInterfaceRead.getEndpoint(0);
		    		ep2 = usbInterfaceWrite.getEndpoint(0);
		    	}
	    	}

	    	if ((ep1 == null) || (ep2 == null))
	    	{
	    		return false;
	    	}
	    	else
	    	{
	    		if (ep1.getType() == UsbConstants.USB_ENDPOINT_XFER_INT)
	    		{
		    		if (ep1.getDirection() == UsbConstants.USB_DIR_IN)
		    		{
		    			usbEndpointRead = ep1;
		    		}
		    		else if (ep1.getDirection() == UsbConstants.USB_DIR_OUT)
		    		{
		    			usbEndpointWrite = ep1;
		    		}
	    		}
	    		if (ep2.getType() == UsbConstants.USB_ENDPOINT_XFER_INT)
	    		{
		    		if (ep2.getDirection() == UsbConstants.USB_DIR_IN)
		    		{
		    			usbEndpointRead = ep2;
		    		}
		    		else if (ep2.getDirection() == UsbConstants.USB_DIR_OUT)
		    		{
		    			usbEndpointWrite = ep2;
		    		}
	    		}
	    		if (usbEndpointRead == null) 
		    	{
		    		return false;
		    	}
	    		
	    		if(usbEndpointWrite == null && !controlTransferFlag)
	    		{
	    			return false;
	    		}
	    			
	    		
		    	connectionRead = usbManager.openDevice(connectDevice);
		    	connectionRead.claimInterface(usbInterfaceRead, true);
		    	if (AccessDeviceInterface[ADIIP_READ_MI] == AccessDeviceInterface[ADIIP_WRITE_MI])
		    	{
		    		connectionWrite = connectionRead;
		    	}
		    	else // if (AccessDeviceInterface[ADIIP_READ_MI] != AccessDeviceInterface[ADIIP_WRITE_MI])
		    	{
			    	connectionWrite = usbManager.openDevice(connectDevice);
			    	connectionWrite.claimInterface(usbInterfaceWrite, true);
		    	}
	    	}

	    	CurrentAccessDeviceInterface = AccessDeviceInterface;
	    	barcodeOnlyDevice = (boolean)(CurrentAccessDeviceInterface[ADIIP_DEVICE_TYPE] == AccessDeviceStatusListener.DEVICE_TYPE_BARCODE);
	    	connected = true;
	    	setStopped(false);
	    	setStopping(false);

    		threadRead = new Thread(this);
    		threadRead.start();

    		return true;
    	}
    	return false;
    }


    private void CleanUpAndClose()
    {
    	int Counter = 0;

    	// Ensure that the handler is alwyas cleared
		try
		{
			if (handlerFindDevice != null)
			{
				handlerFindDevice.removeCallbacks(findDeviceTimeTask);
				handlerFindDevice = null;
			}
        }
        catch (Exception ex)
        {
            // System.out.println("Closing connection to device");
        }

    	if (connected)
    	{
    		try
    		{
	    		setStopping(true);
	    		Counter = 0;
	    		while (!getStopped() && (Counter++ < 5))
	    		{
	    			try
	    			{
		    			Thread.sleep(10);
		    			Thread.yield();
					}
					catch (Exception ex)
					{
						// An exception has occured
					}
	    		}
    		}
            catch (Exception ex)
            {
                // System.out.println("Closing connection to device");
            }

	    	// Close UsbInterface by calling releaseInterface()
	    	// Close UsbDeviceConnection by calling close()
    		if (usbInterfaceRead != null)
    		{
        		try
        		{
	        		if (!connectionRead.equals(connectionWrite))
	        		{
	        			connectionWrite.releaseInterface(usbInterfaceWrite);
	            		connectionWrite.close();
	        		}
        		}
                catch (Exception ex)
                {
                    // System.out.println("Closing connection to device");
                }
        		try
        		{
	        		connectionRead.releaseInterface(usbInterfaceRead);
	        		connectionRead.close();
        		}
                catch (Exception ex)
                {
                    // System.out.println("Closing connection to device");
                }
        		connectionWrite = null;
        		connectionRead = null;
    		}

            try
            {
	    		if (threadRead != null)
	    		{
	    			// The thread still hasn't shut down, so stop it...
	    			threadRead.join(100);
	    		}
            }
            catch (Exception ex)
            {
                // System.out.println("Closing connection to device");
            }

            try
            {
	    		setStopped(true);
	    		threadRead = null;
	    		currentDevice = null;
	    		connected = false;
	    		CurrentAccessDeviceInterface = null;
            }
            catch (Exception ex)
            {
                // System.out.println("Closing connection to device");
            }
    	}

    	// Reset the find device handler if required
    	try
    	{
	    	if (!finished && !applicationStateStopped)
	    	{
				handlerFindDevice = new Handler();
				handlerFindDevice.postDelayed(findDeviceTimeTask, 100);
	    	}
    	}
    	catch (Exception ex)
    	{
    		// An exception has occured
    	}
    }


    public void setBarcodeReadingEnabled(boolean Enabled)
    {
    	if (BarcodeEnabled != Enabled)
    	{
	    	BarcodeEnabled = Enabled;
	    	if ((currentDevice == null) && BarcodeEnabled && (handlerFindDevice == null))
	    	{
	    		FindDevice();
	    	}
    	}
    }


    public boolean getBarcodeReadingEnabled()
    {
    	return BarcodeEnabled;
    }


    public void setMSRReadingEnabled(boolean Enabled)
    {
    	if (MSREnabled != Enabled)
    	{
	    	MSREnabled = Enabled;
	    	if ((currentDevice == null) && MSREnabled && (handlerFindDevice == null))
	    	{
	    		FindDevice();
	    	}
	    	else
	    	{
	    		sendEnabledMessage();
	    	}
    	}
    }


    public boolean getMSRReadingEnabled()
    {
    	return MSREnabled;
    }


    public void setOCRReadingEnabled(boolean Enabled)
    {
    	if (OCREnabled != Enabled)
    	{
	    	OCREnabled = Enabled;
	    	if ((currentDevice == null) && OCREnabled && (handlerFindDevice == null))
	    	{
	    		FindDevice();
	    	}
	    	else
	    	{
	    		sendEnabledMessage();
	    	}
    	}
    }


    public boolean getOCRReadingEnabled()
    {
		return OCREnabled;
    }


    private void setStopped(boolean newStopped)
    {
    	synchronized(lockStopped)
    	{
    		stopped = newStopped;
    	}
    }


    private boolean getStopped()
    {
    	synchronized(lockStopped)
    	{
    		return stopped;
    	}
    }


    private void setStopping(boolean newStopping)
    {
    	synchronized(lockStopping)
    	{
    		stopping = newStopping;
    	}
    }


    private boolean getStopping()
    {
    	synchronized(lockStopping)
    	{
    		return stopping;
    	}
    }


    /*
     * Thread handler for the
     */
	@Override
	public void run()
	{
		if (connected && !getStopping() && !getStopped())
		{
			//int dataLengthReceived = 0;
			int bufferDataLength = usbEndpointRead.getMaxPacketSize();
			// Cycle through multiple instances of messages to ensure
			// that they don't get reused...
			Message newMessage[] = new Message[10];
			int newMessageIndex = 0;

			ByteBuffer buffer = ByteBuffer.allocate(bufferDataLength + 1);
			UsbRequest requestQueued = null;
			UsbRequest request = new UsbRequest();
			request.initialize(connectionRead, usbEndpointRead);

			try
			{
				while (!getStopping())
				{
					request.queue(buffer, bufferDataLength);
					requestQueued = connectionRead.requestWait();
					if (request.equals(requestQueued))
					{
						byte[] byteBuffer = new byte[bufferDataLength + 1];
						buffer.rewind();
						buffer.get(byteBuffer, 0, bufferDataLength);

						// Handle data received
						Bundle bundleBuffer = new Bundle();
						bundleBuffer.putByteArray("ByteArray", byteBuffer);

						newMessage[newMessageIndex] = new Message();
						newMessage[newMessageIndex].arg1 = bufferDataLength; // dataLengthReceived;
						newMessage[newMessageIndex].setData(bundleBuffer);
						handlerDataReceived.sendMessage(newMessage[newMessageIndex]);
						newMessageIndex++;
						newMessageIndex %= 10;

						buffer.clear();
					}
					else
					{
						Thread.sleep(20);
					}
				}
			}
			catch (Exception ex)
			{
				// An exception has occured
			}
			try
			{
				request.cancel();
				request.close();
			}
			catch (Exception ex)
			{
				// An exception has occurred
			}
		}
		setStopped(true);
		threadRead = null;
	}
}
