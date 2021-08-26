package com.virjar.ratel.server.controller;

import static com.virjar.ratel.server.util.Constant.MAX_PKG_LENGTH;

import com.alibaba.fastjson.JSONObject;
import com.virjar.ratel.server.entity.RatelHotModule;
import com.virjar.ratel.server.service.AliOSSHelper;
import com.virjar.ratel.server.service.RatelHotModuleService;
import com.virjar.ratel.server.system.LoginInterceptor;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.util.ReturnUtil;
import com.virjar.ratel.server.util.UploadUtils;
import com.virjar.ratel.server.vo.CommonRes;
import io.swagger.annotations.ApiOperation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * ratel热发模块 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2020-02-27
 */
@RestController
@Slf4j
@RequestMapping("/api/ratel/ratel-hot-module")
public class RatelHotModuleController {

    @Resource
    private RatelHotModuleService ratelHotModuleService;

    @Resource
    private AliOSSHelper aliOSSHelper;

    @ApiOperation("上传热发模块apk文件")
    @PostMapping("/upload")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelHotModule> uploadApk(MultipartFile apkFile) {
        if (apkFile == null) {
            return CommonRes.failed("please select file!!");

        }
        String srcFileName = apkFile.getOriginalFilename();
        if (!StringUtils.endsWithIgnoreCase(srcFileName, ".apk")) {
            return ReturnUtil.failed("the upload file must has .apk suffix");
        }

        File targetFile;
        try {
            targetFile = UploadUtils.uploadToTempFile("ratel_hot_module", ".apk", apkFile);
            return ratelHotModuleService.uploadInternal(LoginInterceptor.getSessionUser(),
                    targetFile);
        } catch (IOException e) {
            log.error("get a upload temp file failed!!", e);
            return ReturnUtil.failed(e);
        }
    }

    @ApiOperation("展示本账号下所有的的热发模块")
    @GetMapping("/list")
    @ResponseBody
    @LoginRequired
    public CommonRes<List<RatelHotModule>> listHotModules() {
        return CommonRes.success(ratelHotModuleService
                .list(LoginInterceptor.getSessionUser(), 50)
                .stream()
                .peek(ratelHotModule -> ratelHotModule.setOssUrl(
                        aliOSSHelper.genAccessUrl(ratelHotModule.getOssUrl())))
                .collect(Collectors.toList()));
    }

    @ApiOperation("给手机拉取配置使用")
    @GetMapping("/hotModuleConfig")
    @ResponseBody
    public CommonRes<List<RatelHotModule>> getDownloadHotModuleConfig(
            String certificateId,
            String mPackage,
            @RequestParam(required = false) String group) {
        log.info("hotModuleConfig request -> certificateId:{} mPackage:{}  group:{}"
                , certificateId, mPackage, group);
        if (certificateId == null || mPackage == null) {
            return CommonRes.failed("nee params: {certificateId} and {mPackage}");
        }
        if (mPackage.length() > MAX_PKG_LENGTH) {
            return CommonRes.failed("package name not illegal");
        }
        CommonRes<List<RatelHotModule>> commonRes = CommonRes.success(ratelHotModuleService
                .availableHotModuleConfig(certificateId, mPackage, group).stream()
                .peek(ratelHotModule -> ratelHotModule.setOssUrl(
                        aliOSSHelper.genAccessUrl(ratelHotModule.getOssUrl())))
                .collect(Collectors.toList()));
        log.info("hotModuleConfig response:{}", JSONObject.toJSONString(commonRes));
        return commonRes;
    }

    /**
     * 启用热发模块某个版本，意味着禁用该模块其他版本。
     */
    @ApiOperation("启用/禁用热发模块")
    @GetMapping("/changeHotModuleStatus")
    @ResponseBody
    public CommonRes<Boolean> changeHotModuleStatus(Long hotModuleId, Boolean enable) {
        RatelHotModule preModule = ratelHotModuleService.getById(hotModuleId);
        if (null == preModule) {
            return CommonRes.failed("id:{" + hotModuleId + "} is no exist.");
        }
        if (preModule.getEnable().equals(enable)) {
            return CommonRes.success(true);
        }
        boolean update = ratelHotModuleService.changeEnable(preModule, enable);
        return CommonRes.success(update);
    }

}
