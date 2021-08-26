package com.virjar.ratel.server.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.virjar.ratel.server.entity.RatelHotModule;
import com.virjar.ratel.server.entity.RatelManagerApk;
import com.virjar.ratel.server.entity.RatelTask;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.mapper.RatelHotModuleMapper;
import com.virjar.ratel.server.mapper.RatelManagerApkMapper;
import com.virjar.ratel.server.mapper.RatelTaskMapper;
import com.virjar.ratel.server.mapper.RatelUserMapper;
import com.virjar.ratel.server.service.AliOSSHelper;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
public class ShowExportTaskController {

    @Resource
    private RatelUserMapper ratelUserMapper;

    @Resource
    private RatelTaskMapper ratelTaskMapper;

    @Resource
    private AliOSSHelper aliOSSHelper;

    @Resource
    private RatelManagerApkMapper ratelManagerApkMapper;

    @Resource
    private RatelHotModuleMapper ratelHotModuleMapper;

    @ApiOperation("查询任务详情")
    @GetMapping("/export/{userName}")
    @ResponseBody
    public String showExportApk(@PathVariable String userName) {

        RatelUser ratelUser = ratelUserMapper.selectOne(new QueryWrapper<RatelUser>().eq(RatelUser.ACCOUNT, userName));
        if (ratelUser == null) {
            return "用户不存在!!";
        }

        List<RatelTask> ratelTasks = ratelTaskMapper.selectList(new QueryWrapper<RatelTask>().eq(
                RatelTask.USER_ID, ratelUser.getId())
                .eq(RatelTask.NEED_EXPORT, true)
                .orderByDesc(RatelTask.ID)
                .last("limit 6")
        );

        JSONArray jsonArray = new JSONArray();
        ShowItem rmShowItem = renderRMLink();
        if (rmShowItem != null) {
            jsonArray.add(rmShowItem);
        }

        int i = 1;
        for (RatelTask ratelTask : ratelTasks) {
            ratelTask.setOutputOssUrl(aliOSSHelper.genAccessUrl(ratelTask.getOutputOssUrl()));
            ShowItem showItem = renderRatelTaskLink(ratelTask, i++);
            if (showItem != null) {
                jsonArray.add(showItem);
            }
        }

        List<RatelHotModule> ratelHotModules = ratelHotModuleMapper.selectList(
                new QueryWrapper<RatelHotModule>().eq(
                        RatelHotModule.USER_ID, ratelUser.getId()
                ).eq(RatelHotModule.ENABLE, true)
                        .orderByDesc(RatelHotModule.UPLOAD_TIME)
                        .last("limit 5")
        );
        for (RatelHotModule ratelHotModule : ratelHotModules) {
            ratelHotModule.setOssUrl(aliOSSHelper.genAccessUrl(ratelHotModule.getOssUrl()));
            ShowItem showItem = renderHotModule(ratelHotModule, i++);
            if (showItem != null) {
                jsonArray.add(showItem);
            }
        }

        return htmlBegin + jsonArray.toJSONString() + htmlFoot;
    }

    /**
     * 导出RM下载连接
     */
    private ShowItem renderRMLink() {
        RatelManagerApk ratelManagerApk = ratelManagerApkMapper.selectOne(new QueryWrapper<RatelManagerApk>()
                .orderByDesc(RatelManagerApk.APP_VERSION_CODE).last(" limit 1"));
        if (ratelManagerApk == null) {
            return null;
        }
        ratelManagerApk.setOssUrl(aliOSSHelper.genAccessUrl(ratelManagerApk.getOssUrl()));

        ShowItem showItem = new ShowItem();
        showItem.setUrl(ratelManagerApk.getOssUrl());
        showItem.setTitle("0.RM:" + ratelManagerApk.getAppVersion() + "_" + ratelManagerApk.getAppVersionCode());
        showItem.setSub("");
        return showItem;
    }

    @Data
    private static class ShowItem {
        private String url;
        private String title;
        private String sub;
    }

    private ShowItem renderHotModule(RatelHotModule ratelHotModule, int i) {
        if (StringUtils.isBlank(ratelHotModule.getOssUrl())) {
            return null;
        }
        ShowItem showItem = new ShowItem();
        showItem.setUrl(ratelHotModule.getOssUrl());
        showItem.setTitle(i + ".插件:" + ratelHotModule.getModulePkgName());

        String sb = ratelHotModule.getModuleVersion() +
                "_" + ratelHotModule.getModuleVersionCode() +
                "_" + new DateTime(ratelHotModule.getUploadTime()).toString("yyyy-MM-dd HH:mm:ss");
        showItem.setSub(sb);

        return showItem;


    }

    private ShowItem renderRatelTaskLink(RatelTask ratelTask, int i) {
        if (ratelTask.getTaskStatus() != 3) {
            return null;
        }
        ShowItem showItem = new ShowItem();
        showItem.setUrl(ratelTask.getOutputOssUrl());
        showItem.setTitle(i + ".感染apk:" + ratelTask.getAppName());

        String sb = ratelTask.getAppPackage() +
                "_" + ratelTask.getAppVersion() +
                "_" + ratelTask.getRatelEngine() +
                "_" + ratelTask.getRatelVersion() +
                "_" + new DateTime(ratelTask.getFinishTime()).toString("yyyy-MM-dd HH:mm:ss");
        showItem.setSub(sb);

        return showItem;
    }

    private static final String htmlBegin = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/><meta name=\"viewport\"content=\"width=device-width, initial-scale=1.0\"/><title>ratel外放apk列表</title><style>*{margin:0;padding:0}.container{list-style:none;width:100vw;height:100vh;display:flex;flex-direction:column}.container li{flex:1;display:flex;align-items:center;justify-content:center;min-height:20vh}.container li a{font-size:20px;text-decoration:none;color:#673ab7;white-space:pre-wrap;word-break:break-all;padding:10px;text-align:center}.container li a small{display:block}.container li:nth-child(odd)a{color:#009688}.container li:nth-child(even){background:#eeeeee}</style></head><body><ul class=\"container\"id=\"apks\"/><script>\n" +
            "\tlet apks=";

    private static final String htmlFoot = ";let tpl=\"\";for(let apk of apks){tpl+=`<li><a href=\"${apk.url}\"target=\"_blank\">${apk.title}<small>${apk.sub}</small></a></li>`}\n" +
            "document.querySelector(\"#apks\").innerHTML=tpl;</script></body></html>";
}
