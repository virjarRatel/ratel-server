package com.virjar.ratel.server.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public class UploadUtils {
    public static File uploadToTempFile(String prefix, String suffix, MultipartFile multipartFile) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        multipartFile.transferTo(tempFile);
        return tempFile;
    }
}
