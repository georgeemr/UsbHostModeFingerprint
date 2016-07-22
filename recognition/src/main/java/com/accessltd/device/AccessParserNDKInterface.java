package com.accessltd.device;


public class AccessParserNDKInterface {

	static {
		System.loadLibrary("AccessHIDNDK");
			
	}

	public native String AccessHIDParseLastError();
	public native String AccessHIDParseOCR(String Line1, String Line2, String Line3, boolean Validate);
	public native String AccessHIDParseMSR(String Track1, String Track2, String Track3, boolean Validate);
}
