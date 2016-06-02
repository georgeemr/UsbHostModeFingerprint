/*yzq: 注意，请务必确保包路径为：package com.xiongdi; */
package com.xiongdi;

/**
 * 射频返回表请参考配套开发文档.
 */
public class EmpPad {
    /***
     * 获取jni库版本信息, 返回字符串形式的版本信息
     */
    public static native String GetLibVersion();

    /*yzq: rfid */

    /**
     * 描述:  打开射频模块功能
     * 返回参数:  !0：打开模块失败 0：打开成功
     */
    public static native int RFIDModuleOpen();

    /**
     * 描述:  关闭射频模块功能
     * 返回参数:  !0：关闭模块失败 0：关闭成功
     */
    public static native int RFIDMoudleClose();

    /**
     * 描述:  选择射频天线,
     * 入口参数:  sLot: 1内置天线, 2外置天线
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
    . */
    public static native int SelectRFIDSlot(int sLot);

    /**
     * 描述:  初始化射频模块功能
     * 入口参数:  Index:天线参数选择  1内置天线, 2外置天线
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rf_Init(int Index);

    /**
     * 描述:  关闭/打开射频
     * 入口参数:  bOnOff : = 1  打开射频, = 0  关闭射频
     * 返回参数: !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    在关闭射频之后，如果再次打开必须至少延时 5ms
     */
    public static native int Rf_OnOff(int bOnOff);

    /**
     * 描述:  通讯协议选择
     * 入口参数:
     * mode:  射频通讯协议模式
     *      = 0 type A
     *      = 1 FELICA
     *      = 2 type B
     *      =7    ISO18092(NFC)
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    射频模块在 Rf_Init 之后，将默认 TypeA 的协议。所以如果其他协议的情况下，可以不用调该函数.
     */
    public static native int Rf_ModeSet(byte mode);

    /**
     * 描述:  TypeA 操作, 寻卡+防冲突+选卡过程
     * 入口参数:
     * mode:
     *      =0        寻 IDLE 状态的卡 0x26 命令
     *      =other     寻 HALT 状态的卡 0x52 命令
     * bLen: 返回的 pUID 的长度
     * pUID: 返回的信息: 序列号+SAK(1)+ATQA(2)
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明: 对于 SAK,ATQA 主要是用来识别不同的卡属性.具体卡属性请参看卡属性表.该函数会自动开射频, 所以调用该函数前可以不调用Rf_OnOff(1)
     */
    public static native int Rfa_GetSNR(int mode, byte[] bLen, byte[] pUID);

    /**
     * 描述:  TypeA 操作，卡复位
     * 入口参数:  resp：  CPU 卡复位返回的数据
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfa_RATS(byte[] resp);

    /**
     * 描述:  对 TypeA 的 CPU 卡发送指令操作
     * 入口参数:
     *      send:          发送的指令
     *      len:           发送指令的长度
     *      OutData:       接收的数据
     *      OutLen:        接收数据的长度
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfa_APDU(byte[] send, int len, byte[] OutData, short[] OutLen);

    /**
     * 描述:  对 TypeA 的 CPU 卡发送指令操作
     * 入口参数:
     *      send:          发送的指令
     *      len:           发送指令的长度
     *      OutData:       接收的数据
     *      OutLen:        接收数据的长度
     *      SW:            返回状态码指针
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    在 Rfa_APDU 函数中返回的数据域中包括有 SW1SW2 状态码
     * 而在 Rfaw_APDU 函数中返回的数据中没有包括 SW1SW2 状态码,已经被提取出来放到SW 中返回.
     */
    public static native int Rfaw_APDU(byte[] send, int len, byte[] OutData, short[] OutLen, short[] SW);

    /**
     * 描述:  使 TypeA 进入 Halt 状态
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfa_Halt();

//    /**
//     * 描述: 对 ULC 卡
//     */
//    public static native int Rfulc_Transceive(byte len, byte[] pData, byte[] rev, byte ex_size);

