package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.virjar.ratel.server.entity.RatelApk;
import com.virjar.ratel.server.entity.RatelCertificate;
import com.virjar.ratel.server.entity.RatelHotModule;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.mapper.RatelCertificateMapper;
import com.virjar.ratel.server.mapper.RatelHotModuleMapper;
import com.virjar.ratel.server.util.CommonUtil;
import com.virjar.ratel.server.util.FileFingerprinter;
import com.virjar.ratel.server.vo.CommonRes;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * <p>
 * ratel热发模块 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2020-02-27
 */
@Service
@Slf4j
public class RatelHotModuleService extends ServiceImpl<RatelHotModuleMapper, RatelHotModule> {


    @Resource
    private AliOSSHelper aliOSSHelper;

    @Resource
    private RatelCertificateMapper ratelCertificateMapper;

    public CommonRes<RatelHotModule> uploadInternal(RatelUser optUser, File targetFile) {

        String fileMD5 = FileFingerprinter.getFileMD5(targetFile.getAbsolutePath());
        RatelHotModule ratelHotModule = getOne(new QueryWrapper<RatelHotModule>().eq(RatelApk.FILE_HASH, fileMD5));
        if (ratelHotModule != null && ratelHotModule.getOssUrl() != null) {
            //如果已经上传过
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.success(ratelHotModule);
        }

        ApkMeta apkMeta;
        Map<String, String> manifestMeta;
        try (ApkFile parsedApkFile = new ApkFile(targetFile)) {
            parsedApkFile.setPreferredLocale(Locale.CHINA);
            apkMeta = parsedApkFile.getApkMeta();
            if (parsedApkFile.getFileData("assets/xposed_init") == null) {
                if (!targetFile.delete()) {
                    log.warn("failed to remove file: {}", targetFile);
                }
                return CommonRes.failed("the apk is`nt xposed module apk");
            }
            manifestMeta = CommonUtil.parseManifestMap(parsedApkFile.getManifestXml());
        } catch (IOException e) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            log.error("error to parse apk file:", e);
            return CommonRes.failed("not a apk file");
        }

