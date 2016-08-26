package com.xiongdi.recognition.util;

import android.os.Build;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by moubiao on 2016/8/16.
 * AES加密工具类
 */
public class AESUtil {
    private final String TAG = "moubiao";
    private static final String ALGORITHM = "AES";
    private Cipher mCipher;

    private AESUtil(int mode, String key, int keySize, String transformation, IvParameterSpec ivParameterSpec) {
        try {
            //得到随机数
            SecureRandom secureRandom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                secureRandom = SecureRandom.getInstance("SHA1PRNG", "Crypto");
            } else {
                secureRandom = SecureRandom.getInstance("SHA1PRNG");
            }
            secureRandom.setSeed(key.getBytes());
            //得到密钥生成器
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(keySize, secureRandom);
            //生成一个密钥
            SecretKey secretKey = keyGenerator.generateKey();
            //将密钥还原为SecretKeySpec
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), ALGORITHM);
            //初始化Cipher
            mCipher = Cipher.getInstance(transformation);
            if (ivParameterSpec == null) {
                mCipher.init(mode, keySpec);
            } else {
                mCipher.init(mode, keySpec, ivParameterSpec);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public Cipher getCipher() {
        return mCipher;
    }

    /**
     * 加密文件
     *
     * @param inputPath  源文件路径
     * @param outputPath 加密后保存文件的路径
     */
    public void encryptFile(String inputPath, String outputPath) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        CipherOutputStream cipherOutputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(inputPath));
            fileOutputStream = new FileOutputStream(new File(outputPath));
            cipherOutputStream = new CipherOutputStream(fileOutputStream, mCipher);

            int readLen;
            byte[] buffer = new byte[2048];
            while ((readLen = fileInputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (cipherOutputStream != null) {
                try {
                    cipherOutputStream.flush();
                    cipherOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解密文件
     *
     * @param inputPath  源文件
     * @param outputPath 解密后保存文件的路径
     */
    public void decryptFile(String inputPath, String outputPath) {
        FileInputStream fileInputStream = null;
        CipherInputStream cipherInputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            fileInputStream = new FileInputStream(new File(inputPath));
            cipherInputStream = new CipherInputStream(fileInputStream, mCipher);
            fileOutputStream = new FileOutputStream(new File(outputPath));
            int readLen;
            byte[] buffer = new byte[2048];
            while ((readLen = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (cipherInputStream != null) {
                try {
                    cipherInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static final class Builder {
        private String KEY;
        private int mKeySize = 128;
        private int mMode = Cipher.DECRYPT_MODE;
        private String mTransformation = ALGORITHM;
        private IvParameterSpec mIvParameterSpec;

        public Builder mode(int mode) {
            mMode = mode;
            return this;
        }

        public Builder key(String key) {
            this.KEY = key;
            return this;
        }

        public Builder keySize(int keySize) {
            mKeySize = keySize;
            return this;
        }

        public Builder transformation(String transformation) {
            mTransformation = transformation;
            return this;
        }

        public Builder ivParameter(IvParameterSpec ivParameterSpec) {
            mIvParameterSpec = ivParameterSpec;
            return this;
        }

        public AESUtil builder() {
            if (TextUtils.isEmpty(KEY)) {
                throw new NullPointerException(KEY);
            }
            return new AESUtil(mMode, KEY, mKeySize, mTransformation, mIvParameterSpec);
        }
    }
}
