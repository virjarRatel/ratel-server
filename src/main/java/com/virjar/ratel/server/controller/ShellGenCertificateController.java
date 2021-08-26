package com.virjar.ratel.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.virjar.ratel.server.service.RatelKeyGenService;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.util.RatelLicenceEncryptor;
import com.virjar.ratel.server.vo.CertificateReq;
import com.virjar.ratel.server.vo.CertificateVo;
import com.virjar.ratel.server.vo.CommonRes;
import io.swagger.annotations.ApiOperation;

import java.nio.charset.StandardCharsets;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 执行脚本生成证书 前端控制器
 * </p>
 *
 * @author fury
 * @since 2019-12-04
 */
@Slf4j
@RestController
@RequestMapping("/api/ratel/shell/certificate")
public class ShellGenCertificateController {

    @Resource
    private RatelKeyGenService ratelKeyGenService;

    @LoginRequired(forAdmin = true)
    @ApiOperation("新建授权证书")
    @ResponseBody
    @PostMapping("/create")
    public CommonRes<String> createCertificate(@RequestBody CertificateReq certificateReq) {

        log.info("[RATEL_SERVER_2_API_shell_新建授权证书]req={}", certificateReq.toString());
        String certificate = ratelKeyGenService.createCertificate(certificateReq);
        if (Strings.isNullOrEmpty(certificate)) {
            return CommonRes.failed("新建授权证书异常,请联系管理员");
        }
        log.info("[RATEL_SERVER_2_API_shell_新建授权证书]resp={}", certificate);
        return CommonRes.success(certificate);
    }

    @LoginRequired(forAdmin = true)
    @ApiOperation("升级授权证书")
    @PostMapping("/upgrade")
    @ResponseBody
    public CommonRes<String> upgradeCertificate(@RequestBody CertificateReq certificateReq) {
        log.info("[RATEL_SERVER_2_API_shell_升级授权证书]req={}", certificateReq.toString());
        try {
            String newCertificate = ratelKeyGenService.upgradeCertificate(certificateReq);
            log.info("[RATEL_SERVER_2_API_shell_升级授权证书]newCertificate={}", newCertificate);
            return CommonRes.success(newCertificate);
        } catch (Exception e) {
            return CommonRes.failed("升级授权证书异常,请联系管理员");
        }
    }

    @ApiOperation("授权证书详情")
    @PostMapping("/detail")
    @ResponseBody
    public CommonRes<CertificateVo> detailCertificate(@RequestBody CertificateReq certificateReq) {
        try {
            String certificateContent = certificateReq.getCertificateContent();
            byte[] decrypt = RatelLicenceEncryptor.standardRSADecrypt(certificateContent);
            String containerJsonString = new String(decrypt, StandardCharsets.UTF_8);
            JSONObject containerJson = JSONObject.parseObject(containerJsonString);
            CertificateVo certificateVo = containerJson.toJavaObject(CertificateVo.class);
            return CommonRes.success(certificateVo);
        } catch (Exception e) {
            return CommonRes.failed("获取授权证书异常,请联系管理员");
        }
    }

}
