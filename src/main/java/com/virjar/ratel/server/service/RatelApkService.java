package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virjar.ratel.server.entity.RatelApk;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.entity.RatelUserApk;
import com.virjar.ratel.server.mapper.RatelApkMapper;
import com.virjar.ratel.server.mapper.RatelUserApkMapper;
import com.virjar.ratel.server.util.FileFingerprinter;
import com.virjar.ratel.server.vo.CommonRes;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

/**
 * <p>
 * 上传的apk文件 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@Service
@Slf4j
public class RatelApkService extends ServiceImpl<RatelApkMapper, RatelApk> implements IService<RatelApk> {

    @Resource
    private RatelUserApkMapper ratelUserApkMapper;

    @Resource
    private OssFileCacheManager ossFileCacheManager;

    @Resource
    private AliOSSHelper aliOSSHelper;

    public CommonRes<RatelApk> uploadInternal(RatelUser loginedUser, File targetFile) {

        ApkMeta apkMeta;
        byte[] fileData;
        boolean isXposedModule;
        try (ApkFile parsedApkFile = new ApkFile(targetFile)) {
            parsedApkFile.setPreferredLocale(Locale.CHINA);
            apkMeta = parsedApkFile.getApkMeta();
            fileData = parsedApkFile.getFileData("assets/ratelConfig.properties");
            isXposedModule = (parsedApkFile.getFileData("assets/xposed_init") != null);
        } catch (IOException e) {
            log.error("error to parse apk file:", e);
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("not a apk file");
        }
        //ignore

        //如果是一个已经被ratel处理过的apk，那么不能再次处理

        if (fileData != null) {
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.failed("can not upload the apk injected by ratel framework");
        }

        //modify srcFileName

        String fileMD5 = FileFingerprinter.getFileMD5(targetFile.getAbsolutePath());
        String srcFileName = apkMeta.getPackageName() + "_" + apkMeta.getVersionName() + "_" + apkMeta.getVersionCode() + "_" + fileMD5 + ".apk";

        RatelApk ratelApk = getOne(new QueryWrapper<RatelApk>().eq(RatelApk.FILE_HASH, fileMD5));
        if (ratelApk != null && ratelApk.getOssUrl() != null) {
            ratelApk.setLastUsedTime(new Date());
            updateById(ratelApk);
            ensureFileOwner(ratelApk, loginedUser, srcFileName);
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
            return CommonRes.success(ratelApk);
        }

        ossFileCacheManager.cacheFile(targetFile, fileMD5);

        if (ratelApk == null) {
            ratelApk = new RatelApk();
        }

        ratelApk.setAppName(apkMeta.getName());
        ratelApk.setAppPackage(apkMeta.getPackageName());
        ratelApk.setLastUsedTime(new Date());
        ratelApk.setAppVersion(apkMeta.getVersionName());
        ratelApk.setAppVersionCode(apkMeta.getVersionCode());
        ratelApk.setFileHash(fileMD5);
        ratelApk.setFileName(srcFileName);
        ratelApk.setUploadTime(new Date());
        ratelApk.setIsXposedModule(isXposedModule);

        if (ratelApk.getId() != null) {
            updateById(ratelApk);
        } else {
            save(ratelApk);
        }

        RatelApk finalRatelApk = ratelApk;
        aliOSSHelper.uploadToOss(fileMD5, targetFile, finalUrl -> {
            finalRatelApk.setOssUrl(finalUrl);
            updateById(finalRatelApk);
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
        });


        ensureFileOwner(ratelApk, loginedUser, srcFileName);
        return CommonRes.success(ratelApk);
    }

    private void ensureFileOwner(RatelApk ratelApk, RatelUser ratelUser, String srcFileName) {
        ratelApk = getOne(new QueryWrapper<RatelApk>().eq(RatelApk.FILE_HASH, ratelApk.getFileHash()));
        RatelUserApk ratelUserApk = ratelUserApkMapper.selectOne(new QueryWrapper<RatelUserApk>().eq(RatelUserApk.APK_ID, ratelApk.getId())
                .eq(RatelUserApk.USER_ID, ratelUser.getId()));

        if (ratelUserApk != null) {
            return;
        }
        ratelUserApk = new RatelUserApk();
        ratelUserApk.setApkId(ratelApk.getId());
        ratelUserApk.setUserId(ratelUser.getId());
        ratelUserApk.setApkFileName(srcFileName);
        ratelUserApk.setAlias(srcFileName);

        ratelUserApkMapper.insert(ratelUserApk);
    }
}
