package com.accessltd.device;

/* AccessBarcodeDataListener
 *
 * Created on 02 September 2011
 *
 * This is sample code only, and isn't supported by Access IS
 * 
 */


import java.nio.ByteBuffer;


public interface AccessBarcodeDataListener
{
	public static final char BARCODE_ID_UNKNOWN 				= 0;
	public static final char BARCODE_ID_AUSTRALIAN_POST 		= 'A';
	public static final char BARCODE_ID_AZTEC 					= 'z';
	public static final char BARCODE_ID_BRITISH_POST 			= 'B';
	public static final char BARCODE_ID_CANADIAN_POST 			= 'C';
	public static final char BARCODE_ID_CHINA_POST 				= 'Q';
	public static final char BARCODE_ID_CODABAR 				= 'a';
	public static final char BARCODE_ID_CODE_11 				= 'h';
	public static final char BARCODE_ID_CODE_128 				= 'j';
	public static final char BARCODE_ID_CODE_16K 				= 'o';
	public static final char BARCODE_ID_CODE_39 				= 'b';
	public static final char BARCODE_ID_CODE_49 				= 'l';
	public static final char BARCODE_ID_CODE_93 				= 'i';
	public static final char BARCODE_ID_DATAMATRIX 				= 'w';
	public static final char BARCODE_ID_EAN_13 					= 'd';
	public static final char BARCODE_ID_EAN_8 					= 'D';
	public static final char BARCODE_ID_EAN_UCC 				= 'y';
	public static final char BARCODE_ID_INTERLEAVED_2_OF_5 		= 'e';
	public static final char BARCODE_ID_JAPANESE_POST 			= 'J';
	public static final char BARCODE_ID_KIX_POST 				= 'K';
	//public static final char BARCODE_ID_KOREA_POST = '';
	public static final char BARCODE_ID_MATRIX_2_OF_5 			= 'm';
	public static final char BARCODE_ID_MAXI_CODE 				= 'x';
	public static final char BARCODE_ID_MICRO_PDF417 			= 'R';
	public static final char BARCODE_ID_MSI 					= 'g';
	public static final char BARCODE_ID_PDF417 					= 'r';
	public static final char BARCODE_ID_PLANET_CODE 			= 'L';
	public static final char BARCODE_ID_PLESSEY_CODE 			= 'n';
	public static final char BARCODE_ID_POSI_CODE 				= 'W';
	public static final char BARCODE_ID_POSTNET 				= 'P';
	public static final char BARCODE_ID_QR_CODE					= 's';
	public static final char BARCODE_ID_IATA_2_OF_5				= 'f';
	public static final char BARCODE_ID_TELEPEN 				= 't';
	//public static final char BARCODE_ID_ = '';


	public static final char[][] BARCODE_ID_MAP =
	{
		{BARCODE_ID_AUSTRALIAN_POST, 'A'},
		{BARCODE_ID_AZTEC, 'z'},
		{BARCODE_ID_BRITISH_POST, 'B'},
		{BARCODE_ID_CANADIAN_POST, 'C'},
		{BARCODE_ID_CHINA_POST, 'Q'},
		{BARCODE_ID_CODABAR, 'a'},
		{BARCODE_ID_CODE_11, 'h'},
		{BARCODE_ID_CODE_128, 'j'},
		{BARCODE_ID_CODE_16K, 'o'},
		{BARCODE_ID_CODE_39, 'b'},
		{BARCODE_ID_CODE_49, 'l'},
		{BARCODE_ID_CODE_93, 'i'},
		{BARCODE_ID_DATAMATRIX, 'w'},
		{BARCODE_ID_EAN_13, 'd'},
		{BARCODE_ID_EAN_8, 'D'},
		{BARCODE_ID_EAN_UCC, 'y'},
		{BARCODE_ID_INTERLEAVED_2_OF_5, 'e'},
		{BARCODE_ID_JAPANESE_POST, 'J'},
		{BARCODE_ID_KIX_POST, 'K'},
		// BARCODE_ID_KOREA_POST = '';
		{BARCODE_ID_MATRIX_2_OF_5, 'm'},
		{BARCODE_ID_MAXI_CODE, 'x'},
		{BARCODE_ID_MICRO_PDF417, 'R'},
		{BARCODE_ID_MSI, 'g'},
		{BARCODE_ID_PDF417, 'r'},
		{BARCODE_ID_PLANET_CODE, 'L'},
		{BARCODE_ID_PLESSEY_CODE, 'n'},
		{BARCODE_ID_POSI_CODE, 'W'},
		{BARCODE_ID_POSTNET, 'P'},
		{BARCODE_ID_QR_CODE, 's'},
		{BARCODE_ID_IATA_2_OF_5, 'f'},
		{BARCODE_ID_TELEPEN, 't'},
		{BARCODE_ID_UNKNOWN , 0}
	};

	/**
	 * Implements a call back method for barcode data
	 */
	public void AccessDataBarcodeRx(char BarcodeID, ByteBuffer dataReceived, int dataReceivedLen);
}
