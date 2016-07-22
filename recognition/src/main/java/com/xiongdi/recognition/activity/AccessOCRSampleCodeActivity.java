package com.xiongdi.recognition.activity;

/* AccessOCRSampleCodeActivity
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
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.accessltd.device.AccessBarcodeDataListener;
import com.accessltd.device.AccessDeviceStatusListener;
import com.accessltd.device.AccessHIDInterface;
import com.accessltd.device.AccessMSRDataListener;
import com.accessltd.device.AccessOCRDataListener;
import com.accessltd.device.AccessParserNDKInterface;
import com.xiongdi.recognition.R;

import java.nio.ByteBuffer;

//import android.content.Intent;

public class AccessOCRSampleCodeActivity extends Activity implements AccessDeviceStatusListener, AccessBarcodeDataListener,
        AccessMSRDataListener, AccessOCRDataListener {
    private static final String TAG = "AccessOCRSampleActivity";

    private AccessHIDInterface hidDevice = null;

    public static String newline = System.getProperty("line.separator");

    private AccessParserNDKInterface accessParserNDKInterface = new AccessParserNDKInterface();

    private final String deviceConnectInstruction = "Connect a device to continue";
    private final String deviceMSROCRInstruction = "Swipe a card";
    private final String deviceBarcodeInstruction = "Scan a barcode";

    private boolean validateOCR = true;
    private boolean validateMSR = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView deviceInstruction = (TextView) findViewById(R.id.textViewInstruction);
        deviceInstruction.setText(deviceConnectInstruction);

        hidDevice = new AccessHIDInterface(this);
        hidDevice.setOnBarcodeRxDataListener(this);
        hidDevice.setOnMSRRxDataListener(this);
        hidDevice.setOnOCRRxDataListener(this);
        hidDevice.setDeviceStatusListener(this);
        hidDevice.setBarcodeReadingEnabled(true);
        hidDevice.setMSRReadingEnabled(true);
        hidDevice.setOCRReadingEnabled(true);
    }

    //these methods handle the HID device state when the status of the android device changes
    @Override
    public void onPause() {
        super.onPause();
        if (hidDevice != null) {
            hidDevice.Stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hidDevice != null) {
            hidDevice.ReStart();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (hidDevice != null) {
            hidDevice.Stop();
            hidDevice = null;
        }
    }

    @Override
    public void AccessDeviceConnected(char DeviceType, String VendorId, String ProductId) {
        // Change the displayed instruction to match the device type
        TextView deviceInstruction = (TextView) findViewById(R.id.textViewInstruction);
        String newInstruction = "";
        if (DeviceType == AccessDeviceStatusListener.DEVICE_TYPE_BARCODE) {
            newInstruction = deviceBarcodeInstruction;
        } else if ((DeviceType & AccessDeviceStatusListener.DEVICE_TYPE_MSR_OCR) > 0) {
            newInstruction = deviceMSROCRInstruction;
        }
        deviceInstruction.setText(newInstruction);

        Toast.makeText(this, "Device Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void AccessDeviceDisconnected() {
        // Change the displayed instruction to indicate that a device must be connected
        TextView deviceInstruction = (TextView) findViewById(R.id.textViewInstruction);
        deviceInstruction.setText(deviceConnectInstruction);

        clearDisplayText();

        Toast.makeText(this, "Device Disconnected", Toast.LENGTH_SHORT).show();
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
        setDisplayText(output);
    }

    @Override
    public void AccessDataMSRRx(char CardType, ByteBuffer dataReceived, int dataReceivedLen) {
        // Not used in this demo
    }

    //parsing MSR is not currently supported, however this method will allow it once supported
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
                setDisplayText(output + newline + newline + "Parse Error: " + parseErrorString);
            } else if (resultString.length() > 0) {
                setDisplayText(output + newline + newline + resultString);
            }
        }
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

        setDisplayText(output);
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
                    setDisplayText(output + newline + newline + parseErrorString);
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
                setDisplayText(output + newline + newline + resultString);
            }
        } else {
            setDisplayText(output + newline + newline + "Validation Failed");
        }
    }

    @Override
    public void AccessDataOCRRx(ByteBuffer dataReceived, int dataReceivedLen) {
        // Not used in this demo
    }


    private void clearDisplayText() {
        TextView deviceType = (TextView) findViewById(R.id.textViewDisplayData);
        deviceType.setText("");
    }

    private void setDisplayText(String displayText) {
        TextView deviceType = (TextView) findViewById(R.id.textViewDisplayData);
        deviceType.setText(displayText);
    }
}