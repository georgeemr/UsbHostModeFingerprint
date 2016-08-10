/* 注意，请务必确保包路径为：package com.xiongdi.natives; */
/**
 * Last edit 2016-8-4, www.xiongdi.cn
 * Please make sure the package path must be "package com.xiongdi.natives;"
 */
package com.xiongdi.natives;

/**
 * 射频返回表请参考配套开发文档.
 * 1.以下说明与配套开发文档同步更新, 如有差异, 请以本文为准.
 * 2.每个需要接收数据的接口函数, 均有推荐的数组大小, 其中示意如下:
 * new byte[25]: 表示只需要25个字节.
 * new byte[25+]: 表示最少需要25个字节, 具体分配多少请开发者视实际情况而定.
 * new byte[25-]: 表示最多需要25个字节, 具体分配多少请开发者视实际情况而定.
 * 3. 如果使用线程调用本文档的函数, 请使用new thread(new runable(){...})方式调用, 而不要使用AsyncTask方式调用.
 */

/**
 * @tips If you use Thread, likely "new thread(new runable(){...}).start" was recommend,
 * "AsyncTask" was not recommend.
 */

public class EmpPad {
    /***
     * 获取jni库版本信息, 返回字符串形式的版本信息
     *
     * @return the version of this library.
     */
    public static native String GetLibVersion();

/************************************************************************************
 * rfid库
 * radio frequency identification devices library
 ************************************************************************************/

    /**
     * 描述:  打开射频模块功能
     * Open RFID Module
     *
     * @return 0 if success, others if fail.
     * !0：打开模块失败 0：打开成功
     */
    public static native int RFIDModuleOpen();

    /**
     * 描述:  关闭射频模块功能
     * Close RFID Module
     * 返回参数:  !0：关闭模块失败 0：关闭成功
     *
     * @return 0 if success, others if fail.
     */
    public static native int RFIDMoudleClose();

//    /**
//     * 描述:  选择射频天线,
//     * 入口参数:  sLot: 1内置天线, 2外置天线
//     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
//    . */
//    public static native int SelectRFIDSlot(int sLot);

//    /*
//     * 描述:  初始化射频模块功能
//     * 入口参数:  Index:天线参数选择  1内置天线, 2外置天线
//     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
//     * 说明: (该函数为预留函数, 实为空操作, 可忽略)
//     */
//    public static native int Rf_Init(int Index);

    /**
     * 描述:  关闭/打开射频
     * Power of/off the RF
     *
     * @param bOnOff = 1  打开射频, = 0  关闭射频
     *               1[on], 0[off]
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * @tips if you want re-power on, please delay at least 5ms to do that.
     * 在关闭射频之后，如果再次打开必须至少延时 5ms
     */
    public static native int Rf_OnOff(int bOnOff);

    //    /**
//     * 描述:  通讯协议选择
//     * 入口参数:
//     * mode:  射频通讯协议模式
//     *      = 0 type A
//     *      = 1 FELICA
//     *      = 2 type B
//     *      =7    ISO18092(NFC)
//     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
//     * 说明:    射频模块在 Rf_Init 之后，将默认 TypeA 的协议。所以如果其他协议的情况下，可以不用调该函数.
//     * (该函数为预留函数, 实为空操作, 可忽略)
//     */
    @Deprecated
    public static native int Rf_ModeSet(byte mode);

    /**
     * 描述:  TypeA 操作, 寻卡+防冲突+选卡过程
     * REQuest command, Type A [REQA]
     *
     * @param mode  =0[寻 IDLE 状态的卡 0x26 命令], =other[寻 HALT 状态的卡 0x52 命令]
     *              [in] 0 if card is IDLE, others if card is HALT.
     * @param bLen: 返回的 pUID 的长度(new byte[1])
     *              [out] length of {@param pUID} datum, in bytes. Use (new byte[1]) to declare it.
     * @param pUID: 返回的信息: 序列号+SAK(1)+ATQA(2) (new byte[13-])
     *              [out] UID datum of the card. It contain SN+SAK(1)+ATQA(2). Use (new byte[13-]) to declare it.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * @tips SAK, ATQA mainly used to identify the different card properties
     * 对于 SAK,ATQA 主要是用来识别不同的卡属性.具体卡属性请参看卡属性表.该函数会自动开射频, 所以调用该函数前可以不调用Rf_OnOff(1)
     */
    public static native int Rfa_GetSNR(int mode, byte[] bLen, byte[] pUID);

