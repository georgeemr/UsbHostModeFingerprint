package com.xiongdi.recognition.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by moubiao on 2016/3/24.
 * 文件工具类
 */
public class FileUtil {
    private final String TAG = "moubiao";

    /**
     * 判断目录是否存在，不存在则创建
     */
    public void isDirExist(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 复制文件
     */
    public void copyFile(String srcFileName, String destFileName)
            throws IOException {
        File srcFile = new File(srcFileName);
        File destFile = new File(destFileName);
        if (!srcFile.exists()) {
            return;
        }
        if (!srcFile.isFile()) {
            return;
        }
        if (!srcFile.canRead()) {
            return;
        }

        if (destFile.exists()) {
            destFile.createNewFile();
        }

        try {
            if (srcFile.exists()) {
                InputStream inStream = new FileInputStream(srcFile);
                FileOutputStream outStream = new FileOutputStream(destFile);
                byte[] buf = new byte[1024];
                int byteRead = 0;
                while ((byteRead = inStream.read(buf)) != -1) {
                    outStream.write(buf, 0, byteRead);
                }

                outStream.flush();
                outStream.close();
                inStream.close();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 循环删除文件及目录
     */
    public boolean deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.exists()) {
            if (file.isFile()) {
                return file.delete();
            }
            if (file.isDirectory()) {
                File[] childFiles = file.listFiles();
                if (childFiles == null || childFiles.length == 0) {
                    return file.delete();
                }

                for (File childFile : childFiles) {
                    deleteDir(childFile.getPath());
                }

                return file.delete();
            }
        }

        return false;
    }

    public boolean deleteFile(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);

            return file.exists() && file.isFile() && file.delete();
        }

        return false;
    }

    public File createFile(String filePath) {
        if (filePath == null) {
            Log.e(TAG, "createFile: file path is null");
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            File dir = new File(file.getParent());
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    try {
                        if (!file.createNewFile()) {
                            return null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    return null;
                }
            }
        }

        return file;
    }
}
