package com.xd;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * byte[]转换类
 */
public class Converter {

    // Hex help
    private static final byte[] HEX_CHAR_TABLE = {(byte) '0', (byte) '1',
            (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
            (byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B',
            (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F'};

    /**
     * convert a byte arrary to hex string
     *
     * @param raw byte arrary
     * @param len lenght of the arrary.
     * @return hex string.
     */
    public static String getHexString(byte[] raw, int len) {
        byte[] hex = new byte[2 * len];
        int index = 0;
        int pos = 0;

        for (byte b : raw) {
            if (pos >= len)
                break;

            pos++;
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }

        return new String(hex);
    }

    /**
     * convert a Asc byte arrary to Hex byte arrary, ex. [0x00, 0x00, 0x34, 0x4E] -> [4E]
     *
     * @param raw Asc byte arrary
     * @return hex byte arrary.
     */
    public static int AscToHex(int raw) {
        int value = 0;

        int v = (raw & 0xFF00) >> 8;
        if (v >= '0' && v <= '9') {
            v -= '0';
        } else if (v >= 'A' && v <= 'F') {
            v -= '7';
        } else {
            Log.i("AscToHex", "**** asc num illegal ****");
        }
        value = v << 4;

        v = (raw & 0x00FF);
        if (v >= '0' && v <= '9') {
            v -= '0';
        } else if (v >= 'A' && v <= 'F') {
            v -= '7';
        }
        value |= v;
        return (value);
    }

    /**
     * 将int类型的数据转换为byte数组
     *
     * @param n int数据
     * @return 生成的byte数组
     */
    public static byte[] intToBytes(int n) {
        String s = String.valueOf(n);
        return s.getBytes();
    }

    /**
     * 将byte数组转换为int数据
     *
     * @param b 字节数组
     * @return 生成的int数据
     */
    public static int bytesToInt(byte[] b) {
        String s = new String(b);
        return Integer.parseInt(s);
    }

    /**
     * 将int类型的数据转换为byte数组
     * 原理：将int数据中的四个byte取出，分别存储
     *
     * @param n int数据
     * @return 生成的byte数组
     */
    public static byte[] intToBytes2(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (24 - i * 8));
        }
        return b;
    }

    /**
     * 将byte数组转换为int数据
     *
     * @param b 字节数组
     * @return 生成的int数据
     */
    public static int byteToInt2(byte[] b) {
//	  return (((int)b[0]) << 24) + (((int)b[1]) << 16) + (((int)b[2]) << 8) + b[3];
//	  return (( (int)b[0]) << 8) + b[1];
        int n = (int) b[0];
        Log.i("Converter", "n=" + n);

        return ((int) b[0]);
    }

    /**
     * calculate the MD5 hash value.
     *
     * @param input input bytes
     * @return MD5 hash value.
     * @throws NoSuchAlgorithmException
     */
    public static byte[] calculateHash(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("MD5");
        digester.update(input, 0, input.length);
        byte[] digest = digester.digest();
        return digest;
    }


    // char[] 转 byte[]
    public byte[] getBytes(char[] chars) {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        //存入数据
        cb.put(chars);
        //缓冲区的位置 “倒带”
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    // byte[] 转 char[]
    public char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    //byte[] 数组转 16进制字符串
    public static String printHexString(byte[] b) {
        String result = "";

        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                //半字节的情形前面补 "0"
                hex = '0' + hex;
            }
            result = result + hex.toUpperCase();
        }
        return result;
    }

    /**
     * hex转换为String
     */
    public static String hex2String(byte[] b, int bLen) {
        String result = "";

        for (int i = 0; i < bLen; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                //半字节的情形前面补 "0"
                hex = '0' + hex;
            }
            result = result + hex.toUpperCase();
        }
        return result;
    }

    /**
     * String转hex
     */
    public static byte[] string2Hex(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        if ((hexString.length() % 2) != 0) {//奇数个字符, 右补"0" 凑成偶数字节
            length += 1;
            hexString = hexString + "0";
        }

        //字符串转 char[] 数组
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        int i, pos;
        for (i = 0; i < length; i++) {
            pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    //char 转 byte
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    //byte[] 数组异或加解密
    public static int dataEncDec(byte[] b, int v) {
        int nRet = 0;

        for (int i = 0; i < b.length; i++) {
            if (b[i] != 0 && b[i] != (byte) v) {
                b[i] ^= v;
            }
        }
        return nRet;
    }

    /**
     * short型数据转化为byte[]数组
     */
    public static byte[] short2ByteArray(short data) {
        byte[] targets = new byte[2];
        for (int i = 0; i < 2; i++) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte) ((data >>> offset) & 0xff);
        }
        return targets;
    }

    /**
     * byte[]数组转换为short
     */
    public static short byteArray2Short(byte[] bytes) {
        return (short) (bytes[1] & 0xff | (bytes[0] & 0xff) << 8);
    }
}