    /**
     * 描述:  TypeA 操作，卡复位
     * Request for Answer To Select, Type A
     *
     * @param resp    CPU 卡复位返回的数据(new byte[255-])
     *                [out] The answer datum. Use (new byte[255-]) declare it.
     * @param respLen 返回数据的长度(new byte[1])
     *                [out] Length of {@param resp} datum. Use (new byte[1]) declare it.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfa_RATS(byte[] resp, byte[] respLen);

    /**
     * 描述:  对 TypeA 的 CPU 卡发送指令操作
     * Transmit APDUs to card TypeA
     *
     * @param send    发送的指令
     *                [in] APTUs to send.
     * @param len     发送指令的长度
     *                [in] Length of {@param send} datum.
     * @param OutData 接收的数据(new byte[514-])
     *                [out] Buffer to receive datum from card.
     * @param OutLen  接收数据的长度(new short[1])
     *                [out] Length of datum in {@param OutData}
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfa_APDU(byte[] send, int len, byte[] OutData, short[] OutLen);

    /**
     * 描述:  对 TypeA 的 CPU 卡发送指令操作
     * Transmit APDUs to card TypeA.
     *
     * @param send    发送的指令
     *                [in] APTUs to send.
     * @param len     发送指令的长度
     *                [in] Length of {@param send} datum.
     * @param OutData 接收的数据(new byte[512-])
     *                [out] Buffer to receive datum from card.
     * @param OutLen  接收数据的长度(new short[1])
     *                [out] Length of datum in {@param OutData}
     * @param SW      返回状态码指针(new short[1])
     *                The response message status code. Use (new short[1]) declare it.
     * @return: 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * @tips: 在 Rfa_APDU 函数中返回的数据域中包括有 SW1SW2 状态码
     * 而在 Rfaw_APDU 函数中返回的数据中没有包括 SW1SW2 状态码,已经被提取出来放到SW 中返回.
     * @see #Rfa_APDU
     */
    public static native int Rfaw_APDU(byte[] send, int len, byte[] OutData, short[] OutLen, short[] SW);

    /**
     * 描述:  使 TypeA 进入 Halt 状态
     * HaLT Command, Type A
     *
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfa_Halt();

    //    /**
//     * 描述: 对 ULC 卡
//     */
    @Deprecated
    public static native int Rfulc_Transceive(byte len, byte[] pData, byte[] rev, byte ex_size);

    /**
     * 描述:  对 ULC 卡进行密钥认证
     * Authenticate ULC
     *
     * @param keyn 认证的密钥索引
     *             [in] Index of key.
     * @param Key  16 个字节密钥数据
     *             [in] key datum, 16 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfulc_Authen(byte keyn, byte[] Key);

    /**
     * 描述: 从 UL 卡里读数据
     * Read data from ULC
     *
     * @param bAddress 卡里读数据的起始地址
     *                 [in] The address to read.
     * @param pData    读出的数据(new byte[16])
     *                 [out] datum of read, the datum length is always 16 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rful_Read(byte bAddress, byte[] pData);

    /**
     * 描述:  对 UL 卡写数据
     * Write data to ULC
     *
     * @param bAddress 写入数据的起始地址
     *                 [in] The address to write.
     * @param pData    写入的数据，4 个字节
     *                 [in] The datum to write. 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * @tip This function is special purpose for UL/ULC command A2, execution time short than {@link #Rful_ComWrite}
     * 该命令是 UL/ULC 卡专用命令 A2,写时间比 Rful_ComWrite 要快.
     */
    public static native int Rful_Write(byte bAddress, byte[] pData);

