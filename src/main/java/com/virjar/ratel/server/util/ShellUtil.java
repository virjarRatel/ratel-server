package com.virjar.ratel.server.util;

import com.virjar.ratel.server.vo.CertificateReq;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author by fury.
 * version 2019/12/4.
 */
@Slf4j
public class ShellUtil {

    private static final int MOD_PARAMS_VERSION = 14;

    private static String getArgs(CertificateReq certificateReq, String certificatePath,
        Long engineBinVersion) {
        String account = certificateReq.getAccount();
        String expireDate = certificateReq.getExpireDate();
        String certificateContent = certificateReq.getCertificateContent();
        Integer licenceVersion = certificateReq.getLicenceVersion();
        String[] packageList = certificateReq.getPackageList();
        String[] deviceList = certificateReq.getDeviceList();

        String args = "";

        // 默认参数
        args += "-a=" + account + " -d=" + certificatePath;

        if (StringUtils.isNotEmpty(expireDate)) {
            args += " -e=" + expireDate;
        }
        if (StringUtils.isNotEmpty(certificateContent)) {
            args += " -o=" + certificateContent;
        }
        if (null != licenceVersion && licenceVersion > 0) {
            if (engineBinVersion > MOD_PARAMS_VERSION) {
                args += " -V=" + licenceVersion;
            } else {
                args += " -v=" + licenceVersion;
            }
        }
        if (null != packageList && packageList.length > 0) {
            List<String> packages = Arrays.asList(packageList);
            String packageJoin = String.join(",", packages);
            args += " -p=" + packageJoin;
        }
        if (null != deviceList && deviceList.length > 0) {
            List<String> devices = Arrays.asList(deviceList);
            String deviceJoin = String.join(",", devices);
            args += " -s=" + deviceJoin;
        }
        return args;
    }

    public static void executeAndWriteFile(CertificateReq certificateReq, String scriptPath,
        File certificateFile, Long engineBinVersion) {
        if (StringUtils.isEmpty(scriptPath)) {
            return;
        }
        String args = getArgs(certificateReq, certificateFile.getAbsolutePath(), engineBinVersion);
        try {
            String cmd = "sh " + scriptPath + " " + args;
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
        } catch (Exception e) {
            log.error("[RATEL_SHELL_EXECUTE_error]:{}", e.getMessage(), e);
        }
    }
}
