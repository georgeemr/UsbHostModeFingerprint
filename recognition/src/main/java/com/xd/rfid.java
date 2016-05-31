package com.xd;

/**
 * 读写卡的接口
 */
public class rfid {
    /**
     * 打开 SAM 模块(SAM硬加密模块)
     *
     * @param sLot 卡槽编号，有效值为 1~4
     * @return 0：成功；非0：失败
     */
    public native static int samOpen(int sLot);

    /**
     * SAM卡复位
     *
     * @param samNum     卡槽编号, 1~4
     * @param samBaud    通信波特率, 可选 9600,19200,38400,115200, 是否支持某个波特率由 sam卡决定
     * @param samVol     给卡的供电电压, 1 表示 1.8v, 2 表示 3.3v, 3 表示 5.0v
     * @param echoATR    sam 卡回应的 ATR 字节数组
     * @param echoATRlen sam 卡回应的 ATR 字节长度
     * @return 0：成功；非0：失败
     */
    public native static int samReset(byte samNum, int samBaud, int samVol, byte[] echoATR, byte[] echoATRlen);

    /**
     * SAM 卡 APDU指令
     *
     * @param samNum      sam 卡槽编号, 1~4
     * @param cosCmd      APDU 指令
     * @param cosCmdLen   APDU 指令长度
     * @param cosResponse 卡回复数据
     * @param cosResLenth 卡回复数据长度
     * @param cosSW       APDU 指令执行状态码, 2 字节
     * @return 0：成功；非0：失败
     */
    public native static int samApdu(byte samNum, byte[] cosCmd, short cosCmdLen, byte[] cosResponse, short[] cosResLenth, byte[] cosSW);

    /**
     * 打开射频模块
     *
     * @return 0：成功；非0：失败
     */
    public native static int RFIDModuleOpen();


    /**
     * 关闭射频模块
     *
     * @return 0：成功；非0：失败
     */
    public native static int RFIDMoudleClose();

    /**
     * 初始化射频模块
     *
     * @return 0：成功；非0：失败
     */
    public native static int RFIDInit();

    /**
     * 设置射频协议
     *
     * @param type_mode 0 表示 ISO14443-A；1 表示 ISO14443-B；2 表示 Felica C
     * @return 0：成功；非0：失败
     */
    public native static int RFIDTypeSet(int type_mode);

    /**
     * 获取卡的序列号
     *
     * @param mode 0: 对应寻卡命令 0x26，寻未进入休眠状态的卡；1: 对应寻卡命令 0x52，寻感应区内所有符合 14443A 标准的卡；
     * @param bLen 保存序列号的长度
     * @param bSNR 保存序列号
     * @return 0：成功；非0：失败
     */
    public native static int RFIDGetSNR(int mode, byte[] bLen, byte[] bSNR);

    /**
     * CPU 卡激活
     *
     * @param cid   通常监测到单卡时，cid 为 0
     * @param bResp ATS 响应, 类似接触卡 ATR, 首字节表示长度
     * @return 成功；非0：失败
     */
    public native static int RFIDTypeARats(int cid, byte[] bResp);

    /**
     * CPU 卡 APDU指令
     *
     * @param cid      通常监测到单卡时，cid 为 0
     * @param bInData  ISO7816-4 指令数据
     * @param inLen    指令长度
     * @param bOutData 指令输出数据
     * @param bOutLen  指令输出数据长度
     * @param bSw      指令执行状态码, 2 字节
     * @return 成功；非0：失败
     */
    public native static int RFIDRfApdu(int cid, byte[] bInData, int inLen, byte[] bOutData, byte[] bOutLen, byte[] bSw);

    /**
     * Desfire 卡 APDU指令
     *
     * @param cid      通常监测到单卡时，cid 为 0
     * @param bInData  ISO7816-4 指令数据
     * @param inLen    指令长度
     * @param bOutData 指令输出数据
     * @param bOutLen  指令输出数据长度
     * @return 成功；非0：失败
     */
    public native static int RFIDDesFireApdu(int cid, byte[] bInData, int inLen, byte[] bOutData, byte[] bOutLen);

    /**
     * Felica 卡操作
     *
     * @param timeout  操作超时时间
     * @param bInData  输入的数据
     * @param inLen    输入数据长度
     * @param bOutData 输出的数据
     * @param bOutLen  输出数据长度
     * @return 成功；非0：失败
     */
    public native static int RFIDFelicaTransceive(int timeout, byte[] bInData, int inLen, byte[] bOutData, byte[] bOutLen);

    /**
     * 认证扇区
     *
     * @param keyAB 密钥的模式 0x0A (keyA 模式), 0x0B (keyB 模式)
     * @param secNo 扇区的索引， 从0开始
     * @param bKey  认证扇区的密钥, 6 字节, 白卡默认密钥 0xff 0xff 0xff 0xff 0xff 0xff
     * @param bSNR  卡的序列号
     * @return 0：成功；非0：失败
     */
    public native static int MifAuthen(byte keyAB, byte secNo, byte[] bKey, byte[] bSNR);

    /**
     * 读卡
     *
     * @param blockNo  块的序列号，从0开始
     * @param bOutData 保存的数据，16字节
     * @return 0：成功；非0：失败
     */
    public native static int MifRead(int blockNo, byte[] bOutData);

    /**
     * 写卡
     *
     * @param blockNo 块的序列号， 从0开始
     * @param bInData 要写入的数据，16字节
     * @return 0：成功；非0：失败
     */
    public native static int MifWrite(int blockNo, byte[] bInData);

    /**
     * M1 卡修改值
     *
     * @param subcommand 控制命令，完成增值、减值
     * @param blockNo    块的序列号
     * @param bInData    要写入的数据
     * @return 0：成功；非0：失败
     */
    public native static int MifChange(int subcommand, int blockNo, byte[] bInData);

    /**
     * M1 卡传输
     *
     * @param blockNo 块的序列号
     * @return 0：成功；非0：失败
     */
    public native static int MifTransfer(int blockNo);

    /**
     * B 卡 REQB 操作
     *
     * @param afi      协议参数，可以参考《ISO14443-3》
     * @param param    协议参数，可以参考《ISO14443-3》
     * @param bOutData 输出的数据
     * @param bOutLen  输出数据长度
     * @return 0：成功；非0：失败
     */
    public native static int PiccREQB(int afi, int param, byte[] bOutData, byte[] bOutLen);

    /**
     * B 卡 attrib 操作
     *
     * @param uid      卡唯一号，可以参考《ISO14443-3》
     * @param cid      卡识别号，临时有读卡器分配的卡号，可以参考《ISO14443-3》
     * @param bOutData 输出的数据
     * @param bOutLen  输出数据长度
     * @return 0：成功；非0：失败
     */
    public native static int PiccAttrib(byte[] uid, int cid, byte[] bOutData, byte[] bOutLen);

    /**
     * 打开射频信号
     *
     * @return 0：成功; 非0：失败
     */
    public native static int RFIDRfOpen();

    /**
     * 关闭射频信号
     *
     * @return 0：成功; 非0：失败
     */
    public native static int RFIDRfClose();


    //导入的lib名去掉前面的lib
    static {
        System.loadLibrary("emp5500drv");
    }
}