    /**
     * 描述: 对 UL 卡写数据
     * Write data to ULC
     *
     * @param bAddress 写入数据的起始地址
     *                 [in] The address to write.
     * @param pData    写入的数据，16 个字节
     *                 [in] The datum to write. 16 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    该命令兼容 Mifare Class 卡写命令 A0
     */
    public static native int Rful_ComWrite(byte bAddress, byte[] pData);

    /**
     * 描述:  对 MIFARE 卡进行密钥认证
     * Authenticate MIFARE card
     *
     * @param cKeyab:   =0x0A  A 密钥
     *                  =0x0B  B 密钥
     * @param cSecotrNo 扇区号
     *                  SecotrNo of MIFARE card.
     * @param pKey      密钥
     *                  [in] Key datum.
     * @param pSNR      卡唯一号, 4个字节.
     *                  [in] SNR of card, 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Authen(byte cKeyab, byte cSecotrNo, byte[] pKey, byte[] pSNR);

    //    /*******************************************************************************
//     * FUNCTION: MIFARE 卡值操作指令
//     * IN   ：
//     * cSubCommand
//     * cBlockNo
//     * pValue
//     * OUT :
//     * RETURN: 0---成功；其他---失败
//     ********************************************************************************/
    @Deprecated
    public static native int MifareChange(byte cSubCommand, byte cBlockNo, byte[] pValue);

    /**
     * 描述: MIFARE 卡，对块进行传输操作，把内部寄存器里的值写到块里。
     * Transfer datum form internal register to {@param cBlockNo}.
     *
     * @param cBlockNo: 要修改的块号
     *                  [in] Block number.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Transfer(byte cBlockNo);

    /**
     * 描述:  对 MIFARE 卡读一个块。
     * Read MIFARE block.
     *
     * @param cBlockNo 块号
     *                 [in] block number.
     * @param pRdData  读出数据(16个字节, new byte[16])
     *                 [out] datum of read, the datum length is always 16 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Read(byte cBlockNo, byte[] pRdData);

    /**
     * 描述:  对 MIFARE 卡指定的值块进行读操作
     * Read MIFARE Value-block
     *
     * @param cBlockNo 块号
     *                 [in] block number.
     * @param bValue   读出的值的大小(小端模式)，(4 个字节, new byte[4])
     *                 [out] block value(little-endian), 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * @tip the block must be value-block.
     * 所读的块必须满足 Mifare Class  值块格式要求
     */
    public static native int Rfmif_ReadValue(byte cBlockNo, byte[] bValue);

