package com.virjar.ratel.server.service;

import com.virjar.ratel.server.util.FileFingerprinter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

@Service
@Slf4j
public class OssFileCacheManager {

    private static final int fileCacheLength = 120;

    @Value("ratel-file-cache")
    private String cacheDir;

    @Resource
    private AliOSSHelper aliOSSHelper;

    private void makeSureDirAndClean() {
        File cacheDirFile = new File(cacheDir);
        if (!cacheDirFile.exists()) {
            if (!cacheDirFile.mkdirs()) {
                throw new IllegalStateException("failed to create ratel cache dir");
            }
            return;
        }

        File[] files = cacheDirFile.listFiles();
        if (files == null) {
            return;
        }

        if (files.length < fileCacheLength) {
            //我们缓存120个文件
            return;
        }
        ArrayList<File> sortedFile = new ArrayList<>(Arrays.asList(files));
        sortedFile.sort(Comparator.comparingLong(File::lastModified));

        LinkedList<File> removeList = new LinkedList<>(sortedFile);

        while (removeList.size() > fileCacheLength) {
            File file = removeList.removeFirst();
            if (!file.delete()) {
                log.warn("failed to remove file:", file.getAbsoluteFile());
            }
        }
    }

    void cacheFile(File needCacheFile, String md5) {
        makeSureDirAndClean();
        File cacheDirFile = new File(cacheDir);
        //check if cached
        File[] files = cacheDirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().contains(md5)) {
                    return;
                }
            }
        }
        try {
            FileUtils.copyFile(needCacheFile, new File(cacheDirFile, md5));
        } catch (IOException e) {
            throw new IllegalStateException("copy file failed", e);
        }


    }

    File forceLoadFileFromOss(String ossUrl, String fileHash) {
        makeSureDirAndClean();
        File targetFile = new File(cacheDir, fileHash);
        if (targetFile.exists()) {
            if (!targetFile.setLastModified(System.currentTimeMillis())) {
                throw new IllegalStateException("update cache file timestamp failed");
            }
            return targetFile;
        }

        try {
            File tempFile = File.createTempFile("oss-temp", ".temp");
            aliOSSHelper.downloadFile(ossUrl, tempFile);
            //check hash again
            String fileMD5 = FileFingerprinter.getFileMD5(tempFile.getAbsolutePath());
            if (!fileMD5.equals(fileHash)) {
                log.warn("broken file from oss download task!!");
                throw new IllegalStateException("broken file from oss download task!!");
            }

            if (!tempFile.renameTo(targetFile)) {
                throw new IllegalStateException("create cache file failed");
            }
            if (!tempFile.delete()) {
                log.warn("failed to delete file: " + tempFile.getAbsolutePath());
            }
            if (!targetFile.exists()) {
                throw new IllegalStateException("create cache file failed");
            }
            return targetFile;
        } catch (IOException e) {
            log.error("download oss filed failed!!");
            throw new IllegalStateException("download oss filed failed!!", e);
        }

    }
}
