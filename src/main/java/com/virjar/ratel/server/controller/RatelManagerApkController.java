package com.virjar.ratel.server.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.virjar.ratel.server.entity.RatelManagerApk;
import com.virjar.ratel.server.mapper.RatelManagerApkMapper;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.service.AliOSSHelper;
import com.virjar.ratel.server.util.FileFingerprinter;
import com.virjar.ratel.server.util.ReturnUtil;
import com.virjar.ratel.server.vo.CommonRes;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * ratelManager的发布包 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2019-09-02
 */
@RestController
@RequestMapping("/api/ratel/manager-apk")
@Slf4j
public class RatelManagerApkController {

    @Resource
    private RatelManagerApkMapper ratelManagerApkMapper;

    @Resource
    private AliOSSHelper aliOSSHelper;

    @ApiOperation("返回最新版本的5条记录")
    @PostMapping("/listLast")
    @ResponseBody
    public CommonRes<List<RatelManagerApk>> listLast() {
        List<RatelManagerApk> ratelManagerApks = ratelManagerApkMapper.selectList(new QueryWrapper<RatelManagerApk>()
                .orderByDesc(RatelManagerApk.APP_VERSION_CODE).last(" limit 5"));

        ratelManagerApks.forEach(ratelManagerApk -> ratelManagerApk.setOssUrl(aliOSSHelper.genAccessUrl(ratelManagerApk.getOssUrl())));
        return CommonRes.success(ratelManagerApks);
    }

    @ApiOperation("上传Manager apk文件")
    @PostMapping("/upload")
    @ResponseBody
    @LoginRequired(forAdmin = true)
    public CommonRes<RatelManagerApk> uploadApk(MultipartFile apkFile) {

        String srcFileName = apkFile.getOriginalFilename();
        if (!StringUtils.endsWithIgnoreCase(srcFileName, ".apk")) {
            return ReturnUtil.failed("the upload file must has .apk suffix");
        }

        File targetFile = null;
        ApkFile apkMetaApkFile = null;
        try {
            targetFile = File.createTempFile("managerAPK", ".apk");
            apkFile.transferTo(targetFile);

            String fileMD5 = FileFingerprinter.getFileMD5(targetFile.getAbsolutePath());
            RatelManagerApk existedManagerApk = ratelManagerApkMapper.selectOne(new QueryWrapper<RatelManagerApk>().eq(RatelManagerApk.FILE_HASH, fileMD5));

            if (existedManagerApk != null) {
                return CommonRes.failed("upload already!!");
            }

            apkMetaApkFile = new ApkFile(targetFile);
            ApkMeta apkMeta = apkMetaApkFile.getApkMeta();

            String packageName = apkMeta.getPackageName();
            if (!"com.virjar.ratel.manager".equals(packageName)) {
                return CommonRes.failed("ratel manager package name illegal!!");
            }

            RatelManagerApk ratelManagerApk = ratelManagerApkMapper.selectOne(new QueryWrapper<RatelManagerApk>().eq(RatelManagerApk.APP_VERSION_CODE, apkMeta.getVersionCode()));
            if (ratelManagerApk != null) {
                return CommonRes.failed("please upgrade manager versionCode before build apks");
            }

            ratelManagerApk = new RatelManagerApk();
            ratelManagerApk.setAppVersion(apkMeta.getVersionName());
            ratelManagerApk.setFileHash(fileMD5);
            ratelManagerApk.setAppVersionCode(apkMeta.getVersionCode());
            //manager是一个小文件,所以就直接上传了，不需要异步
            String ossUrl = aliOSSHelper.uploadFileToAliOSS("RatelManager-" + apkMeta.getVersionName() + "-" + apkMeta.getVersionCode() + "-" + fileMD5 + ".apk", targetFile);
            ratelManagerApk.setOssUrl(ossUrl);

            ratelManagerApkMapper.insert(ratelManagerApk);
            return CommonRes.success(ratelManagerApk);

        } catch (IOException e) {
            log.error("get a upload temp file failed!!", e);
            return ReturnUtil.failed(e);
        } finally {
            if (targetFile != null) {
                try {
                    FileUtils.forceDelete(targetFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            IOUtils.closeQuietly(apkMetaApkFile);
        }
    }
}
