package com.virjar.ratel.server.service;

import com.alibaba.fastjson.JSONObject;
import com.virjar.ratel.server.entity.RatelEngineBin;
import com.virjar.ratel.server.util.RatelLicenceEncryptor;
import com.virjar.ratel.server.util.ShellUtil;
import com.virjar.ratel.server.vo.CertificateReq;
import com.virjar.ratel.server.vo.CertificateVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 管理员生成授权证书
 * </p>
 *
 * @author fury
 * @since 2019-12-06
 */
@Slf4j
@Service
public class RatelKeyGenService {

    @Resource
    private RatelEngineBinService ratelEngineBinService;


    public String createCertificate(CertificateReq certificateReq) {

        try {
            RatelEngineBin ratelEngineBin = ratelEngineBinService.nowEngineDir();
            File certificateFile = File.createTempFile("ratel_certificate", ".txt");
            String scriptPath = genScriptPath(ratelEngineBin);

            ShellUtil.executeAndWriteFile(certificateReq, scriptPath, certificateFile,
                ratelEngineBin.getEngineVersionCode());
            return FileUtils.readFileToString(certificateFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[RatelKeyGenService_createCertificate_error]:{}", e.getMessage(), e);
        }
        return "";
    }

    private String genScriptPath(RatelEngineBin ratelEngineBin) throws Exception {
        if (ratelEngineBin == null) {
            log.info("ratel engine not upload now");
            return "";
        }
        File keygenShFile = new File(ratelEngineBin.nowEngineDir, "ratel-keygen.sh");
        return keygenShFile.getAbsolutePath();
    }

    public String upgradeCertificate(CertificateReq certificateReq) {
        String certificateContent = certificateReq.getCertificateContent();
        byte[] decrypt = RatelLicenceEncryptor.standardRSADecrypt(certificateContent);
        String containerJsonString = new String(decrypt, StandardCharsets.UTF_8);
        JSONObject containerJson = JSONObject.parseObject(containerJsonString);
        CertificateVo certificateVo = containerJson.toJavaObject(CertificateVo.class);
        log.info("[RATEL_SERVER_2_API_shell_升级授权证书]certificateVo={}", certificateVo);

        int licenceVersion = certificateVo.getLicenceVersion();
        certificateReq.setLicenceVersion(licenceVersion + 1);

        return createCertificate(certificateReq);
    }


}
