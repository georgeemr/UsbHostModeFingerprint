package com.accessltd.device;

/* AccessMSRDataListener
 *
 * Created on 02 September 2011
 *
 * This is sample code only, and isn't supported by Access IS
 * 
 */


import java.nio.ByteBuffer;


public interface AccessMSRDataListener {

	/**
	 * Implements a call back method for data
	 */
	public void AccessDataMSRRx(char CardType, ByteBuffer dataReceived, int dataReceivedLen);
	public void AccessDataMSRTracksRx(char CardType, ByteBuffer[] dataReceivedTrack, int[] dataReceivedLen);
}
