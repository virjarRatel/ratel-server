package com.virjar.ratel.server.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.virjar.ratel.server.entity.RatelCertificate;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.mapper.RatelCertificateMapper;
import com.virjar.ratel.server.system.LoginInterceptor;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.util.CommonUtil;
import com.virjar.ratel.server.util.RatelLicenceEncryptor;
import com.virjar.ratel.server.vo.CertificateVo;
import com.virjar.ratel.server.vo.CommonRes;
import com.virjar.ratel.server.vo.RatelPage;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * <p>
 * 授权证书 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@RestController
@RequestMapping("/api/ratel/certificate")
public class RatelCertificateController {

    @Resource
    private RatelCertificateMapper ratelCertificateMapper;

    @ApiOperation("添加授权证书")
    @PostMapping("/importCertificate")
    @ResponseBody
    @LoginRequired
    public CommonRes<CertificateVo> importCertificate(String certificateContent) {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        try {
            String containerJsonString = new String(RatelLicenceEncryptor.standardRSADecrypt(certificateContent), StandardCharsets.UTF_8);
            JSONObject containerJson = JSONObject.parseObject(containerJsonString);
            CertificateVo certificateVo = containerJson.toJavaObject(CertificateVo.class);


            RatelCertificate ratelCertificate = ratelCertificateMapper.selectOne(
                    new QueryWrapper<RatelCertificate>().eq(RatelCertificate.LICENCE_ID, certificateVo.getLicenceId())
                            // .eq(RatelCertificate.LICENCE_VERSION_CODE, certificateVo.getLicenceVersion())
                            .eq(RatelCertificate.USER_ID, ratelUser.getId())
                            .orderByDesc(RatelCertificate.LICENCE_VERSION_CODE)
                            .last(" limit 1")
            );

            if (ratelCertificate != null) {
                if (ratelCertificate.getLicenceVersionCode() > certificateVo.getLicenceVersion()) {
                    return CommonRes.failed("certificate version down");
                }
            }
            boolean insert = false;
            if (ratelCertificate == null) {
                ratelCertificate = new RatelCertificate();
                insert = true;
            }
            ratelCertificate.setContent(certificateContent);
            ratelCertificate.setLicenceId(certificateVo.getLicenceId());
            ratelCertificate.setLicenceVersionCode(certificateVo.getLicenceVersion());
            ratelCertificate.setUserId(ratelUser.getId());

            if (insert) {
                ratelCertificateMapper.insert(ratelCertificate);
            } else {
                ratelCertificateMapper.updateById(ratelCertificate);
            }
            certificateVo.setId(ratelCertificate.getId());
            return CommonRes.success(certificateVo);
        } catch (Exception e) {
            return CommonRes.failed("illegal certificate");
        }
    }


    @ApiOperation("查看证书列表")
    @LoginRequired
    @GetMapping("listCertificate")
    @ResponseBody
    public CommonRes<Page<CertificateVo>> listCertificate(@PageableDefault Pageable pageable) {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        RatelPage<RatelCertificate> ratelPage = CommonUtil.wrapperPage(pageable);
        IPage<RatelCertificate> certificateIPages = ratelCertificateMapper.selectPage(ratelPage, new QueryWrapper<RatelCertificate>().eq(RatelCertificate.USER_ID, ratelUser.getId()));
        PageImpl<CertificateVo> pageImpl = new PageImpl<>(certificateIPages.getRecords().stream().map(input -> {
            if (input == null) {
                return null;
            }
            return CommonUtil.certificateToVo(input);
        }).collect(Collectors.toList()), pageable, ratelPage.getTotal());

        return CommonRes.success(pageImpl);
    }


    @LoginRequired
    @ApiOperation("查看单个证书详情")
    @ResponseBody
    @GetMapping("certificateDetail")
    public CommonRes<CertificateVo> queryCertificateDetail(String licenceId, Integer licenceVersionCode) {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        QueryWrapper<RatelCertificate> queryWrapper = new QueryWrapper<>();
        queryWrapper = queryWrapper.eq(RatelCertificate.USER_ID, ratelUser.getId()).eq(RatelCertificate.LICENCE_ID, licenceId);
        if (licenceVersionCode != null) {
            queryWrapper = queryWrapper.eq(RatelCertificate.LICENCE_VERSION_CODE, licenceVersionCode);
        } else {
            queryWrapper = queryWrapper.orderByAsc(RatelCertificate.LICENCE_VERSION_CODE).last(" limit 1");
        }
        RatelCertificate ratelCertificate = ratelCertificateMapper.selectOne(queryWrapper);
        if (ratelCertificate == null) {
            return CommonRes.failed("record not found");
        }
        return CommonRes.success(CommonUtil.certificateToVo(ratelCertificate));
    }
}
