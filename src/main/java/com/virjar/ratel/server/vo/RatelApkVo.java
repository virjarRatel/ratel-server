package com.virjar.ratel.server.vo;

import com.virjar.ratel.server.entity.RatelApk;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.entity.RatelUserApk;
import com.virjar.ratel.server.service.AliOSSHelper;
import lombok.Data;

import java.util.Date;

@Data
public class RatelApkVo {

    private Long id;

    private Long userId;

    private Long apkId;


    private String fileName;

    private String fileHash;

    private String appPackage;

    private String appName;

    private String appVersion;

    private Long appVersionCode;

    private String ossUrl;

    private Date uploadTime;

    private Date lastUsedTime;

    private Boolean isXposedModule;

    private String apkFileName;

    private String alias;

    public static RatelApkVo transform(RatelApk ratelApk, RatelUser ratelUser, RatelUserApk ratelUserApk,AliOSSHelper aliOSSHelper) {
        RatelApkVo ratelApkVo = new RatelApkVo();
        ratelApkVo.setId(ratelUserApk.getId());
        ratelApkVo.setUserId(ratelUser.getId());
        ratelApkVo.setApkId(ratelApk.getId());
        ratelApkVo.setFileName(ratelUserApk.getApkFileName());
        ratelApkVo.setFileHash(ratelApk.getFileHash());
        ratelApkVo.setAppPackage(ratelApk.getAppPackage());
        ratelApkVo.setAppVersion(ratelApk.getAppVersion());
        ratelApkVo.setAppVersionCode(ratelApk.getAppVersionCode());
        ratelApkVo.setOssUrl(aliOSSHelper.genAccessUrl(ratelApk.getOssUrl()));
        //TODO
        ratelApkVo.setUploadTime(ratelApk.getUploadTime());
        ratelApkVo.setLastUsedTime(ratelApk.getLastUsedTime());
        ratelApkVo.setIsXposedModule(ratelApk.getIsXposedModule());
        ratelApkVo.setApkFileName(ratelUserApk.getApkFileName());
        ratelApkVo.setAlias(ratelUserApk.getAlias());
        ratelApkVo.setAppName(ratelApk.getAppName());
        return ratelApkVo;
    }
}