    /**
     * 描述:  对 ULC 卡进行密钥认证
     * 入口参数:
     *      keyn： 认证的密钥索引
     *      Key： 16 个字节密钥数据(new btye[16])
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfulc_Authen(byte keyn, byte[] Key);

    /**
     * 描述: 从 UL 卡里读数据
     * 入口参数:
     *      bAddress： 卡里读数据的起始地址
     *      pData：    读出的数据，16 个字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rful_Read(byte bAddress, byte[] pData);

    /**
     * 描述:  对 UL 卡写数据
     * 入口参数:
     *      bAddress：    写入数据的起始地址
     *      pData：       写入的数据，4 个字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    该命令是 UL/ULC 卡专用命令 A2,写时间比 Rful_ComWrite 要快.
     */
    public static native int Rful_Write(byte bAddress, byte[] pData);

    /**
     * 描述: 对 UL 卡写数据
     * 入口参数:
     *      bAddress：    写入数据的起始地址
     *      pData：       写入的数据，16 个字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    该命令兼容 Mifare Class 卡写命令 A0
     */
    public static native int Rful_ComWrite(byte bAddress, byte[] pData);

    /**
     * 描述:  对 MIFARE 卡进行密钥认证
     * 入口参数:
     *      cKeyab:
     *          =0x0A  A 密钥
     *          =0x0B  B 密钥
     *      cSecotrNo:    扇区号
     *      *pKey:      密钥
     *      *pSNR:     卡唯一号, uint8_t[4].
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
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
//    public static native int MifareChange(byte cSubCommand, byte cBlockNo, byte[] pValue);

    /**
     * 描述: MIFARE 卡，对块进行传输操作，把内部寄存器里的值写到块里。
     * 入口参数:  cBlockNo: 要修改的块号
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Transfer(byte cBlockNo);

    /**
     * 描述:  对 MIFARE 卡读一个块。
     * 入口参数:
     *      cBlockNo: 块号
     *      pRdData: 读出数据，16 个字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Read(byte cBlockNo, byte[] pRdData);

    /**
     * 描述:  对 MIFARE 卡指定的值块进行读操作
     * 入口参数:
     *      cBlockNo:    块号
     *      bValue:      读出的值的大小(小端模式)，4 字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    所读的块必须满足 Mifare Class  值块格式要求
     */
    public static native int Rfmif_ReadValue(byte cBlockNo, byte[] bValue);

