package com.xiongdi.recognition.util;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Android Bitmap Object to .bmp image (Windows BMP v3 24bit) file util class
 * <p/>
 * ref : http://en.wikipedia.org/wiki/BMP_file_format
 *
 * @author ultrakain ( ultrasonic@gmail.com )
 * @since 2012-09-27
 */
public class BmpUtil {
    private final int BMP_WIDTH_OF_TIMES = 4;
    private final int BYTE_PER_PIXEL = 3;

    /**
     * Android Bitmap Object to Window's v3 24bit Bmp Format File
     *
     * @param orgBitmap 要转换的bitmap
     * @param outPath   图片输出的路径
     * @return file saved result
     */
    public boolean save(Bitmap orgBitmap, String outPath) {
        if (orgBitmap == null || outPath == null) {
            return false;
        }

        boolean isSaveSuccess = true;

        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size. reason : bmp file's width equals 4's multiple
        int dummySize = 0;
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        if (isBmpWidth4Times(width)) {
            hasDummy = true;
            dummySize = width % BMP_WIDTH_OF_TIMES;
            dummyBytesPerRow = new byte[dummySize];
            Arrays.fill(dummyBytesPerRow, (byte) 0x00);
        }

        int[] pixels = new int[width * height];
        int imageSize = pixels.length * BYTE_PER_PIXEL + (height * dummySize);
        int imageDataOffset = 0x36;
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        ByteBuffer buffer;
        buffer = ByteBuffer.allocate(fileSize);

        try {
            /**
             * BITMAP FILE HEADER Write Start
             **/
            //位图文件的类型(2字节)，必须为BM 0x424d 表示.bmp
            buffer.put((byte) 0x42);
            buffer.put((byte) 0x4D);

            //位图文件的大小(4字节)，以字节为单位
            buffer.put(writeInt(fileSize));

            //位图文件保留字，必须为0
            buffer.put(writeShort((short) 0));
            buffer.put(writeShort((short) 0));

            //位图数据（像素数组）的地址偏移，也就是起始地址
            buffer.put(writeInt(imageDataOffset));

            /** BITMAP FILE HEADER Write End */

            /** BITMAP INFO HEADER Write Start */
            //本结构(信息头)所占用字节数 40字节
            buffer.put(writeInt(0x28));

            //位图宽度和高度，单位为像素（有符号整数）
            buffer.put(writeInt(width));
            buffer.put(writeInt(height));

            //色彩平面数；只有1为有效值
            buffer.put(writeShort((short) 1));

            //色深
            buffer.put(writeShort((short) 24));

            //压缩方法，位图压缩类型，必须是 0
            buffer.put(writeInt(0));

            //图像大小。指原始位图数据的大小，与文件大小不是同一个概念。如果压缩方式为BI_RGB，则该项可能为零
            buffer.put(writeInt(imageSize));

            //图像的横向分辨率，单位为像素每米（有符号整数）
            buffer.put(writeInt(0));

            //图像的纵向分辨率，单位为像素每米（有符号整数）
            buffer.put(writeInt(0));

            //调色板的颜色数，为0时表示颜色数为默认的
            buffer.put(writeInt(0));

            //重要颜色数，为0时表示所有颜色都是重要的；通常不使用本项
            buffer.put(writeInt(0));

            /** BITMAP INFO HEADER Write End */

            int row = height;
            int column = width;
            int startPosition;
            int endPosition;
            while (row > 0) {
                startPosition = (row - 1) * column;
                endPosition = row * column;
                for (int i = startPosition; i < endPosition; i++) {
                    buffer.put(write24BitForPixel(pixels[i]));
                    if (hasDummy) {
                        if (i == endPosition - 1) {
                            buffer.put(dummyBytesPerRow);
                        }
                    }
                }
                row--;
            }

            FileOutputStream fos = new FileOutputStream(outPath);
            fos.write(buffer.array());
            fos.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            isSaveSuccess = false;
        }

        return isSaveSuccess;
    }

    /**
     * Is last pixel in Android Bitmap width 这个方法不能准确的判断是否是最后一个像素
     *
     * @param width
     * @param i
     * @return
     */
    private boolean isBitmapWidthLastPixcel(int width, int i) {
        return i > 0 && (i % (width - 1)) == 0;
    }

    /**
     * BMP file is a multiples of 4?
     *
     * @param width
     * @return
     */
    private boolean isBmpWidth4Times(int width) {
        return width % BMP_WIDTH_OF_TIMES > 0;
    }

    /**
     * Write integer to little-endian
     *
     * @param value
     * @return
     * @throws IOException
     */
    private byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);
        b[3] = (byte) ((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write integer pixel to little-endian byte array
     *
     * @param value
     * @return
     * @throws IOException
     */
    private byte[] write24BitForPixel(int value) throws IOException {
        byte[] b = new byte[3];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);

        return b;
    }

    /**
     * Write short to little-endian byte array
     *
     * @param value
     * @return
     * @throws IOException
     */
    private byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte) (value & 0x00FF);
        b[1] = (byte) ((value & 0xFF00) >> 8);

        return b;
    }
}
