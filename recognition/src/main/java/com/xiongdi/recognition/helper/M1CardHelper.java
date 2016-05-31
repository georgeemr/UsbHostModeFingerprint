package com.xiongdi.recognition.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.xd.Converter;
import com.xd.rfid;
import com.xiongdi.OpenJpeg;
import com.xiongdi.recognition.constant.PictureConstant;
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
 * Created by moubiao on 2016/4/7.
 * 操作M1卡的帮助类
 */
public class M1CardHelper {
    private static String TAG = "moubiao";
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

    //Mode：0 表示 ISO14443-A；1 表示 ISO14443-B；2 表示 Felica C；
    private static int RFID_PROTOCOL_A = 0;

    public M1CardHelper(Context mContext) {
        this.mContext = mContext;
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

    /**
     * 设置射频模块
     */
    public void setRFModule() {
        //打开射频模块
        if (rfid.RFIDModuleOpen() != 0) {
            Log.d(TAG, "RFID module open failed!");
            return;
        }

        //初始化射频模块
        if (rfid.RFIDInit() != 0) {
            Log.d(TAG, "RFID module initialize failed!");
        }
    }

    public void closeRFModule() {
        if (rfid.RFIDMoudleClose() != 0) {
            Log.d(TAG, "RFID module close failed!");
            closeRFModule();
        }
    }

    /**
     * 打开射频信号
     */
    public void openRFSignal() {
        int openRFSignalResult = rfid.RFIDRfOpen();
        if (openRFSignalResult != 0) {
            Log.d(TAG, "RFID signal open failed!");
            openRFSignal();
        }
    }

    /**
     * 关闭射频信号
     */
    public void closeRFSignal() {
        int closeRFSignalResult = rfid.RFIDRfClose();
        if (closeRFSignalResult != 0) {
            closeRFSignal();
        }
    }

    /**
     * 设置射频协议
     *
     * @return true:设置成功；false：设置失败
     */
    private boolean setRFIDProtocol() {
        if (rfid.RFIDTypeSet(RFID_PROTOCOL_A) != 0) {
            Log.d(TAG, "RFID protocol set failed!");
            return false;
        }

        return true;
    }

    /**
     * 认证扇区
     *
     * @param sectorNo 认证的扇区
     * @param serialNo 卡的序列号
     * @return true：认证成功；false：认证失败
     */
    private boolean authenticateCard(int sectorNo, byte[] serialNo) {
        byte[] key = new byte[6];
        Arrays.fill(key, (byte) 0xFF);
        byte[] ID = new byte[4];
        ID[0] = serialNo[0];
        ID[1] = serialNo[1];
        ID[2] = serialNo[2];
        ID[3] = serialNo[3];
        int verifyRet = rfid.MifAuthen((byte) 0x0A, (byte) sectorNo, key, ID);
        if (verifyRet != 0) {
            Log.d(TAG, "authenticateCard sector " + sectorNo + " failed!");
            return false;
        }

        return true;
    }

    /**
     * 读M1卡
     */
    public boolean readM1Card() {
        //设置射频协议
        if (!setRFIDProtocol()) {
            return false;
        }

        //获取序列号
        byte[] serialLength = new byte[1];
        byte[] serialNo = new byte[64];
        int serialRet = rfid.RFIDGetSNR(0, serialLength, serialNo);
        if (serialRet != 0) {
            Log.d(TAG, "serial number read failed!");

            return false;
        }

        return readCard(serialNo);
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
        M1Id[0] = serialNo[0];
        M1Id[1] = serialNo[1];
        M1Id[2] = serialNo[2];
        M1Id[3] = serialNo[3];
        StringBuilder totalData = new StringBuilder();//读出来的所有数据

        // S50 的卡, 16 扇区;  S70的卡, 40扇区
        for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
            Arrays.fill(bKey, (byte) 0xFF);
            if (sectorIndex > 31) {
                dummySectorIndex = 32 + (sectorIndex - 32) * 4;
                ret = rfid.MifAuthen((byte) 0x0A, (byte) dummySectorIndex, bKey, M1Id);
                blockDensity = 16;//如果是后八个扇区则每个扇区里有16个块
            } else {
                ret = rfid.MifAuthen((byte) 0x0A, (byte) sectorIndex, bKey, M1Id);
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

                if (rfid.MifRead(blockIndex, bOutData) != 0) {
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
     * 基本数据在前三扇区 0，1，2
     *
     * @return
     */
    private boolean decodeBaseData() {
        byte[] readBaseData = new byte[112];
        Arrays.fill(readBaseData, (byte) 0x00);

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

        Log.d(TAG, "read data success!");

        return true;
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

        byte[] validFingerData = null;
        for (int i = (readFingerData.length - 1); i >= 0; i--) {
            if (readFingerData[i] != 0) {
                validFingerData = new byte[i + 1];
                System.arraycopy(readFingerData, 0, validFingerData, 0, i + 1);
                break;
            }
        }

        File file = mContext.getExternalFilesDir("card");
        if (file != null && !file.exists()) {
            if (!file.mkdirs()) {
                Log.d(TAG, "create the directory of fingerprint failed!");
                return false;
            }
        }
        File fingerFile = new File(file, "cardFingerprint.xyt");
        if (!fingerFile.exists()) {
            try {
                if (!fingerFile.createNewFile()) {
                    Log.d(TAG, "create the file of fingerprint failed!");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(fingerFile);
            bos = new BufferedOutputStream(fos);
            if (validFingerData != null) {
                bos.write(validFingerData);
            }
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
                    mContext.getExternalFilesDir("card") + File.separator + "cardFingerprint.xyt"
            };
            MediaScannerConnection.scanFile(mContext, scanFile, null, null);
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

    /**
     * 读取块里的数据
     *
     * @param desData 存放数据的数组
     * @param offset  偏移量
     * @param block   块
     */
    private boolean readBlock(byte[] desData, int offset, int block) {
        byte[] readTemp = new byte[BLOCK_LENGTH];
        int readRet = rfid.MifRead(block, readTemp);
        if (readRet != 0) {
            Log.d(TAG, "read block " + block + " failed!");
            return false;
        }
        System.arraycopy(readTemp, 0, desData, offset, BLOCK_LENGTH);

        return true;
    }

    /**
     * 写M1卡
     */
    public boolean writeM1Card() {
        //设置射频协议
        if (!setRFIDProtocol()) {
            return false;
        }

        //获取序列号
        byte[] serialLength = new byte[1];
        byte[] serialNo = new byte[64];
        int serialRet = rfid.RFIDGetSNR(0, serialLength, serialNo);
        if (serialRet != 0) {
            Log.d(TAG, "serial number read failed!");
            return false;
        }

        //写基本信息数据
        if (!writeBaseData(serialNo)) {
            return false;
        }
        //写指纹
        if (fingerUrlCard != null) {
            if (!writeFingerprint(serialNo)) {
                return false;
            }
        }
        //写照片
        if (imgUrlCard != null) {
            if (!writePicture(serialNo)) {
                return false;
            }
        }

        return true;
    }

    private boolean writeBaseData(byte[] serialNo) {
        byte[] writeBaseData = new byte[112];
        Arrays.fill(writeBaseData, (byte) 0x00);
        System.arraycopy(IDCard.getBytes(), 0, writeBaseData, 0, IDCard.getBytes().length);
        System.arraycopy(nameCard.getBytes(), 0, writeBaseData, 5, nameCard.getBytes().length);
        System.arraycopy(genderCard.getBytes(), 0, writeBaseData, 21, genderCard.getBytes().length);
        System.arraycopy(birthdayCard.getBytes(), 0, writeBaseData, 27, birthdayCard.getBytes().length);
        System.arraycopy(addressCard.getBytes(), 0, writeBaseData, 37, addressCard.getBytes().length);
        System.arraycopy(IDNOCard.getBytes(), 0, writeBaseData, 97, IDNOCard.getBytes().length);

        byte[] baseTemp = new byte[16];
        StringBuilder baseStringBuilder = new StringBuilder();

        int writeBaseOffset = 0;
        //验证扇区0(0-3，只有1，2可以写数据)
        if (!authenticateCard(0, serialNo)) {
            return false;
        }

        for (int i = 1; i < 3; i++) {
            if (!writeBlock(writeBaseData, writeBaseOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeBaseData, writeBaseOffset * BLOCK_LENGTH, baseTemp, 0, BLOCK_LENGTH);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeBaseOffset++;
        }

        //验证扇区1
        if (!authenticateCard(1, serialNo)) {
            return false;
        }

        for (int i = 4; i < 7; i++) {
            if (!writeBlock(writeBaseData, writeBaseOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeBaseData, writeBaseOffset * BLOCK_LENGTH, baseTemp, 0, BLOCK_LENGTH);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeBaseOffset++;
        }

        //验证扇区2
        if (!authenticateCard(2, serialNo)) {
            return false;
        }
        for (int i = 8; i < 10; i++) {
            if (!writeBlock(writeBaseData, writeBaseOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeBaseData, writeBaseOffset * BLOCK_LENGTH, baseTemp, 0, BLOCK_LENGTH);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeBaseOffset++;
        }

        //将基本信息保存到文件，以备校对
        String dirPath = mContext.getExternalFilesDir(null) + File.separator + "card";
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File desFile = new File(directory, "backup.bin");
        if (!desFile.exists()) {
            try {
                desFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            fileWriter = new FileWriter(desFile);
            bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(baseStringBuilder.toString());

            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "write base data success!");
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

        byte[] baseTemp = new byte[16];
        StringBuilder baseStringBuilder = new StringBuilder();

        int writeOffset = 0;
        //验证扇区34(160-175)
        if (!authenticateCard(40, serialNo)) {
            return false;
        }
        for (int i = 160; i < 175; i++) {
            if (!writeBlock(writePicData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writePicData, writeOffset * BLOCK_LENGTH, baseTemp, 0, 16);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区35(176-191)
        if (!authenticateCard(44, serialNo)) {
            return false;
        }
        for (int i = 176; i < 191; i++) {
            if (!writeBlock(writePicData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writePicData, writeOffset * BLOCK_LENGTH, baseTemp, 0, 16);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区36(192-206)
        if (!authenticateCard(48, serialNo)) {
            return false;
        }
        for (int i = 192; i < 207; i++) {
            if (!writeBlock(writePicData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writePicData, writeOffset * BLOCK_LENGTH, baseTemp, 0, 16);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区37(208-222)
        if (!authenticateCard(52, serialNo)) {
            return false;
        }

        for (int i = 208; i < 223; i++) {
            if (!writeBlock(writePicData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writePicData, writeOffset * BLOCK_LENGTH, baseTemp, 0, 16);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区38(224-238)
        if (!authenticateCard(56, serialNo)) {
            return false;
        }

        for (int i = 224; i < 239; i++) {
            if (!writeBlock(writePicData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writePicData, writeOffset * BLOCK_LENGTH, baseTemp, 0, 16);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区39(240-254)
        if (!authenticateCard(60, serialNo)) {
            return false;
        }

        for (int i = 240; i < 255; i++) {
            if (!writeBlock(writePicData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writePicData, writeOffset * BLOCK_LENGTH, baseTemp, 0, 16);
            baseStringBuilder.append(Converter.hex2String(baseTemp, 16));
            baseStringBuilder.append("\n");

            writeOffset++;
        }

        //将基本信息保存到文件，以备校对
        String dirPath = mContext.getExternalFilesDir(null) + File.separator + "card";
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File desFile = new File(directory, "backup.bin");
        if (!desFile.exists()) {
            try {
                desFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            fileWriter = new FileWriter(desFile, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(baseStringBuilder.toString());

            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "write picture success!");
        return true;
    }

    /**
     * 写指纹
     */
    private boolean writeFingerprint(byte[] serialNo) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }

        final int FINGER_MAX_DATA = 1392;//可以保存的指纹数据的最大值
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

        byte[] fingerTemp = new byte[16];
        StringBuilder fingerBuilder = new StringBuilder();

        int writeOffset = 0;
        //验证扇区3(12-15)
        if (!authenticateCard(3, serialNo)) {
            return false;
        }
        for (int i = 12; i < 15; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区4(16-19)
        if (!authenticateCard(4, serialNo)) {
            return false;
        }

        for (int i = 16; i < 19; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区5(20-23)
        if (!authenticateCard(5, serialNo)) {
            return false;
        }

        for (int i = 20; i < 23; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区6(24-27)
        if (!authenticateCard(6, serialNo)) {
            return false;
        }

        for (int i = 24; i < 27; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区7(28-31)
        if (!authenticateCard(7, serialNo)) {
            return false;
        }
        for (int i = 28; i < 31; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区8(32-35)
        if (!authenticateCard(8, serialNo)) {
            return false;
        }

        for (int i = 32; i < 35; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区9(36-39)
        if (!authenticateCard(9, serialNo)) {
            return false;
        }

        for (int i = 36; i < 39; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区10(40-43)
        if (!authenticateCard(10, serialNo)) {
            return false;
        }

        for (int i = 40; i < 43; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区11(44-47)
        if (!authenticateCard(11, serialNo)) {
            return false;
        }
        for (int i = 44; i < 47; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区12(48-51)
        if (!authenticateCard(12, serialNo)) {
            return false;
        }

        for (int i = 48; i < 51; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区13(52-55)
        if (!authenticateCard(13, serialNo)) {
            return false;
        }

        for (int i = 52; i < 55; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区14(56-59)
        if (!authenticateCard(14, serialNo)) {
            return false;
        }

        for (int i = 56; i < 59; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区15(60-63)
        if (!authenticateCard(15, serialNo)) {
            return false;
        }
        for (int i = 60; i < 63; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区16(64-67)
        if (!authenticateCard(BLOCK_LENGTH, serialNo)) {
            return false;
        }

        for (int i = 64; i < 67; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区17(68-71)
        if (!authenticateCard(17, serialNo)) {
            return false;
        }

        for (int i = 68; i < 71; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区18(72-75)
        if (!authenticateCard(18, serialNo)) {
            return false;
        }

        for (int i = 72; i < 75; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区19(76-79)
        if (!authenticateCard(19, serialNo)) {
            return false;
        }
        for (int i = 76; i < 79; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区20(80-83)
        if (!authenticateCard(20, serialNo)) {
            return false;
        }

        for (int i = 80; i < 83; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区21(84-87)
        if (!authenticateCard(21, serialNo)) {
            return false;
        }

        for (int i = 84; i < 87; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区22(88-91)
        if (!authenticateCard(22, serialNo)) {
            return false;
        }

        for (int i = 88; i < 91; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区23(92-95)
        if (!authenticateCard(23, serialNo)) {
            return false;
        }
        for (int i = 92; i < 95; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区24(96-99)
        if (!authenticateCard(24, serialNo)) {
            return false;
        }

        for (int i = 96; i < 99; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区25(100-103)
        if (!authenticateCard(25, serialNo)) {
            return false;
        }

        for (int i = 100; i < 103; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区26(104-107)
        if (!authenticateCard(26, serialNo)) {
            return false;
        }

        for (int i = 104; i < 107; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区27(108-111)
        if (!authenticateCard(27, serialNo)) {
            return false;
        }
        for (int i = 108; i < 111; i++) {
            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
                return false;
            }
            System.arraycopy(writeFingerData, writeOffset * BLOCK_LENGTH, fingerTemp, 0, 16);
            fingerBuilder.append(Converter.hex2String(fingerTemp, 16));
            fingerBuilder.append("\n");

            writeOffset++;
        }

        //验证扇区28(112-115)
//        if (!authenticateCard(28, serialNo)) {
//            return;
//        }
//
//        for (int i = 112; i < 115; i++) {
//            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
//                return;
//            }
//
//            writeOffset++;
//        }
//
//        //验证扇区29(116-119)
//        if (!authenticateCard(29, serialNo)) {
//            return;
//        }
//
//        for (int i = 116; i < 119; i++) {
//            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
//                return;
//            }
//
//            writeOffset++;
//        }
//
//        //验证扇区30(120-123)
//        if (!authenticateCard(30, serialNo)) {
//            return;
//        }
//
//        for (int i = 120; i < 123; i++) {
//            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
//                return;
//            }
//
//            writeOffset++;
//        }
//
//        //验证扇区31(124-127)
//        if (!authenticateCard(31, serialNo)) {
//            return;
//        }
//        for (int i = 124; i < 127; i++) {
//            if (!writeBlock(writeFingerData, writeOffset * BLOCK_LENGTH, i)) {
//                return;
//            }
//
//            writeOffset++;
//        }

        //将基本信息保存到文件，以备校对
        String dirPath = mContext.getExternalFilesDir(null) + File.separator + "card";
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File desFile = new File(directory, "backup.bin");
        if (!desFile.exists()) {
            try {
                desFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            fileWriter = new FileWriter(desFile, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(fingerBuilder.toString());

            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "write fingerprint success!");

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
        int writeRet = rfid.MifWrite(block, writeTemp);
        if (writeRet != 0) {
            Log.d(TAG, "write block " + block + " failed!");

            return false;
        }

        return true;
    }
}

