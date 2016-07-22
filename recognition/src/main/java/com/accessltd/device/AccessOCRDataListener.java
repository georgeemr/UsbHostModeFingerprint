package com.accessltd.device;

/* AccessOCRDataListener
 *
 * Created on 02 September 2011
 *
 * This is sample code only, and isn't supported by Access IS
 * 
 */


import java.nio.ByteBuffer;


public interface AccessOCRDataListener {

	
	public static final String[] OCR_PARSED_FIELD_NAMES = {
		"DOB",
		"Expiry",
		"Issuer",
		"Document Type",
		"Last Name",
		"First Name",
		"Nationality",
		"Discretionary",
		"Discretionary2",
		"Document Number",
		"Sex"
	};

	public static final String[] OCR_PARSED_FIELD_IDS = {
		"DOB",
		"Expiry",
		"Issuer",
		"DocumentType",
		"LastName",
		"FirstName",
		"Nationality",
		"Discretionary",
		"Discretionary2",
		"DocumentNumber",
		"Sex"
	};
	
	
	
	/**
	 * Implements a call back method for data
	 */
	public void AccessDataOCRRx(ByteBuffer dataReceived, int dataReceivedLen);
	public void AccessDataOCRLinesRx(ByteBuffer[] dataReceivedLine, int[] dataReceivedLen);
}