    /**
     * 描述:  向 MIFARE 写一个值块
     * Write MIFARE block
     *
     * @param cBlockNo 块号
     *                 [in] block number.
     * @param pWrData  写入数据，16 个字节
     *                 [in] datum to write, the datum length is always 16 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Write(byte cBlockNo, byte[] pWrData);

    /**
     * 描述:  MIFARE 卡，将普通块写成值块模式
     * Write MIFARE Value-block
     *
     * @param cBlockNo 块
     *                 [in] block number.
     * @param bValue   值的大小(小端模式)，4 字节
     *                 [in] block value(little-endian), 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     * @tip if {@param cBlockNo} is not value-block, this function will change it to be value-block.
     * 该块不管满不满足值块要求，都将写成值块的格式。
     */
    public static native int Rfmif_WriteValue(byte cBlockNo, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对值块加值，并且保存到寄存器里。
     * increase MIFARE Value-block, and save value in register.
     *
     * @param block  块号
     *               [in] block number.
     * @param bValue 加值的大小(小端模式)，4 字节
     *               [in] increment value(little-endian), 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_inc(byte block, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对值块减值，并且保存到寄存器里
     * decrease MIFARE Value-block, and save value in register.
     *
     * @param block  块号
     *               [in] block number.
     * @param bValue 减值的大小(小端模式)，4 字节
     *               [in] decrement value(little-endian), 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_dec(byte block, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对块发送恢复命令。把 cBlockNo 块的内容读到内部寄存器
     * Restore MIFARE Value-block, and save value in register.
     *
     * @param cBlockNo 块号
     *                 [in] block number.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Restore(byte cBlockNo);

    /**
     * 描述: MIFARE 卡，对块加值并修改保存
     * increase MIFARE Value-block, and Transfer value to dest block.
     *
     * @param bSrcBlock 对该值块 bSrcBlock 的内容加值，并保存到内部寄存器里
     *                  [in] src block number.
     * @param bDstBlock 加值后的内部寄存器里的内容传输保存到 bDstBlock 块里。
     *                  [in] dest block number.
     * @param bValue    加值的大小(小端模式)，4 字节
     *                  [in] increment value(little-endian), 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_IncTransfer(byte bSrcBlock, byte bDstBlock, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对块减值并修改保存
     * decrease MIFARE Value-block, and Transfer value to dest block.
     *
     * @param bSrcBlock 对该值块 bSrcBlock 的内容减值，并保存到内部寄存器里
     *                  [in] src block number.
     * @param bDstBlock 减值后的内部寄存器里的内容传输保存到 bDstBlock 块里。
     *                  [in] dest block number.
     * @param bValue    减值的大小(小端模式)，4 字节
     *                  [in] decrement value(little-endian), 4 bytes.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_DecrementTransfer(byte bSrcBlock, byte bDstBlock, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对卡的恢复并且传输保存指令
     * Restore MIFARE Value-block, and Transfer value to dest block.
     *
     * @param bSrcBlock 把该块读到内部寄存器里。
     *                  [in] src block number.
     * @param bDstBlock 把内部寄存器的内容保存到 bDstBlock 块里
     *                  [in] dest block number.
     * @return 0 if success, others if fail.
     * !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_RestoreTransfer(byte bSrcBlock, byte bDstBlock);

    /**
     * 描述: B 卡寻卡指令
     * REQuest command, Type B.[REQB]
     *
     * @param afi    固定填0
     *               [in] Application Family Identifier, Type B, Recommend afi = 0;
     * @param param  固定填0
     *               [in] Recommend param = 0;
     * @param atqb   返回卡的ID(new byte[256-])
     *               [out] Answer To reQuest, Type B
     * @param aqtLen 返回数据长度(new byte[1])
     *               [out] Length of datum in {@param atqb}.
     * @return 0 if success, others if fail.
     * 0---成功；其他---失败
     * @tip Type B UID is form atqb[1] to atqb[aqtLen[0] - 1], except atqb[0].
     * atqb[0]不属于ID号部分, 真正的B卡ID号是从atqb[1]到atqb[aqtLen[0] - 1].
     */
    public static native int REQB(byte afi, byte param, byte[] atqb, byte[] aqtLen); // (OUT) 12 * n bytes

    /**
     * 描述: B 卡选卡指令
     * Selection command, Type B
     *
     * @param uid    B卡ID号, 请注意是从atqb[1]开始的, 因此不能直接传入atqb.
     *               [in] Type B UID
     * @param cid    固定填0
     *               [in] Card IDentifier, Recommend cid = 0;
     * @param ata    选卡成功后返回的数据(new byte[255-])
     *               [out] data from card
     * @param ataLen 返回数据的长度(new byte[1])
     *               [out] length of datum in {@param ata}.
     * @return 0 if success, others if fail.
     * 0---成功；其他---失败
     */
    public static native int ATTRIB(byte[] uid, byte cid, byte[] ata, byte[] ataLen);


/************************************************************************************
 * psam库
 * Purchase Secure Access Module[PSAM] Library
 ************************************************************************************/

    /**
     * 描述: 检查看是否在位函数
     * check card slot presence status
     *
     * @param slot 卡槽号, [0, 4], 0 是外部卡槽
     *             card slot, value is [0, 4], 0 is the external slot.
     * @return 0 if presence, others if not presence.
     * 0=卡在位, 其他=卡未检测到
     * 说  明: 该函数是从函数tda8026_rst中提取出来的, 注意硬件的检测脚是否被拉死到3V3
     */
    public static native int SimPresenceCheck(byte slot);

    /**
     * 描述: 复位指令
     * Reset SAM Card
     *
     * @param CardSelect 卡槽选择[0, 4]
     *                   [in] card slot, value is [0, 4], 0 is the external slot.
     * @param uiRate     波特率, 参数为整型9600、38400、115200
     *                   [in] Baud rate, must be 9600/38400/115200.
     * @param ucVoltage  复位电压, 参数为1、2、3, 分别对应电压1.8V, 3.3V, 5V
     *                   [in] Reset Voltage, the value must be 1/2/3. mean is:
     *                   1=[1.8V], 2=[3.3V], 3=[5V].
     * @param mode       复位模式, 0:冷复位. 1:热复位
     *                   [in] Reset mode. 0 if Cold reset, 1 if Warm reset.
     * @param rLen       返回数据长度(new byte[1])
     *                   [out] Length of datum in {@param ATR}.
     * @param ATR        复位信息(new byte[128-])
     *                   [out] Datum from card after reset.
     * @return 0 if success, others if fail.
     * 0---成功；其他---失败
     */
    public static native int IccSimReset(byte CardSelect, int uiRate, byte ucVoltage, byte[] rLen, byte[] ATR, byte mode);


    /**
     * 描述: APDU指令
     * Transmit APDUs to card SAM.
     *
     * @param Slot    卡槽选择0 ~ 4
     *                [in] card slot, value is [0, 4], 0 is the external slot.
     * @param buffer  要发送的数据(APDU命令)
     *                [in] APTUs to send.
     * @param length  发送数据长度
     *                [in] Length of {@param buffer} datum.
     * @param rbuffer APDU 返回信息(new byte[256-])
     *                [out] Buffer to receive datum from card.
     * @param Revlen  接收数据长度(new short[1])
     *                [out] Length of datum in {@param rbuffer}
     * @param SW      APDU 状态码(new short[1])
     *                The response message status code. Use (new short[1]) declare it.
     * @return 0 if success, others if fail.
     * 0---成功；其他---失败
     */
    public static native int Sim_Apdu(byte Slot, byte[] buffer, short length, byte[] rbuffer, short[] Revlen, short[] SW);

    /**
     * 描述: 打开SAM 模块
     * Open sim module
     *
     * @return 0 if success, others if fail.
     * 0---成功；其他---失败
     */
    public static native int OpenSimMoudle();

    /**
     * 描述: 关闭SAM 模块
     * Close sim module
     *
     * @return 0 if success, others if fail.
     * 0---成功；其他---失败
     */
    public static native int CloseSimModule();

/*****************************************************************************
 * misc库控制各个模块电源
 * A misc library controlling each module power supply
 *****************************************************************************/
    /**
     * 功  能: 打开电源管理
     * Open the power management
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int OpenPowerManager();

    /**
     * 功  能: 关闭电源管理
     * Shut off the power management
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int ClosePowerManager();

    /**
     * 功  能: 打开MRZ电源
     * Power on Machine Readable Zone (MRZ) Reader
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int MRZPowerOn();

    /**
     * 功  能: 关闭MRZ电源
     * Power off Machine Readable Zone (MRZ) Reader
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int MRZPowerOff();

    /**
     * 功  能: 打开指纹模块电源
     * Power on fingerprint module
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int FingerPrintPowerOn();

    /**
     * 功  能: 关闭指纹模块电源
     * Power off fingerprint module
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int FingerPrintPowerOff();

    /**
     * 功  能: 打开UsbTypeA电源
     * Power on UsbTypeA power supply
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int UsbTypeAPowerOn();

    /**
     * yzq:
     * 功  能: 关闭UsbTypeA电源
     * Power off UsbTypeA power supply
     *
     * @return 0 if success, others if fail.
     * 0=成功, 其他=失败
     */
    public static native int UsbTypeAPowerOff();


    static {
        System.loadLibrary("jniEmpPad");/*yzq: jniLib */
    }
}
