package com.virjar.ratel.server.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CreateRatelTaskBean {
    @ApiModelProperty(value = "待感染app，必填")
    private String originApkFileHash;

    @ApiModelProperty(value = "模块插件app（可以将插件合并到输出apk包中），可选")
    private String xposedModuleApkFileHash;

    @ApiModelProperty(value = "是否在apk文件中增加debug的开关,增加debug开关之后，apk可以被调试，不过如果存在反调试对抗那么需要手动bypass")
    private Boolean addDebugFlag = false;

    @ApiModelProperty(value = "使用的引擎，选择appendDex|rebuildDex|shell三个之一,默认使用rebuildDex，可选")
    private String ratelEngine;

    @ApiModelProperty(value = "使用的证书，必填")
    private Long certificateId;

    @ApiModelProperty(value = "扩展参数，会append到打包命令中，可选")
    private String extParam;
}
