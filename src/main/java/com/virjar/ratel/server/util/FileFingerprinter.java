package com.virjar.ratel.server.util;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author lei.X
 * @date 2018/8/29
 * 文件指纹生成
 */
@Slf4j
public class FileFingerprinter {

    private static char[] hexChar = {'0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'};

    public static String getFileMD5(String filename) {
        try {
            return getHash(filename, "MD5");
        } catch (Exception e) {
            log.error("MD5加密失败:{}", e);
            throw new RuntimeException(e);
        }
    }

    public static String getFileMD5(InputStream inputStream) {
        try {
            return getHashWithInputStream(inputStream, "MD5");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static String getHash(String fileName, String hashType)
            throws Exception {
        try (FileInputStream inputStream = new FileInputStream(fileName)) {
            return getHashWithInputStream(inputStream, hashType);
        }
    }

    private static String getHashWithInputStream(InputStream inputStream, String hashType) throws IOException, NoSuchAlgorithmException {
        byte[] buffer = new byte[1000];
        MessageDigest md5 = MessageDigest.getInstance(hashType);
        int numRead;
        while ((numRead = inputStream.read(buffer)) > 0) {
            md5.update(buffer, 0, numRead);
        }
        inputStream.close();
        return toHexString(md5.digest());
    }

    private static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte b1 : b) {
            sb.append(hexChar[((b1 & 0xF0) >>> 4)]);
            sb.append(hexChar[(b1 & 0xF)]);
        }
        return sb.toString();
    }
}
