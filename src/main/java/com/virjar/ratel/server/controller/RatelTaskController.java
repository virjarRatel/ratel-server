package com.virjar.ratel.server.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.virjar.ratel.server.entity.RatelApk;
import com.virjar.ratel.server.entity.RatelCertificate;
import com.virjar.ratel.server.entity.RatelTask;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.mapper.RatelApkMapper;
import com.virjar.ratel.server.mapper.RatelCertificateMapper;
import com.virjar.ratel.server.mapper.RatelTaskMapper;
import com.virjar.ratel.server.service.RatelTaskService;
import com.virjar.ratel.server.system.LoginInterceptor;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.service.AliOSSHelper;
import com.virjar.ratel.server.util.CommonUtil;
import com.virjar.ratel.server.util.Constant;
import com.virjar.ratel.server.util.QRCodeGenerator;
import com.virjar.ratel.server.vo.CertificateVo;
import com.virjar.ratel.server.vo.CommonRes;
import com.virjar.ratel.server.vo.CreateRatelTaskBean;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * apk处理任务 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@RestController
@RequestMapping("/api/ratel/task")
public class RatelTaskController {


    @Resource
    private RatelApkMapper ratelApkMapper;

    @Resource
    private RatelTaskMapper ratelTaskMapper;
    @Resource
    private RatelCertificateMapper ratelCertificateMapper;

    @Resource
    private RatelTaskService ratelTaskService;

    @Resource
    private AliOSSHelper aliOSSHelper;

    @ApiOperation("创建构建任务")
    @PostMapping("/create")
    @ResponseBody
    @LoginRequired
    @ApiImplicitParams(
            @ApiImplicitParam(name = "ratelEngine", value = "选择的引擎", paramType = "query", allowableValues = "appendDex,rebuildDex,shell")
    )
    public CommonRes<RatelTask> createTask(CreateRatelTaskBean createRatelTaskBean) {

        RatelUser ratelUser = LoginInterceptor.getSessionUser();

        //check
        if (StringUtils.isEmpty(createRatelTaskBean.getOriginApkFileHash())) {
            return CommonRes.failed("need pass origin apk");
        }
        //这里用户如果拿到了文件的id，那么可以操作非可见的自有apk，不过这个限定为非越权操作。因为apk资源并不是需要管控的，具有价值的资源

        RatelApk originApk = ratelApkMapper.selectOne(new QueryWrapper<RatelApk>().eq(RatelApk.FILE_HASH, createRatelTaskBean.getOriginApkFileHash()));
        if (originApk == null) {
            return CommonRes.failed("origin apk file not exists!!");
        }

        RatelApk xposedModuleApk = null;
        if (StringUtils.isNotBlank(createRatelTaskBean.getXposedModuleApkFileHash())) {
            xposedModuleApk = ratelApkMapper.selectOne(new QueryWrapper<RatelApk>().eq(RatelApk.FILE_HASH, createRatelTaskBean.getXposedModuleApkFileHash()));
            if (xposedModuleApk == null) {
                return CommonRes.failed("xposed module apk file not exists!!");
            }
        }

        if (createRatelTaskBean.getCertificateId() == null) {
            return CommonRes.failed("need parse certificate");
        }
        RatelCertificate ratelCertificate = ratelCertificateMapper.selectOne(new QueryWrapper<RatelCertificate>().eq(RatelCertificate.ID, createRatelTaskBean.getCertificateId()));
        if (ratelCertificate == null) {
            return CommonRes.failed("certificate not exist");
        }

        //检查证书是否为当前用户所有
        if (!ratelCertificate.getUserId().equals(ratelUser.getId())) {
            return CommonRes.failed("certificate owner error!!");
        }

        CertificateVo certificateVo = CommonUtil.certificateToVo(ratelCertificate);
        String[] packageList = certificateVo.getPackageList();
        boolean hasPermission = false;
        if (packageList == null || packageList.length == 0) {
            hasPermission = true;
        } else {
            for (String appPackage : packageList) {
                if (originApk.getAppPackage().equals(appPackage)) {
                    hasPermission = true;
                    break;
                }
            }
        }
        if (!hasPermission) {
            return CommonRes.failed("your certificate now suitable for this apk!!");
        }

        //检查证书是否过期
        if (certificateVo.getExpire() - System.currentTimeMillis() < 24 * 60 * 60 * 1000) {
            return CommonRes.failed("certificate expire!!");
        }

        return ratelTaskService.createTask(ratelUser, originApk, xposedModuleApk, ratelCertificate, createRatelTaskBean);
    }