    /**
     * 描述:  向 MIFARE 写一个值块
     * 入口参数:
     *      cBlockNo:    块号
     *      pWrData:     写入数据，16 个字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Write(byte cBlockNo, byte[] pWrData);

    /**
     * 描述:  MIFARE 卡，将普通块写成值块模式
     * 入口参数:
     *      cBlockNo: 块
     *      bValue: 值的大小(小端模式)，4 字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     * 说明:    该块不管满不满足值块要求，都将写成值块的格式。
     */
    public static native int Rfmif_WriteValue(byte cBlockNo, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对值块加值，并且保存到寄存器里。
     * 入口参数:
     *      block：        块号
     *      bValue：       加值的大小(小端模式)，4 字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_inc(byte block, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对值块减值，并且保存到寄存器里
     * 入口参数:
     *      block: 块号
     *      bValue: 减值的大小(小端模式)，4 字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_dec(byte block, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对块发送恢复命令。把 cBlockNo 块的内容读到内部寄存器
     * 入口参数: cBlockNo: 块号
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_Restore(byte cBlockNo);

    /**
     * 描述: MIFARE 卡，对块加值并修改保存
     * 入口参数:
     *      bSrcBlock：     对该值块 bSrcBlock 的内容加值，并保存到内部寄存器里
     *      bDstBlock：     加值后的内部寄存器里的内容传输保存到 bDstBlock 块里。
     *      bValue：        加值的大小(小端模式)，4 字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_IncTransfer(byte bSrcBlock, byte bDstBlock, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对块减值并修改保存
     * 入口参数:
     *      bSrcBlock：     对该值块 bSrcBlock 的内容减值，并保存到内部寄存器里
     *      bDstBlock：     减值后的内部寄存器里的内容传输保存到 bDstBlock 块里。
     *      bValue：        减值的大小(小端模式)，4 字节
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_DecrementTransfer(byte bSrcBlock, byte bDstBlock, byte[] bValue);

    /**
     * 描述: MIFARE 卡，对卡的恢复并且传输保存指令
     * 入口参数:
     *      bSrcBlock：     把该块读到内部寄存器里。
     *      bDstBlock：     把内部寄存器的内容保存到 bDstBlock 块里
     * 返回参数:  !0：失败 0：成功, 详细返回值参考射频返回表说明
     */
    public static native int Rfmif_RestoreTransfer(byte bSrcBlock, byte bDstBlock);

    /*******************************************************************************
     * FUNCTION: B 卡寻卡指令
     * IN   ：
     *      afi:    固定填0
     *      param: 固定填0
     * OUT :
     *      atqb:   返回卡的ID
     *      aqtLen: 返回数据长度(new byte[1])
     * RETURN: 0---成功；其他---失败
     * 说明: atqb[0]不属于ID号部分, 真正的B卡ID号是从atqb[1]开始到结束的.
     ********************************************************************************/
    public static native int REQB(byte afi, byte param, byte[] atqb, byte[] aqtLen); // (OUT) 12 * n bytes

    /*******************************************************************************
     * FUNCTION: B 卡选卡指令
     * IN   ：
     *      uid:    B卡ID号, 请注意是从atqb[1]开始的, 因此不能直接传入atqb.
     *      cid:    固定填0
     * OUT :
     *      ata:    选卡成功后返回的数据
     *      ataLen: 返回数据的长度(new byte[1])
     * RETURN: 0---成功；其他---失败
     ********************************************************************************/
    public static native int ATTRIB(byte[] uid, byte cid, byte[] ata, byte[] ataLen);


    /*yzq: sam */

    /*******************************************************************************
     * FUNCTION: 复位指令
     * IN   ：
     *      CardSelect ---卡槽选择1 ~ 4
     *      uiRate ----波特率, 参数为整型9600、38400、115200
     *      ucVoltage----复位电压, 参数为1、2、3, 分别对应电压1.8V, 3.3V, 5V
     *      mode ---复位模式, 0:冷复位. 1:热复位
     * OUT :
     *      rLen ---返回数据长度(new byte[1])
     *      ATR---复位信息(new byte[18+])
     * RETURN: 0---成功；其他---失败
     ********************************************************************************/
    public static native int IccSimReset(byte CardSelect, int uiRate, byte ucVoltage, byte[] rLen, byte[] ATR, byte mode);


    /*******************************************************************************
     * FUNCTION: APDU指令
     * IN   ：
     *      Slot ---卡槽选择1 ~ 4
     *      buffer ----要发送的数据(APDU命令)
     *      length---发送数据长度
     * OUT :
     *      rbuffer---APDU 返回信息
     *      Revlen----接收数据长度
     *      SW-----APDU 状态码
     * RETURN: 0---成功；其他---失败
     ********************************************************************************/
    public static native int Sim_Apdu(byte Slot, byte[] buffer, short length, byte[] rbuffer, short[] Revlen, short[] SW);

    /*******************************************************************************
     * FUNCTION: 打开SAM 模块
     * IN   ：
     * OUT :
     * RETURN: 0---成功；其他---失败
     ********************************************************************************/
    public static native int OpenSimMoudle();

    /*******************************************************************************
     * FUNCTION: 关闭SAM 模块
     * IN   ：
     * OUT :
     * RETURN: 0---成功；其他---失败
     ********************************************************************************/
    public static native int CloseSimModule();

    static {
        System.loadLibrary("jniEmpPad");/*yzq: jniLib */
    }


}