        // 检查这个插件是否配置目标app,需要注意的是，一个热发插件只能指定一个目标app
        String forRatelApp = manifestMeta.get("for_ratel_apps");
        if (StringUtils.isBlank(forRatelApp)) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("need <for_ratel_apps> configuration in AndroidManifest meta-data");
        }
        forRatelApp = forRatelApp.trim();
        if (forRatelApp.contains(",")) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("only support one target app for ratel hot module,now is: " + forRatelApp);
        }

        if (forRatelApp.length() > 125) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("target app package  name to long");
        }

        //我们根据证书来实现不同用户之间的热发插件隔离
        String ratelCertificateId = manifestMeta.get("ratel_certificate_id");
        if (StringUtils.isBlank(ratelCertificateId)) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("need <ratel_certificate_id> configuration in AndroidManifest meta-data");
        }


        //这个可以为空
        String ratelGroup = manifestMeta.get("ratel_group");
        if (StringUtils.isBlank(ratelGroup)) {
            ratelGroup = RatelHotModule.RATEL_GROUP_ALL;
        }

        RatelHotModule existRatelHotModule = getOne(new QueryWrapper<RatelHotModule>()
                .eq(RatelHotModule.MODULE_PKG_NAME, apkMeta.getPackageName())
                .eq(RatelHotModule.MODULE_VERSION_CODE, apkMeta.getVersionCode())
                .eq(RatelHotModule.CERTIFICATE_ID, ratelCertificateId)
                .eq(RatelHotModule.FOR_RATEL_APP, forRatelApp)
                .eq(RatelHotModule.RATEL_GROUP, ratelGroup));

        if (existRatelHotModule != null && StringUtils.isNotBlank(existRatelHotModule.getOssUrl())) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("this version of module has upload already! please upgrade versionCode config for build.gradle");
        }
        if (existRatelHotModule == null) {
            existRatelHotModule = new RatelHotModule();
        }
        existRatelHotModule.setCertificateId(ratelCertificateId);
        existRatelHotModule.setFileHash(fileMD5);
        existRatelHotModule.setForRatelApp(forRatelApp);
        existRatelHotModule.setModuleVersion(apkMeta.getVersionName());
        existRatelHotModule.setModuleVersionCode(apkMeta.getVersionCode());
        existRatelHotModule.setRatelGroup(ratelGroup);
        existRatelHotModule.setUploadTime(new Date());
        existRatelHotModule.setUserId(optUser.getId());
        existRatelHotModule.setUserName(optUser.getAccount());

        existRatelHotModule.setModulePkgName(apkMeta.getPackageName());
        existRatelHotModule.setEnable(true);
        existRatelHotModule.setFileSize(targetFile.length() / 1024);

        RatelHotModule finalExistRatelHotModule = existRatelHotModule;

        aliOSSHelper.uploadToOss(fileMD5, targetFile, finalUrl -> {
            finalExistRatelHotModule.setOssUrl(finalUrl);
            finalExistRatelHotModule.setUploadTime(new Date());
            updateById(finalExistRatelHotModule);
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
        });

        if (existRatelHotModule.getId() != null) {
            updateById(existRatelHotModule);
        } else {
            save(existRatelHotModule);
        }
        return CommonRes.success(existRatelHotModule);
    }


    public List<RatelHotModule> list(RatelUser ratelUser, int num) {

        List<RatelCertificate> ratelCertificates =
                ratelCertificateMapper.selectList(
                        new QueryWrapper<RatelCertificate>()
                                .eq(RatelCertificate.USER_ID, ratelUser.getId())
                );

        if (ratelCertificates.size() == 0) {
            return Collections.emptyList();
        }

        List<String> retrieve = ratelCertificates.stream()
                .map(RatelCertificate::getLicenceId).collect(Collectors.toList());

        return list(new QueryWrapper<RatelHotModule>()
                .in(RatelHotModule.CERTIFICATE_ID, retrieve)
                .orderByDesc(RatelHotModule.ID)
                .last(" limit " + num)
        );
    }

    public List<RatelHotModule> availableHotModuleConfig(String certificateId,
                                                         String mPackage,
                                                         String group) {
        QueryWrapper<RatelHotModule> queryWrapper = new QueryWrapper<RatelHotModule>()
                .eq(RatelHotModule.CERTIFICATE_ID, certificateId)
                .eq(RatelHotModule.FOR_RATEL_APP, mPackage)
                .eq(RatelHotModule.ENABLE, true);
        if (StringUtils.isBlank(group)) {
            queryWrapper = queryWrapper.eq(RatelHotModule.RATEL_GROUP, RatelHotModule.RATEL_GROUP_ALL);
        } else {
            queryWrapper = queryWrapper.in(RatelHotModule.RATEL_GROUP, Lists.newArrayList(RatelHotModule.RATEL_GROUP_ALL, group));
        }

        List<RatelHotModule> candidateAvailableModules = list(queryWrapper);

        //聚合，每个pkg取版本最高的，不是最高的设置enable=false

        Map<String, List<RatelHotModule>> map = new HashMap<>();
        candidateAvailableModules.forEach(
                ratelHotModule -> {
                    List<RatelHotModule> ratelHotModules = map.computeIfAbsent(ratelHotModule.getModulePkgName(), k -> Lists.newArrayList());
                    ratelHotModules.add(ratelHotModule);
                });

        List<RatelHotModule> ratelHotModules = Lists.newArrayList();
        for (List<RatelHotModule> allVersions : map.values()) {
            if (allVersions.size() == 1) {
                ratelHotModules.add(allVersions.get(0));
            } else {
                allVersions.sort((o1, o2) -> o2.getModuleVersionCode().compareTo(o1.getModuleVersionCode()));
                ratelHotModules.add(allVersions.get(0));
                for (int i = 1; i < allVersions.size(); i++) {
                    RatelHotModule ratelHotModule = allVersions.get(i);
                    ratelHotModule.setEnable(false);
                    updateById(ratelHotModule);
                }
            }
        }
        return ratelHotModules;
    }


    public boolean changeEnable(RatelHotModule reqModule, Boolean targetStatus) {
        if (targetStatus) {
            // 若是启用，需要查出已经启用的热发模块，然后禁用
            QueryWrapper<RatelHotModule> queryWrapper = new QueryWrapper<RatelHotModule>()
                    .eq(RatelHotModule.CERTIFICATE_ID, reqModule.getCertificateId())
                    .eq(RatelHotModule.MODULE_PKG_NAME, reqModule.getModulePkgName())
                    .eq(RatelHotModule.FOR_RATEL_APP, reqModule.getForRatelApp())
                    .eq(RatelHotModule.RATEL_GROUP, reqModule.getRatelGroup())
                    .eq(RatelHotModule.ENABLE, true);
            List<RatelHotModule> enableModules = list(queryWrapper);
            for (RatelHotModule module : enableModules) {
                // 禁用其他热发模块
                if (!reqModule.getId().equals(module.getId())) {
                    module.setEnable(false);
                    updateById(module);
                }
            }
        }
        // 启用/禁用当前指定的热发模块
        reqModule.setEnable(targetStatus);
        return updateById(reqModule);
    }
}
