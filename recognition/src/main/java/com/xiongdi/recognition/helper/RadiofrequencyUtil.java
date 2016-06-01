package com.xiongdi.recognition.helper;

import android.util.Log;

import com.xiongdi.EmpPad;

/**
 * Created by moubiao on 2016/5/16.
 * 测试射频的工具类
 */
public class RadioFrequencyUtil {
    private static String TAG = "moubiao";

    /**
     * 打开射频模块
     */
    public static boolean openRFModel() {
        if (0 != EmpPad.RFIDModuleOpen()) {
            Log.e(TAG, "openRFModel: failed!");
            return false;
        }
        return true;
    }

    /**
     * 关闭射频模块
     */
    public static boolean closeRFModel() {
        if (0 != EmpPad.RFIDMoudleClose()) {
            Log.e(TAG, "close RFModel: failed!");
            return false;
        }
        return true;
    }

    /**
     * 选择天线
     *
     * @param aerialIndex 天线编号
     */
    public static boolean chooseAerial(int aerialIndex) {
        if (0 != EmpPad.SelectRFIDSlot(aerialIndex)) {
            Log.e(TAG, "choose aerial failed!");

            return false;
        }
        return true;
    }

    /**
     * 初始化射频模块
     *
     * @param aerialIndex 天线编号
     */
    public static boolean initRFModel(int aerialIndex) {
        if (0 != EmpPad.Rf_Init(aerialIndex)) {
            Log.e(TAG, "init RFModel failed!");
            return false;
        }
        return true;
    }

    /**
     * 打开或关闭射频信号
     */
    public static boolean openOrCloseRFSignal(int open) {
        if (0 == EmpPad.Rf_OnOff(open)) {
            return true;
        } else {
            if (0 == open) {
                Log.e(TAG, "close RF signal: failed!");
            } else {
                Log.e(TAG, "open RF signal: failed!");
            }
            return false;
        }
    }

    /**
     * 获取卡的序列号
     *
     * @param mode   模式
     * @param serLen 序列号的长度
     * @param PUID   保存序列号
     */
    public static boolean getSerialNumber(int mode, byte[] serLen, byte[] PUID) {
        if (0 != EmpPad.Rfa_GetSNR(mode, serLen, PUID)) {
            Log.e(TAG, "get serialNumber failed");
            return false;
        }
        return true;
    }

    /**
     * 复位Cpu卡
     */
    public static boolean resetCpuCard(byte[] resp) {
        if (0 != EmpPad.Rfa_RATS(resp)) {
            Log.e(TAG, "resetCpuCard: failed!");
            return false;
        }
        return true;
    }

    /**
     * 向cpu卡发送apdu指令
     *
     * @param send    指令
     * @param len     指令长度
     * @param outData 接收数据
     * @param outLen  接收数据长度
     */
    public static boolean sendApdu(byte[] send, int len, byte[] outData, short[] outLen) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "sendApdu: send apdu start----->");
        if (0 != EmpPad.Rfa_APDU(send, len, outData, outLen)) {
            Log.d(TAG, "sendApdu: send apdu failed end time = " + (System.currentTimeMillis() - startTime));
            Log.e(TAG, "sendApdu: failed!");
            return false;
        }
        Log.d(TAG, "sendApdu: send apdu success end time = " + (System.currentTimeMillis() - startTime));
        return true;
    }

    /**
     * 认证M1卡
     */
    public static boolean authenticateM1Card(byte cKeyab, byte cSecotrNo, byte[] pKey, byte[] pSNR) {
        if (0 != EmpPad.Rfmif_Authen(cKeyab, cSecotrNo, pKey, pSNR)) {
            Log.e(TAG, "authenticate sector index " + cSecotrNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 写M1卡
     */
    public static boolean writeM1Card(byte cBlockNo, byte[] pWrData) {
        if (0 != EmpPad.Rfmif_Write(cBlockNo, pWrData)) {
            Log.e(TAG, "Write  block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 读M1卡
     */
    public static boolean readM1Card(byte cBlockNo, byte[] pRdData) {
        if (0 != EmpPad.Rfmif_Read(cBlockNo, pRdData)) {
            Log.e(TAG, "Read block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 写M1卡值块
     */
    public static boolean writeValueM1Card(byte cBlockNo, byte[] pWrData) {
        if (0 != EmpPad.Rfmif_WriteValue(cBlockNo, pWrData)) {
            Log.e(TAG, "Write value  block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 读M1卡值块
     */
    public static boolean readValueM1Card(byte cBlockNo, byte[] pRdData) {
        if (0 != EmpPad.Rfmif_ReadValue(cBlockNo, pRdData)) {
            Log.e(TAG, "Read value block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 对M1卡加值保存数据
     */
    public static boolean incWriteValueM1Card(byte bSrcBlock, byte bDstBlock, byte[] bValue) {
        if (0 != EmpPad.Rfmif_IncTransfer(bSrcBlock, bDstBlock, bValue)) {
            Log.e(TAG, "increase value write block index source =  " + bSrcBlock + " des block index = " + bDstBlock + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 对M1卡减值保存数据
     */
    public static boolean decWriteValueM1Card(byte bSrcBlock, byte bDstBlock, byte[] bValue) {
        if (0 != EmpPad.Rfmif_DecrementTransfer(bSrcBlock, bDstBlock, bValue)) {
            Log.e(TAG, "decrease value write block index source =  " + bSrcBlock + " des block index = " + bDstBlock + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 对卡的恢复并且传输保存指令
     */
    public static boolean restoreTransferM1Card(byte bSrcBlock, byte bDstBlock) {
        if (0 != EmpPad.Rfmif_RestoreTransfer(bSrcBlock, bDstBlock)) {
            Log.e(TAG, "restore transfer value write block index source =  " + bSrcBlock + " des block index = " + bDstBlock + " failed!");
            return false;
        }
        return true;
    }
}
