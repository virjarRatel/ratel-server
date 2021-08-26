package com.virjar.ratel.server.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.virjar.ratel.server.entity.RatelApk;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.entity.RatelUserApk;
import com.virjar.ratel.server.mapper.RatelApkMapper;
import com.virjar.ratel.server.mapper.RatelUserApkMapper;
import com.virjar.ratel.server.service.AliOSSHelper;
import com.virjar.ratel.server.service.RatelApkService;
import com.virjar.ratel.server.system.LoginInterceptor;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.util.ReturnUtil;
import com.virjar.ratel.server.util.UploadUtils;
import com.virjar.ratel.server.vo.CommonRes;
import com.virjar.ratel.server.vo.RatelApkVo;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 上传的apk文件 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@RestController
@RequestMapping("/api/ratel/apk")
@Slf4j
public class RatelApkController {


    @Resource
    private RatelApkService ratelApkService;

    @Resource
    private RatelApkMapper ratelApkMapper;


    @Resource
    private RatelUserApkMapper ratelUserApkMapper;

    @Resource
    private AliOSSHelper aliOSSHelper;

    @ApiOperation("上传apk文件")
    @PostMapping("/upload")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelApk> uploadApk(MultipartFile apkFile) {
        if (apkFile == null) {
            return CommonRes.failed("please select file!!");

        }
        RatelUser loginedUser = LoginInterceptor.getSessionUser();
        String srcFileName = apkFile.getOriginalFilename();
        if (!StringUtils.endsWithIgnoreCase(srcFileName, ".apk")) {
            return ReturnUtil.failed("the upload file must has .apk suffix");
        }

        File targetFile;
        try {
            targetFile = UploadUtils.uploadToTempFile("ratelAPk", ".apk", apkFile);
            return ratelApkService.uploadInternal(loginedUser, targetFile);
        } catch (IOException e) {
            log.error("get a upload temp file failed!!", e);
            return ReturnUtil.failed(e);
        }
    }


    @ApiOperation("查看apk文件列表")
    @LoginRequired
    @GetMapping("listApks")
    @ResponseBody
    @ApiImplicitParams(
            @ApiImplicitParam(name = "apkType", value = "查询类型,可不传,可选值（null,1,2）", dataType = "integer", paramType = "query", allowableValues = "null,1,2")
    )
    public CommonRes<Page<RatelApkVo>> listApks(
            @RequestParam(required = false) Integer apkType,
            @RequestParam(required = false) String appPackage) {
        RatelUser ratelUser = LoginInterceptor.getSessionUser();

        QueryWrapper<RatelApk> queryWrapper = new QueryWrapper<>();
        if (apkType != null) {
            if (apkType == 1) {
                queryWrapper = queryWrapper.eq(RatelApk.IS_XPOSED_MODULE, true);
            } else {
                queryWrapper = queryWrapper.eq(RatelApk.IS_XPOSED_MODULE, false);
            }

        }
        if (StringUtils.isNotBlank(appPackage)) {
            queryWrapper = queryWrapper.eq(RatelApk.APP_NAME, appPackage);
        }

        //TODO
        queryWrapper = queryWrapper.inSql(RatelApk.ID, "select apk_id from ratel_user_apk where user_id='" + ratelUser.getId() + "'");


        List<RatelApk> ratelApks = ratelApkMapper.selectList(queryWrapper);


        Set<Long> ids = new HashSet<>();

        for (RatelApk ratelApk : ratelApks) {
            ids.add(ratelApk.getId());
        }

        if (ids.size() == 0) {
            return CommonRes.success(new PageImpl<>(Collections.emptyList()));
        }


        List<RatelUserApk> ratelUserApks = ratelUserApkMapper.selectList(new QueryWrapper<RatelUserApk>()
                .in(RatelUserApk.APK_ID, ids)
                .eq(RatelUserApk.USER_ID, ratelUser.getId())
        );

        final Map<Long, RatelUserApk> ratelUserApkMap = new HashMap<>();
        for (RatelUserApk ratelUserApk : ratelUserApks) {
            ratelUserApkMap.put(ratelUserApk.getApkId(), ratelUserApk);
        }


        List<RatelApkVo> collect = ratelApks.stream()
                .map(
                        ratelApk -> RatelApkVo.transform(ratelApk, ratelUser, ratelUserApkMap.get(ratelApk.getId()), aliOSSHelper)
                ).sorted(
                        (o1, o2) -> o2.getId().compareTo(o1.getId())
                ).collect(
                        Collectors.toList()
                );

        return CommonRes.success(new PageImpl<>(
                collect
        ));
    }


    @ApiOperation("通过文件hash查询文件记录")
    @LoginRequired
    @GetMapping("apkDetail")
    @ResponseBody
    public CommonRes<RatelApkVo> findByHash(String fileHash) {
        if (StringUtils.isBlank(fileHash)) {
            return CommonRes.failed("param can not be empty!!");
        }
        RatelApk ratelApk = ratelApkMapper.selectOne(new QueryWrapper<RatelApk>().eq(RatelApk.FILE_HASH, fileHash));
        if (ratelApk == null) {
            return CommonRes.failed("record not exist!!");
        }

        RatelUser ratelUser = LoginInterceptor.getSessionUser();

        RatelUserApk ratelUserApk = ratelUserApkMapper.selectOne(new QueryWrapper<RatelUserApk>().eq(RatelUserApk
                .USER_ID, ratelUser.getId()).eq(RatelUserApk.APK_ID, ratelApk.getId()));
        if (ratelUserApk == null) {
            return CommonRes.failed("record not exist!!");
        }
        return CommonRes.success(RatelApkVo.transform(ratelApk, ratelUser, ratelUserApk, aliOSSHelper));
    }

    @ApiOperation("支持根据文件别名模糊搜索")
    @LoginRequired
    @GetMapping("searchByAlias")
    @ResponseBody
    public CommonRes<List<RatelApkVo>> searchByAlias(String alias) {
        if (StringUtils.length(alias) < 2) {
            //太少了，就不让模糊查询
            return CommonRes.success(Collections.emptyList());
        }
        RatelUser ratelUser = LoginInterceptor.getSessionUser();
        List<RatelUserApk> ratelUserApks = ratelUserApkMapper.selectList(new QueryWrapper<RatelUserApk>()
                .eq(RatelUserApk.USER_ID, ratelUser.getId())
                .like(RatelUserApk.ALIAS, "%" + alias.replaceAll("%", "") + "%")
                .last(" limit 15")
        );

        Map<Long, RatelUserApk> ratelApkIds = new HashMap<>();
        for (RatelUserApk ratelUserApk : ratelUserApks) {
            ratelApkIds.put(ratelUserApk.getApkId(), ratelUserApk);
        }

        List<RatelApk> ratelApks = ratelApkMapper.selectList(new QueryWrapper<RatelApk>()
                .in(RatelUser.ID, ratelApkIds.keySet()));
        return CommonRes.success(ratelApks.stream().map(input -> RatelApkVo.transform(input, ratelUser, ratelApkIds.get(input.getId()), aliOSSHelper)).collect(Collectors.toList()));
    }
}
