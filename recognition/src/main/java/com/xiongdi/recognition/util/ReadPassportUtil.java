package com.xiongdi.recognition.util;

import android.app.Activity;
import android.util.Log;

import com.accessltd.device.AccessBarcodeDataListener;
import com.accessltd.device.AccessDeviceStatusListener;
import com.accessltd.device.AccessHIDInterface;
import com.accessltd.device.AccessMSRDataListener;
import com.accessltd.device.AccessOCRDataListener;
import com.accessltd.device.AccessParserNDKInterface;

import java.nio.ByteBuffer;

/**
 * Created by moubiao on 2016/7/22.
 * 读护照的工具类
 */
public class ReadPassportUtil implements AccessDeviceStatusListener, AccessBarcodeDataListener, AccessMSRDataListener,
        AccessOCRDataListener {
    private final String TAG = "moubiao";

    private Activity mActivity;
    private AccessHIDInterface hidDevice;
    private AccessParserNDKInterface accessParserNDKInterface;

    private final String deviceConnectInstruction = "Connect a device to continue";
    private final String deviceMSROCRInstruction = "Swipe a card";
    private final String deviceBarcodeInstruction = "Scan a barcode";
    public static String newline = System.getProperty("line.separator");

    private boolean validateOCR = true;
    private boolean validateMSR = false;

    public ReadPassportUtil(Activity activity) {
        mActivity = activity;
        accessParserNDKInterface = new AccessParserNDKInterface();
        hidDevice = new AccessHIDInterface(mActivity);
        hidDevice.setOnBarcodeRxDataListener(this);
        hidDevice.setOnMSRRxDataListener(this);
        hidDevice.setOnOCRRxDataListener(this);
        hidDevice.setDeviceStatusListener(this);
        hidDevice.setBarcodeReadingEnabled(true);
        hidDevice.setMSRReadingEnabled(true);
        hidDevice.setOCRReadingEnabled(true);
    }

    private void startHidDevice() {
        if (hidDevice != null) {
            hidDevice.ReStart();
        }
    }

    private void stopHidDevice(boolean destroy) {
        if (hidDevice != null) {
            hidDevice.Stop();
            if (destroy) {
                hidDevice = null;
            }
        }
    }

    @Override
    public void AccessDataBarcodeRx(char BarcodeID, ByteBuffer dataReceived, int dataReceivedLen) {
        String output = "Barcode Data Received";
        //handle barcode symbologies - output the detected symbology to the screen
        switch (BarcodeID) {
            case BARCODE_ID_AZTEC:
                output += " (Aztec)";
                break;
            case BARCODE_ID_DATAMATRIX:
                output += " (Data Matrix)";
                break;
            case BARCODE_ID_PDF417:
                output += " (PDF417)";
                break;
            case BARCODE_ID_QR_CODE:
                output += " (QR)";
                break;
            default:
            case BARCODE_ID_AUSTRALIAN_POST:
            case BARCODE_ID_BRITISH_POST:
            case BARCODE_ID_CANADIAN_POST:
            case BARCODE_ID_CHINA_POST:
            case BARCODE_ID_CODABAR:
            case BARCODE_ID_CODE_11:
            case BARCODE_ID_CODE_128:
            case BARCODE_ID_CODE_16K:
            case BARCODE_ID_CODE_39:
            case BARCODE_ID_CODE_49:
            case BARCODE_ID_CODE_93:
            case BARCODE_ID_EAN_13:
            case BARCODE_ID_EAN_8:
            case BARCODE_ID_EAN_UCC:
            case BARCODE_ID_INTERLEAVED_2_OF_5:
            case BARCODE_ID_JAPANESE_POST:
            case BARCODE_ID_KIX_POST:
                // BARCODE_ID_KOREA_POST = '';
            case BARCODE_ID_MATRIX_2_OF_5:
            case BARCODE_ID_MAXI_CODE:
            case BARCODE_ID_MICRO_PDF417:
            case BARCODE_ID_MSI:
            case BARCODE_ID_PLANET_CODE:
            case BARCODE_ID_PLESSEY_CODE:
            case BARCODE_ID_POSI_CODE:
            case BARCODE_ID_POSTNET:
            case BARCODE_ID_IATA_2_OF_5:
            case BARCODE_ID_TELEPEN:
                break;
            case BARCODE_ID_UNKNOWN:
                output += " (Unknown)";
                break;
        }
        output += "\r\n";
        String tempString = new String(dataReceived.array());
        output += tempString;
        Log.d(TAG, "AccessDataBarcodeRx: output = " + output);
    }

    @Override
    public void AccessDeviceConnected(char DeviceType, String VendorId, String ProductId) {
        String newInstruction = "";
        if (DeviceType == AccessDeviceStatusListener.DEVICE_TYPE_BARCODE) {
            newInstruction = deviceBarcodeInstruction;
        } else if ((DeviceType & AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR) > 0) {
            newInstruction = deviceMSROCRInstruction;
        }

        Log.d(TAG, "AccessDeviceConnected: Device Connected");
    }

    @Override
    public void AccessDeviceDisconnected() {
        Log.e(TAG, "AccessDeviceDisconnected: Device disconnected");
    }

    @Override
    public void AccessDataMSRRx(char CardType, ByteBuffer dataReceived, int dataReceivedLen) {

    }

    @Override
    public void AccessDataMSRTracksRx(char CardType, ByteBuffer[] dataReceivedTrack, int[] dataReceivedLen) {
        String output = "MSR Data Received\r\nCardType: ";
        output += CardType;
        for (int Counter = 0; Counter < dataReceivedTrack.length; Counter++) {
            if (dataReceivedLen[Counter] > 0) {
                output += "\r\n";
                output += new String(dataReceivedTrack[Counter].array());
            }
        }

        if (!output.contains("*")) //only parse if output does not contain a *
        {
            String[] Track = new String[]{"", "", ""};
            for (int Counter = 0; Counter < dataReceivedTrack.length; Counter++) {
                if (dataReceivedLen[Counter] > 0) {
                    if (Counter < 3) {
                        Track[Counter] = new String(dataReceivedTrack[Counter].array());
                    }
                }
            }

            String resultString = "";
            String parseErrorString = "";
            try {
                resultString = accessParserNDKInterface.AccessHIDParseMSR(Track[0], Track[1], Track[2], validateMSR);
                if (resultString.length() == 0) {
                    parseErrorString = accessParserNDKInterface.AccessHIDParseLastError();
                }
            } catch (Exception ex) {
                Log.d(TAG, "AccessDataMSRLinesRx - Error - " + ex.toString());
                parseErrorString = "Error occurred accessing parser";
            }

            if (parseErrorString.length() > 0) {
                Log.d(TAG, "AccessDataMSRLinesRx - ParseError - " + parseErrorString);
            } else if (resultString.length() > 0) {
                Log.d(TAG, "AccessDataMSRTracksRx: " + output + newline + newline + resultString);
            }
        }
    }

    @Override
    public void AccessDataOCRRx(ByteBuffer dataReceived, int dataReceivedLen) {

    }

    @Override
    public void AccessDataOCRLinesRx(ByteBuffer[] dataReceivedLine, int[] dataReceivedLen) {
        String output = newline;
        //String output = "";
        //display the raw OCR data, line by line
        for (int Counter = 0; Counter < dataReceivedLine.length; Counter++) {
            if (dataReceivedLen[Counter] > 0) {
                output += "\r\n";
                output += new String(dataReceivedLine[Counter].array());
            }
        }

        Log.d(TAG, "AccessDataOCRLinesRx: first = " + output);
        if (!output.contains("*")) //only parse if output does not contain a *
        {
            String[] Line = new String[]{"", "", ""};
            int Counter = 0;
            //separate the received data into constituent lines
            for (Counter = 0; Counter < dataReceivedLine.length; Counter++) {
                if (dataReceivedLen[Counter] > 0) {
                    if (Counter < 3) {
                        Line[Counter] = new String(dataReceivedLine[Counter].array());
                    }
                }
            }

            //Toast.makeText(this, "Line Count: "+ Counter, Toast.LENGTH_SHORT).show();
            String resultString = "";
            String parseErrorString = "";
            String[] token = null;
            try {
                Log.d(TAG, "AccessDataOCRLinesRx - validateOCR - " + validateOCR);
                //attempt to parse the OCR data
                resultString = accessParserNDKInterface.AccessHIDParseOCR(Line[0], Line[1], Line[2], validateOCR);
                if (resultString.length() == 0) {
                    //if the parser does not return anything, display an error
                    parseErrorString = accessParserNDKInterface.AccessHIDParseLastError();
                    Log.d(TAG, "AccessDataOCRLinesRx: second = " + output + newline + newline + parseErrorString);
                }

                token = resultString.split("\n");        // Don't split on /r as all lines must exist
            } catch (Exception ex) {
                Log.d(TAG, "AccessDataOCRLinesRx - Error - " + ex.toString());
                parseErrorString = "Error occurred accessing parser";
            }

            if (parseErrorString.length() > 0) {
                Log.d(TAG, "AccessDataOCRLinesRx - ParseError - " + parseErrorString);
            } else if (resultString.length() > 0) {
                try {
                    resultString = "";
                    for (Counter = 0; Counter < OCR_PARSED_FIELD_IDS.length; Counter++) {
                        if (Counter < token.length) {
                            // End tokens that are empty are not part of the array
                            resultString += OCR_PARSED_FIELD_NAMES[Counter] + ": " + token[Counter] + newline;
                        }
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "AccessDataOCRLinesRx - Error - " + ex.toString());
                }
                //display the parsed data below the raw data
                Log.d(TAG, "AccessDataOCRLinesRx: third = " + output + newline + newline + resultString);
            }
        } else {
            Log.d(TAG, "AccessDataOCRLinesRx: forth = " + output + newline + newline + "Validation Failed");
        }
    }
}
