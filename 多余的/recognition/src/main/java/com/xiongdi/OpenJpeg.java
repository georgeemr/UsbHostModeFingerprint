package com.xiongdi;

/**
 * Created by moubiao on 2016/4/13
 * 压缩解压图片
 */
public class OpenJpeg {
    /*yzq: 获取库版本, 返回的字符串即是版本号 */
    public static native String GetLibVersion();

    /*yzq:
    * 功  能: 压缩,将普通图像格式转换为.j2k或.jp2,
    * 输入值: inputFilePathAndName: 输入文件, 扩展名为:  *.pnm, *.pgm, *.ppm, *.pgx, *png, *.bmp, *.tif, *.raw or *.tga
    * 输入值: outputFilePathAndName: 输出文件, 扩展名为:  *.j2k or *.jp2
    * 输入值: CompressRatio: 压缩比, 可设置三种色素的压缩比, 格式为 r,g,b, 例如:"100,200,300", 可传入null, 库自动以"100,100,100"进行压缩
    * 返回值: 0,成功; 其他,失败
    *  */
    public static native int CompressImage(String inputFilePathAndName, String outputFilePathAndName, String CompressRatio);

    /*yzq:
    * 功能: 解压,将j2k图像格式转换为普通图像
    * 输入值: inputFilePathAndName: 输入文件, 扩展名为:  *.j2k or *.jp2
    * 输入值: outputFilePathAndName: 输出文件, 扩展名为:  *.pnm, *.pgm, *.ppm, *.pgx, *png, *.bmp, *.tif, *.raw or *.tga
    * 返回值: 0,成功; 其他,失败
    * */
    public static native int DecompressImage(String inputFilePathAndName, String outputFilePathAndName);

    static {
        System.loadLibrary("jniopenjp2");
    }
}
