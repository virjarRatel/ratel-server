package com.virjar.ratel.server.service;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author lei.X
 * @date 2019/4/11
 */
@Slf4j
@Service
public class AliOSSHelper {


    private static final String endpoint = "https://oss-cn-beijing.aliyuncs.com";
    @Value("${oss.accessKey}")
    private String accessKeyId;
    @Value("${oss.secretKey}")
    private String accessKeySecret;

    @Value("${oss.bucket}")
    private String bucketName;

    private static final BlockingQueue<UploadTask> queue = new LinkedBlockingDeque<>();
    private Thread asyncTaskThread = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                UploadTask uploadTask = queue.take();
                String uploadUrl = uploadFileToAliOSS(genOssFileName(uploadTask.file_hash, uploadTask.uploadFile), uploadTask.uploadFile);
                uploadTask.uploadCallback.onUploadFinished(uploadUrl);
            } catch (Throwable throwable) {
                log.warn("upload  file failed", throwable);
            }
        }

    });

    public void uploadToOss(String hash, File theFile, UploadCallback uploadCallback) {
        if (!asyncTaskThread.isAlive()) {
            asyncTaskThread.start();
        }

        queue.add(new UploadTask(hash, theFile, uploadCallback));
    }

    public String uploadFileToAliOSSWithHash(String file_hash, File uploadFile) {


        String uploadUrl = uploadFileToAliOSS(genOssFileName(file_hash, uploadFile), uploadFile);
        return uploadFileToAliOSS(uploadUrl, uploadFile);
    }

    private static String genOssFileName(String file_hash, File uploadFile) {
        String name = uploadFile.getName();
        String suffix = "";
        int i = name.lastIndexOf(".");
        if (i > 0) {
            suffix = name.substring(i);
        }
        if (".apk".equalsIgnoreCase(suffix)) {
            try (ApkFile apkFile = new ApkFile(uploadFile)) {
                ApkMeta apkMeta = apkFile.getApkMeta();
                return apkMeta.getPackageName() + "_" + apkMeta.getVersionName() + "_" + apkMeta.getVersionCode() + "_" + file_hash + suffix;
            } catch (IOException e) {
                //ignore
            }

        }
        return "ratel_server_" + file_hash + suffix;


    }

    public String uploadFileToAliOSS(String fileName, File uploadFile) {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        ossClient.putObject(bucketName, fileName, uploadFile);
        ossClient.shutdown();
        return fileName;
    }

    public void downloadFile(String fileName, File dest) {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        ossClient.getObject(new GetObjectRequest(bucketName, fileName), dest);
        ossClient.shutdown();
    }


    public interface UploadCallback {
        void onUploadFinished(String finalUrl);
    }

    private static class UploadTask {
        String file_hash;
        File uploadFile;
        UploadCallback uploadCallback;

        UploadTask(String file_hash, File uploadFile, UploadCallback uploadCallback) {
            this.file_hash = file_hash;
            this.uploadFile = uploadFile;
            this.uploadCallback = uploadCallback;
        }
    }

    public String genAccessUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return fileName;
        }
        if (fileName.startsWith("http:") || fileName.startsWith("https:")) {
            return fileName;
        }
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        URL url = ossClient.generatePresignedUrl(bucketName, fileName, new Date(new Date().getTime() + 1000 * 60 * 60 * 8));
        ossClient.shutdown();

        return url.toString();
    }

}
