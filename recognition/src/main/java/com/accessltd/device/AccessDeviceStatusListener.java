package com.accessltd.device;

/* AccessDeviceStatusListener
 *
 * Created on 02 September 2011
 *
 * This is sample code only, and isn't supported by Access IS
 * 
 */


public interface AccessDeviceStatusListener
{
	public static final char DEVICE_TYPE_BARCODE = 1;
	public static final char DEVICE_TYPE_MSR	  = 2;
	public static final char DEVICE_TYPE_OCR	  = 4;
	public static final char DEVICE_TYPE_MSR_OCR = DEVICE_TYPE_MSR | DEVICE_TYPE_OCR;


	/**
	 * Implements a call back method for data
	 */
	public void AccessDeviceConnected(char DeviceType, String VendorId, String ProductId);
	public void AccessDeviceDisconnected();
}
