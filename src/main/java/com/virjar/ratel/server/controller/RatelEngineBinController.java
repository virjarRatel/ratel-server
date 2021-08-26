package com.virjar.ratel.server.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.virjar.ratel.server.entity.RatelEngineBin;
import com.virjar.ratel.server.mapper.RatelEngineBinMapper;
import com.virjar.ratel.server.service.RatelEngineBinService;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.util.ReturnUtil;
import com.virjar.ratel.server.vo.CommonRes;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * ratel构建引擎二进制发布包 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@RestController
@RequestMapping("/api/ratel/engine-bin")
@Slf4j
public class RatelEngineBinController {

    @Resource
    private RatelEngineBinService ratelEngineBinService;

    @Resource
    private RatelEngineBinMapper ratelEngineBinMapper;

    @ApiOperation("上传ratel builder发布包文件")
    @PostMapping("/upload")
    @ResponseBody
    @LoginRequired(forAdmin = true)
    public CommonRes<RatelEngineBin> uploadApk(MultipartFile ratelBuilderZip) {
        // RatelUser loginedUser = LoginInterceptor.getSessionUser();
        String srcFileName = ratelBuilderZip.getOriginalFilename();
        if (!StringUtils.endsWithIgnoreCase(srcFileName, ".zip")) {
            return ReturnUtil.failed("the upload file must has .zip suffix");
        }

        File targetFile;
        try {
            targetFile = File.createTempFile("ratelBuilder", ".zip");
            return ratelEngineBinService.uploadInternal(ratelBuilderZip, targetFile);
        } catch (IOException e) {
            log.error("get a upload temp file failed!!", e);
            return ReturnUtil.failed(e);
        }
    }

    @ApiOperation("查询最后十个版本的记录")
    @GetMapping("/listLastTen")
    @ResponseBody
    @LoginRequired(forAdmin = true)
    public CommonRes<List<RatelEngineBin>> listLastTen() {
        return CommonRes.success(ratelEngineBinMapper.selectList(new QueryWrapper<RatelEngineBin>().orderByDesc(RatelEngineBin.ENGINE_VERSION_CODE).last(" limit 10")));
    }

    @ApiOperation("启用某个版本的发布包")
    @GetMapping("/enableEngine")
    @ResponseBody
    @LoginRequired(forAdmin = true)
    public CommonRes<String> updateEngineStatus(String bindMd5) {
        RatelEngineBin ratelEngineBin = ratelEngineBinMapper.selectOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.FILE_HASH, bindMd5));
        if (ratelEngineBin == null) {
            return CommonRes.failed("record not exist");
        }

        if (ratelEngineBin.getEnabled()) {
            return CommonRes.success("ok");
        }
        List<RatelEngineBin> ratelEngineBins = ratelEngineBinMapper.selectList(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.ENABLED, true));
        //disable all
        for (RatelEngineBin needDisable : ratelEngineBins) {
            needDisable.setEnabled(false);
            ratelEngineBinMapper.updateById(needDisable);
        }
        ratelEngineBin.setEnabled(true);
        ratelEngineBinMapper.updateById(ratelEngineBin);
        return CommonRes.success("ok");
    }
}