    @ApiOperation("列出构建任务,不分页，只列出最后20条")
    @GetMapping("/list")
    @ResponseBody
    @LoginRequired
    public CommonRes<List<RatelTask>> listTask() {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        return CommonRes.success(
                ratelTaskMapper.selectList(
                        new QueryWrapper<RatelTask>().eq(RatelTask.USER_ID, ratelUser.getId()).orderByDesc(RatelTask.ID)
                                .last(" limit 80")
                ).stream().map(this::transformUrl).collect(Collectors.toList())
        );
    }


    @ApiOperation("查询任务详情")
    @GetMapping("/taskDetail")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelTask> taskDetail(Long taskId) {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        return CommonRes.success(transformUrl(
                ratelTaskMapper.selectOne(
                        new QueryWrapper<RatelTask>()
                                .eq(RatelTask.USER_ID, ratelUser.getId())
                                .eq(RatelTask.ID, taskId)
                ))
        );
    }


    @GetMapping(value = "/taskRRI")
    @LoginRequired
    public ResponseEntity<byte[]> getQRImage(Long taskId) {

        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        RatelTask ratelTask = ratelTaskMapper.selectOne(
                new QueryWrapper<RatelTask>()
                        .eq(RatelTask.USER_ID, ratelUser.getId())
                        .eq(RatelTask.ID, taskId)
        );
        transformUrl(ratelTask);


        byte[] qrcode = QRCodeGenerator.generateQRCodeImage(ratelTask.getOutputOssUrl());
        // Set headers
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(qrcode, headers, HttpStatus.CREATED);
    }

    @ApiOperation("导出apk")
    @GetMapping("/setExportStatus")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelTask> setExportStatus(Long taskId, Boolean export) {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        RatelTask ratelTask = ratelTaskMapper.selectOne(
                new QueryWrapper<RatelTask>()
                        .eq(RatelTask.USER_ID, ratelUser.getId())
                        .eq(RatelTask.ID, taskId)
        );

        if (ratelTask == null) {
            return CommonRes.failed("record not exist");
        }

        ratelTask.setNeedExport(export);
        ratelTaskMapper.updateById(ratelTask);
        return CommonRes.success(transformUrl(ratelTask));
    }


    private RatelTask transformUrl(RatelTask ratelTask) {
        if (ratelTask == null) {
            return null;
        }
        ratelTask.setLogOssUrl(aliOSSHelper.genAccessUrl(ratelTask.getLogOssUrl()));
        ratelTask.setOutputOssUrl(aliOSSHelper.genAccessUrl(ratelTask.getOutputOssUrl()));
        return ratelTask;
    }


    private CommonRes<RatelTask> checkTaskOwner(Long taskId) {
        if (taskId == null) {
            return CommonRes.failed("record not existed");
        }
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        RatelTask ratelTask = ratelTaskMapper.selectById(taskId);
        if (ratelTask == null) {
            return CommonRes.failed("record not existed");
        }
        if (!ratelTask.getUserId().equals(ratelUser.getId())) {
            return CommonRes.failed("permission delay!!");
        }
        return CommonRes.success(ratelTask);
    }

    @ApiOperation("重试失败任务")
    @GetMapping("/retryFiledTask")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelTask> retryFailedTask(Long taskId) {
        CommonRes<RatelTask> checkRes = checkTaskOwner(taskId);
        if (!checkRes.isOk()) {
            return checkRes;
        }

        RatelTask ratelTask = checkRes.getData();

        Integer taskStatus = ratelTask.getTaskStatus();
        if (taskStatus != Constant.ratelTaskStatusFailed && taskStatus != Constant.rateltaskStatusBadcase) {
            return CommonRes.failed("task status error,please use clone task function if execute task again");
        }

        ratelTask.setTaskStatus(Constant.ratelTaskStatusInit);
        ratelTask.setFinishTime(null);
        ratelTask.setOutputOssUrl(null);
        ratelTask.setConsumer(null);
        ratelTaskMapper.updateById(ratelTask);
        // ratelTask = ratelTaskMapper.selectById(ratelTask.getId());
        return CommonRes.success(transformUrl(ratelTask));
    }


    @ApiOperation("克隆构建任务")
    @GetMapping("/cloneTask")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelTask> cloneRatelTask(Long taskId) {
        CommonRes<RatelTask> checkRes = checkTaskOwner(taskId);
        if (!checkRes.isOk()) {
            return checkRes;
        }
        RatelTask ratelTask = checkRes.getData();
        return CommonRes.success(ratelTaskService.cloneTask(ratelTask));
    }


}
