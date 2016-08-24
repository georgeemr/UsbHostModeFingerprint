package com.xiongdi.recognition.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.xiongdi.OpenJpeg;
import com.xiongdi.natives.EmpPad;
import com.xiongdi.recognition.application.App;
import com.xiongdi.recognition.constant.PictureConstant;
import com.xiongdi.recognition.util.Converter;
import com.xiongdi.recognition.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by moubiao on 2016/5/16.
 * 测试射频的工具类
 */
public class OperateCardHelper {
    private static String TAG = "moubiao";
    private static boolean hasOutAerial = false;
    private static int BLOCK_LENGTH = 16;

    private Context mContext;
    private String IDCard;
    private String nameCard;
    private String genderCard;
    private String birthdayCard;
    private String addressCard;
    private String IDNOCard;
    private String imgUrlCard;
    private String fingerUrlCard;
    private Bitmap cardImg;

    public OperateCardHelper(Context context) {
        mContext = context;
    }

    /**
     * 打开射频模块
     */
    public boolean openRFModel() {
        if (0 != EmpPad.RFIDModuleOpen()) {
            Log.e(TAG, "openRFModel: failed!");
            return false;
        }
        return true;
    }

    /**
     * 关闭射频模块
     */
    public boolean closeRFModel() {
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
    public boolean chooseAerial(int aerialIndex) {
        if (!hasOutAerial) {
            return true;
        }
//        if (0 != EmpPad.SelectRFIDSlot(aerialIndex)) {
//            Log.e(TAG, "choose aerial failed!");
//
//            return false;
//        }
        return true;
    }

    /**
     * 初始化射频模块
     *
     * @param aerialIndex 天线编号
     */
    public boolean initRFModel(int aerialIndex) {
        if (!hasOutAerial) {
            return true;
        }
//        if (0 != EmpPad.Rf_Init(aerialIndex)) {
//            Log.e(TAG, "init RFModel failed!");
//            return false;
//        }
        return true;
    }

    /**
     * 打开或关闭射频信号
     */
    public boolean openOrCloseRFSignal(int open) {
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
    public boolean getSerialNumber(int mode, byte[] serLen, byte[] PUID) {
        if (0 != EmpPad.Rfa_GetSNR(mode, serLen, PUID)) {
            Log.e(TAG, "get serialNumber failed");
            return false;
        }
        return true;
    }

    /**
     * 复位Cpu卡
     */
    public boolean resetCpuCard(byte[] resp, byte[] length) {
        if (0 != EmpPad.Rfa_RATS(resp, length)) {
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
    public boolean sendApdu(byte[] send, int len, byte[] outData, short[] outLen) {
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
    public boolean authenticateM1Card(byte cKeyab, byte cSecotrNo, byte[] pKey, byte[] pSNR) {
        if (0 != EmpPad.Rfmif_Authen(cKeyab, cSecotrNo, pKey, pSNR)) {
            Log.e(TAG, "authenticate sector index " + cSecotrNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 写M1卡
     */
    public boolean writeM1Card(byte cBlockNo, byte[] pWrData) {
        if (0 != EmpPad.Rfmif_Write(cBlockNo, pWrData)) {
            Log.e(TAG, "Write  block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 读M1卡
     */
    public boolean readM1Card(byte cBlockNo, byte[] pRdData) {
        if (0 != EmpPad.Rfmif_Read(cBlockNo, pRdData)) {
            Log.e(TAG, "Read block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 写M1卡值块
     */
    public boolean writeValueM1Card(byte cBlockNo, byte[] pWrData) {
        if (0 != EmpPad.Rfmif_WriteValue(cBlockNo, pWrData)) {
            Log.e(TAG, "Write value  block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 读M1卡值块
     */
    public boolean readValueM1Card(byte cBlockNo, byte[] pRdData) {
        if (0 != EmpPad.Rfmif_ReadValue(cBlockNo, pRdData)) {
            Log.e(TAG, "Read value block index " + cBlockNo + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 对M1卡加值保存数据
     */
    public boolean incWriteValueM1Card(byte bSrcBlock, byte bDstBlock, byte[] bValue) {
        if (0 != EmpPad.Rfmif_IncTransfer(bSrcBlock, bDstBlock, bValue)) {
            Log.e(TAG, "increase value write block index source =  " + bSrcBlock + " des block index = " + bDstBlock + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 对M1卡减值保存数据
     */
    public boolean decWriteValueM1Card(byte bSrcBlock, byte bDstBlock, byte[] bValue) {
        if (0 != EmpPad.Rfmif_DecrementTransfer(bSrcBlock, bDstBlock, bValue)) {
            Log.e(TAG, "decrease value write block index source =  " + bSrcBlock + " des block index = " + bDstBlock + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 对卡的恢复并且传输保存指令
     */
    public boolean restoreTransferM1Card(byte bSrcBlock, byte bDstBlock) {
        if (0 != EmpPad.Rfmif_RestoreTransfer(bSrcBlock, bDstBlock)) {
            Log.e(TAG, "restore transfer value write block index source =  " + bSrcBlock + " des block index = " + bDstBlock + " failed!");
            return false;
        }
        return true;
    }

    /**
     * 读M1卡
     */
    public boolean readM1Card() {
        byte[] uidlen = new byte[1];
        byte[] pUID = new byte[256];
        byte[] Serial = new byte[4];

        chooseAerial(1);//内置天线
        initRFModel(1);
        openOrCloseRFSignal(0);
        getSerialNumber(0, uidlen, pUID);
        System.arraycopy(pUID, 0, Serial, 0, 4);

        return readCard(Serial);
    }

    int readLastBlock = -1;
    int fingerDataLength = 0;
    int pictureDataLength = 0;

    /**
     * 读卡
     * 前三个扇区存储基本数据，是使用的块是 1，2，4，5，6，8，9，10
     * 第四个扇区到第34个扇区存储指纹数据
     * 第三十五到第40扇区存储照片数序
     *
     * @param serialNo 卡的序列号
     * @return true：成功 false：失败
     */
    private boolean readCard(byte[] serialNo) {
        int sectorCount = 40;//扇区的总数量
        int blockDensity = 4;//每个扇区的块数
        int ret;//验证卡，读卡的返回值
        byte[] bKey = new byte[6];
        byte[] bOutData = new byte[16];
        int dummySectorIndex;
        int blockIndex;
        byte[] M1Id = new byte[4];
        System.arraycopy(serialNo, 0, M1Id, 0, 4);
        StringBuilder totalData = new StringBuilder();//读出来的所有数据

        // S50 的卡, 16 扇区;  S70的卡, 40扇区
        for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
            Arrays.fill(bKey, (byte) 0xFF);
            if (sectorIndex > 31) {
                dummySectorIndex = 32 + (sectorIndex - 32) * 4;
                ret = EmpPad.Rfmif_Authen((byte) 0x0A, (byte) dummySectorIndex, bKey, M1Id);
                blockDensity = 16;//如果是后八个扇区则每个扇区里有16个块
            } else {
                ret = EmpPad.Rfmif_Authen((byte) 0x0A, (byte) sectorIndex, bKey, M1Id);
            }

            if (ret != 0) {
                Log.e(TAG, "readCard: authenticate failed sector index = " + sectorIndex);
                return false;
            }

            //读扇区里的所有块
            for (int j = 0; j < blockDensity; j++) {
                if (sectorIndex > 31) {
                    if (15 == j) {//判断是否是密钥块，如果是怎跳过读下一个块
                        continue;
                    }

                    blockIndex = 32 * 4 + (sectorIndex - 32) * blockDensity + j;
                } else {
                    if (0 == sectorIndex && 0 == j) {//第0扇区的第0块是厂家信息，不用读
                        continue;
                    }
                    if (3 == j) {//判断是否是密钥块，如果是怎跳过读下一个块
                        continue;
                    }

                    blockIndex = (sectorIndex * blockDensity + j);
                }

                //判断是否读完了有效数据
                if (readLastBlock != -1 && blockIndex != 12 && blockIndex != 160 && blockIndex >= readLastBlock) {
                    continue;
                }

                if (EmpPad.Rfmif_Read((byte) blockIndex, bOutData) != 0) {
                    Log.e(TAG, "readCard: read failed block index = " + blockIndex);
                    return false;
                }

                //判断指纹和照片的存储长度，从第十二个块开始存储指纹，从第160个块开始存储照片
                if (12 == blockIndex || 160 == blockIndex) {
                    final int MAX_LENGTH = 1392;//前32块里面最多存储指纹的长度
                    final int SECTOR_32_START = 128;//第32扇区第一个块的索引数
                    byte[] fileLength = new byte[2];
                    System.arraycopy(bOutData, 0, fileLength, 0, fileLength.length);
                    short length = Converter.byteArray2Short(fileLength);//指纹或照片的长度
                    int realPlaceBlock;//实际数据占用的块数
                    int keyBlockCount;//存储密钥的块数
                    if (sectorIndex > 31) {
                        realPlaceBlock = (int) Math.ceil((length + 2) / 16.0);//16.0每个块的字节数
                        keyBlockCount = (int) Math.floor(realPlaceBlock / 15.0);//15.0每个扇区可以存储数据的块数
                        readLastBlock = realPlaceBlock + keyBlockCount + blockIndex;
                    } else {
                        if ((length + 2) <= MAX_LENGTH) {
                            realPlaceBlock = (int) Math.ceil((length + 2) / 16.0);
                            keyBlockCount = (int) Math.floor(realPlaceBlock / 3.0);
                            readLastBlock = realPlaceBlock + keyBlockCount + blockIndex;
                        } else {
                            int extraLength = length - MAX_LENGTH;//在第32块及32以上的块存储的长度
                            realPlaceBlock = (int) Math.ceil(extraLength / 16.0);
                            keyBlockCount = (int) Math.floor(realPlaceBlock / 15.0);
                            readLastBlock = realPlaceBlock + keyBlockCount + SECTOR_32_START;
                        }
                    }

                    if (12 == blockIndex) {
                        fingerDataLength = length;
                    } else {
                        pictureDataLength = length;
                    }
                    Log.d(TAG, "readCard: block index = " + blockIndex + " data length = " + length + " use block count = " + realPlaceBlock
                            + " read last block = " + readLastBlock);
                }

                //将读到的byte数据转换成hexString
                totalData.append(Converter.hex2String(bOutData, bOutData.length));
                totalData.append("\n");
            }
        }

        return saveDataToFile(totalData.toString());
    }

    /**
     * 将从卡里的读出来的数据保存到文件里
     */
    private boolean saveDataToFile(String dataStr) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "saveDataToFile: external storage not mounted!");
            return false;
        }

        String dirPath = mContext.getExternalFilesDir(null) + File.separator + "card";
        File directory = new File(dirPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(TAG, "readCard: create card directory failed!");
                return false;
            }
        }
        File cardBin = new File(directory, "card.bin");
        if (!cardBin.exists()) {
            try {
                if (!cardBin.createNewFile()) {
                    Log.e(TAG, "readCard: create card.bin file failed!");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(cardBin);
            bw = new BufferedWriter(fw);
            bw.write(dataStr);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return readCardBin();
    }

    /**
     * 从card.bin里读数据
     */
    private boolean readCardBin() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "saveDataToFile: external storage not mounted!");
            return false;
        }

        String dirPath = mContext.getExternalFilesDir(null) + File.separator + "card";
        File directory = new File(dirPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(TAG, "readCard: create card directory failed!");
                return false;
            }
        }
        File cardBin = new File(directory, "card.bin");
        if (!cardBin.exists()) {
            try {
                if (!cardBin.createNewFile()) {
                    Log.e(TAG, "readCard: create card.bin file failed!");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //读基本信息
        byte[] readBaseData = new byte[128];
        Arrays.fill(readBaseData, (byte) 0x00);
        //读指纹信息
        int fingerBlockCount = (int) Math.ceil((fingerDataLength + 2) / 16.0);
        byte[] readFingerData = new byte[fingerBlockCount * 16];
        Arrays.fill(readFingerData, (byte) 0x00);
        //读照片信息
        int picBlockCount = (int) Math.ceil((pictureDataLength + 2) / 16.0);
        byte[] head = PictureConstant.JP2HEAD;
        byte[] pictureData = new byte[picBlockCount * 16 + head.length];//byte[] pictureData = new byte[1648]
        Arrays.fill(pictureData, (byte) 0x00);
        System.arraycopy(head, 0, pictureData, 0, head.length);

        //读card.bin文件
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(cardBin);
            br = new BufferedReader(fr);
            byte[] temp;
            String readStr;
            int rowCount = 0;
            while ((readStr = br.readLine()) != null) {
                temp = Converter.string2Hex(readStr);
                if (rowCount < 8) {//前八行是基本信息
                    System.arraycopy(temp, 0, readBaseData, rowCount * 16, temp.length);
                } else if (rowCount > 7 && rowCount < 8 + fingerBlockCount) {//指纹信息
                    System.arraycopy(temp, 0, readFingerData, (rowCount - 8) * 16, temp.length);
                } else if (rowCount >= 8 + fingerBlockCount) {//照片信息
                    if (rowCount == 8 + fingerBlockCount) {
                        System.arraycopy(temp, 2, pictureData, (rowCount - 8 - fingerBlockCount) * 16 + 208, temp.length - 2);
                    } else {
                        System.arraycopy(temp, 0, pictureData, (rowCount - 8 - fingerBlockCount) * 16 + 208 - 2, temp.length);
                    }
                }

                rowCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return readBaseInformation(readBaseData) && readFingerprint(readFingerData) && readPicture(pictureData);
    }

    /**
     * 读取基本数据
     */
    private boolean readBaseInformation(byte[] readBaseData) {
        byte[] IDDataRead = new byte[5];
        byte[] nameDataRead = new byte[16];
        byte[] genderDataRead = new byte[6];
        byte[] birthdayDataRead = new byte[10];
        byte[] addressDataRead = new byte[60];
        byte[] IDNODataRead = new byte[5];
        System.arraycopy(readBaseData, 0, IDDataRead, 0, 5);//ID
        System.arraycopy(readBaseData, 5, nameDataRead, 0, 16);//name
        System.arraycopy(readBaseData, 21, genderDataRead, 0, 6);//gender
        System.arraycopy(readBaseData, 27, birthdayDataRead, 0, 10);//birthday
        System.arraycopy(readBaseData, 37, addressDataRead, 0, 60);//address
        System.arraycopy(readBaseData, 97, IDNODataRead, 0, 5);//IDNO

        byte[] finNameData = null;
        for (int i = (nameDataRead.length - 1); i >= 0; i--) {
            if (nameDataRead[i] != 0) {
                finNameData = new byte[i + 1];
                System.arraycopy(nameDataRead, 0, finNameData, 0, i + 1);
                break;
            }

        }

        byte[] finGenderData = null;
        for (int i = (genderDataRead.length - 1); i >= 0; i--) {
            if (genderDataRead[i] != 0) {
                finGenderData = new byte[i + 1];
                System.arraycopy(genderDataRead, 0, finGenderData, 0, i + 1);
                break;
            }

        }

        byte[] finAddressData = null;
        for (int i = (addressDataRead.length - 1); i >= 0; i--) {
            if (addressDataRead[i] != 0) {
                finAddressData = new byte[i + 1];
                System.arraycopy(addressDataRead, 0, finAddressData, 0, i + 1);
                break;
            }
        }

        IDCard = new String(IDDataRead);
        if (finNameData != null) {
            nameCard = new String(finNameData);
        } else {
            nameCard = "";
        }
        if (finGenderData != null) {
            genderCard = new String(finGenderData);
        } else {
            genderCard = "";
        }
        birthdayCard = new String(birthdayDataRead);
        if (finAddressData != null) {
            addressCard = new String(finAddressData);
        } else {
            addressCard = "";
        }
        IDNOCard = new String(IDNODataRead);

        Log.d(TAG, "read base information success!");
        return true;
    }

    /**
     * 读指纹
     */
    private boolean readFingerprint(byte[] readFingerData) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.d(TAG, "external storage mounted failed!");
            return false;
        }

        byte[] validFingerData = new byte[readFingerData.length - 2];
        System.arraycopy(readFingerData, 2, validFingerData, 0, validFingerData.length);

        FileUtil fileUtil = new FileUtil();
        String filePath = mContext.getExternalFilesDir("card") + File.separator + "cardFingerprint";
        File fingerFile = fileUtil.createFile(filePath);

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(fingerFile);
            bos = new BufferedOutputStream(fos);
            bos.write(validFingerData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (null != fos) {
                    fos.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            String[] scanFile = new String[]{
                    mContext.getExternalFilesDir("card") + File.separator + "cardFingerprint"
            };
            MediaScannerConnection.scanFile(mContext, scanFile, null, null);
            App.FINGERPRINT_PATH = fingerFile.getPath();
        }

        Log.d(TAG, "read fingerprint information success!");
        return true;
    }

    /**
     * 读取卡里的照片
     */
    private boolean readPicture(byte[] picData) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.d(TAG, "external storage mounted failed!");
            return false;
        }

        byte[] validPicData = null;
        for (int i = (picData.length - 1); i >= 0; i--) {
            if (picData[i] != 0) {
                validPicData = new byte[i + 1];
                System.arraycopy(picData, 0, validPicData, 0, i + 1);
                break;
            }
        }

        OpenJpeg.GetLibVersion();
        FileOutputStream fos = null;
        try {
            File directory = mContext.getExternalFilesDir("card");
            if (directory != null && !directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "readPicture: create card directory failed");
                    return false;
                }
            }
            if (directory != null) {
                String filePath = directory.getPath() + "/decodePic.jp2";
                String decompressPath = directory.getPath() + "/decodePic.png";
                fos = new FileOutputStream(filePath);
                if (validPicData != null) {
                    fos.write(validPicData);
                    fos.flush();
                    if (0 == OpenJpeg.DecompressImage(filePath, decompressPath)) {
                        cardImg = BitmapFactory.decodeFile(decompressPath);
                        new FileUtil().deleteFile(filePath);
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d(TAG, "read picture information success!");
        return true;
    }

    public String[] getBaseData() {
        return new String[]{
                IDCard,
                nameCard,
                genderCard,
                birthdayCard,
                addressCard,
                IDNOCard,
                fingerUrlCard
        };
    }

    public Bitmap getPicture() {
        return cardImg;
    }

    public void setSaveData(String[] saveData) {
        IDCard = saveData[0];
        nameCard = saveData[1];
        genderCard = saveData[2];
        birthdayCard = saveData[3];
        addressCard = saveData[4];
        IDNOCard = saveData[5];
        imgUrlCard = saveData[6];
        fingerUrlCard = saveData[7];
    }

    /**
     * 写M1卡(0-2扇区写基本数据，3-33扇区写指纹，34-39扇区写照片)
     */
    public boolean writeM1Card() {
        byte[] uidlen = new byte[1];
        byte[] pUID = new byte[256];
        byte[] Serial = new byte[4];

        chooseAerial(1);//内置天线
        initRFModel(1);
        openOrCloseRFSignal(0);
        getSerialNumber(0, uidlen, pUID);
        System.arraycopy(pUID, 0, Serial, 0, 4);

        //写基本信息数据
        if (!writeBaseData(Serial)) {
            return false;
        }
        //写指纹
        if (fingerUrlCard != null) {
            if (!writeFingerprint(Serial)) {
                return false;
            }
        }
        //写照片
        if (imgUrlCard != null) {
            if (!writePicture(Serial)) {
                return false;
            }
        }

        return true;
    }

    private boolean writeBaseData(byte[] serialNo) {
        byte[] writeBaseData = new byte[128];
        Arrays.fill(writeBaseData, (byte) 0x00);
        System.arraycopy(IDCard.getBytes(), 0, writeBaseData, 0, IDCard.getBytes().length);
        System.arraycopy(nameCard.getBytes(), 0, writeBaseData, 5, nameCard.getBytes().length);
        System.arraycopy(genderCard.getBytes(), 0, writeBaseData, 21, genderCard.getBytes().length);
        System.arraycopy(birthdayCard.getBytes(), 0, writeBaseData, 27, birthdayCard.getBytes().length);
        System.arraycopy(addressCard.getBytes(), 0, writeBaseData, 37, addressCard.getBytes().length);
        System.arraycopy(IDNOCard.getBytes(), 0, writeBaseData, 97, IDNOCard.getBytes().length);

        if (!loopWrite(serialNo, writeBaseData, 0, 3)) {
            return false;
        }

        Log.d(TAG, "write base data success!");
        return true;
    }

    /**
     * 写指纹
     */
    private boolean writeFingerprint(byte[] serialNo) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }

        final int FINGER_MAX_DATA = 1904;//可以保存的指纹数据的最大值
        File file = new File(fingerUrlCard);
        if (!file.exists()) {
            return false;
        }

        byte[] writeFingerData = new byte[FINGER_MAX_DATA];
        Arrays.fill(writeFingerData, (byte) 0x00);
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        try {
            fis = new FileInputStream(file);
            baos = new ByteArrayOutputStream(FINGER_MAX_DATA);
            byte[] temp = new byte[FINGER_MAX_DATA];
            int n;
            while ((n = fis.read(temp)) != -1) {
                baos.write(temp, 0, n);
            }

            byte[] validData = baos.toByteArray();
            int fingerLength = validData.length;
            if (fingerLength > FINGER_MAX_DATA - 2) {
                Log.d(TAG, "finger data too large!");
                return false;
            }
            byte[] saveLength = Converter.short2ByteArray((short) fingerLength);
            System.arraycopy(saveLength, 0, writeFingerData, 0, saveLength.length);
            System.arraycopy(validData, 0, writeFingerData, saveLength.length, fingerLength);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (!loopWrite(serialNo, writeFingerData, 3, 34)) {
            return false;
        }


        Log.d(TAG, "write fingerprint success!");
        return true;
    }

    /**
     * 写照片
     */
    private boolean writePicture(byte[] serialNo) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }

        File file = new File(imgUrlCard);
        if (!file.exists()) {
            return false;
        }

        int fileLen = (int) file.length();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(fileLen);
        FileInputStream fis;
        byte[] readData = new byte[fileLen];
        int readLen;
        try {
            fis = new FileInputStream(file);
            while ((readLen = fis.read(readData)) != -1) {
                bos.write(readData, 0, readLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final int HEAD_LENGTH = 208;//图片头的长度
        final int PICTURE_MAX_LENGTH = 1440;//去掉头后图片的最大长度
        final int TEMP = 2;//保存图片长度需要的字节数
        byte[] bitmapData = bos.toByteArray();
        int length = bitmapData.length - HEAD_LENGTH;//除去头的长度
        if (length > PICTURE_MAX_LENGTH - TEMP) {
            Log.d(TAG, "picture too large! current is " + length + " limit is " + PICTURE_MAX_LENGTH);
            return false;
        }

        byte[] lengthData = Converter.short2ByteArray((short) length);
        byte[] writePicData = new byte[PICTURE_MAX_LENGTH];
        Arrays.fill(writePicData, (byte) 0x00);
        System.arraycopy(lengthData, 0, writePicData, 0, lengthData.length);
        System.arraycopy(bitmapData, HEAD_LENGTH, writePicData, lengthData.length, length);

        if (!loopWrite(serialNo, writePicData, 34, 40)) {
            return false;
        }

        Log.d(TAG, "write picture success!");
        return true;
    }

    /**
     * 循环向卡里写入数据
     *
     * @param serialNo         卡的序列号
     * @param srcData          要写入的数据
     * @param startSectorIndex 开始的扇区号
     * @param endSectorIndex   结束的扇区号
     * @return
     */
    private boolean loopWrite(byte[] serialNo, byte[] srcData, int startSectorIndex, int endSectorIndex) {
        int blockDensity = 4;//每个扇区的块数
        int ret;//验证卡，读卡的返回值
        byte[] bKey = new byte[6];
        int dummySectorIndex;
        int blockIndex;
        byte[] M1Id = new byte[4];
        System.arraycopy(serialNo, 0, M1Id, 0, 4);

        // S50 的卡, 16 扇区;  S70的卡, 40扇区
        int writeBaseOffset = 0;
        for (int sectorIndex = startSectorIndex; sectorIndex < endSectorIndex; sectorIndex++) {
            //验证扇区
            Arrays.fill(bKey, (byte) 0xFF);
            if (sectorIndex > 31) {
                dummySectorIndex = 32 + (sectorIndex - 32) * 4;
                ret = EmpPad.Rfmif_Authen((byte) 0x0A, (byte) dummySectorIndex, bKey, M1Id);
                blockDensity = 16;//如果是后八个扇区则每个扇区里有16个块
            } else {
                ret = EmpPad.Rfmif_Authen((byte) 0x0A, (byte) sectorIndex, bKey, M1Id);
            }

            if (ret != 0) {
                Log.e(TAG, "loopWrite: authenticate failed sector index = " + sectorIndex + " return code = " + ret);
                return false;
            }

            //向扇区里的数据块写数据
            for (int j = 0; j < blockDensity; j++) {
                if (sectorIndex > 31) {
                    if (15 == j) {//判断是否是密钥块，如果是则跳过读下一个块
                        continue;
                    }

                    blockIndex = 32 * 4 + (sectorIndex - 32) * blockDensity + j;
                } else {
                    if (0 == sectorIndex && 0 == j) {//第0扇区的第0块是厂家信息，不能写
                        continue;
                    }
                    if (3 == j) {//判断是否是密钥块，如果是怎跳过读下一个块
                        continue;
                    }

                    blockIndex = (sectorIndex * blockDensity + j);
                }
                //写数据
                Log.d(TAG, "loopWrite: sectorIndex = " + sectorIndex + " blockIndex = " + blockIndex
                        + " writeBaseOffset = " + writeBaseOffset + " offset = " + writeBaseOffset * BLOCK_LENGTH
                        + " data size = " + srcData.length);
                if (!writeBlock(srcData, writeBaseOffset * BLOCK_LENGTH, blockIndex)) {
                    return false;
                }

                writeBaseOffset++;
            }
        }

        return true;
    }

    /**
     * 将数据写到块里去
     *
     * @param srcData   要写的数据
     * @param srcOffset 数据偏移量
     * @param block     要写的块
     */
    private boolean writeBlock(byte[] srcData, int srcOffset, int block) {
        byte[] writeTemp = new byte[BLOCK_LENGTH];
        System.arraycopy(srcData, srcOffset, writeTemp, 0, BLOCK_LENGTH);
        int writeRet = EmpPad.Rfmif_Write((byte) block, writeTemp);
        if (writeRet != 0) {
            Log.e(TAG, "write block " + block + " failed!");
            return false;
        }

        return true;
    }
}
