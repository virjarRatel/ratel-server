package com.virjar.ratel.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * apk处理任务
 * </p>
 *
 * @author virjar
 * @since 2019-09-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_task")
@ApiModel(value = "RatelTask对象", description = "apk处理任务")
public class RatelTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属用户")
    private Long userId;

    @ApiModelProperty(value = "原始apk")
    private Long originApkId;

    @ApiModelProperty(value = "xposed模块apk，非embed模式下，这个可以为空")
    private Long xposedModuleApkId;

    @ApiModelProperty(value = "是否在apk文件中增加debug的开关")
    private Boolean addDebugFlag;

    @ApiModelProperty(value = "使用的引擎，选择appendDex|rebuildDex|shell三个之一,默认使用rebuildDex")
    private String ratelEngine;

    @ApiModelProperty(value = "使用的授权证书，如果没有证书，那么无法构建apk")
    private Long certificateId;

    @ApiModelProperty(value = "任务构建日志将会上传到oss")
    private String logOssUrl;

    @ApiModelProperty(value = "task输出产物是一个apk，将会上传到oss")
    private String outputOssUrl;

    @ApiModelProperty(value = "任务创建时间")
    private Date addTime;

    @ApiModelProperty(value = "任务完成时间")
    private Date finishTime;

    @ApiModelProperty(value = "当前任务状态,{ratelTaskStatusInit = 0,ratelTaskStatusRunning = 1,ratelTaskStatusFailed = 2,ratelTaskStatusSuccess = 3,ratelTaskStatusUploading = 4,rateltaskStatusBadcase = 5}")
    private Integer taskStatus;

    @ApiModelProperty(value = "任务消费主机，在多机部署的时候，考虑任务被不同机器消费。避免冲突")
    private String consumer;

    @ApiModelProperty(value = "打包的附加参数")
    private String extParam;

    @ApiModelProperty(value = "apk中提取的name label,迁移自ratel_apk")
    private String appName;

    @ApiModelProperty(value = "apk中提取的版本号,迁移自ratel_apk")
    private String appVersion;

    @ApiModelProperty(value = "apk中提取的版本号的数字号码,迁移自ratel_apk")
    private Long appVersionCode;

    @ApiModelProperty(value = "任务备注")
    private String comment;

    @ApiModelProperty(value = "apk的package,迁移自ratel_apk")
    private String appPackage;

    @ApiModelProperty(value = "引擎版本")
    private String ratelVersion;


    @ApiModelProperty(value = "是否导出")
    private Boolean needExport;

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String ORIGIN_APK_ID = "origin_apk_id";

    public static final String XPOSED_MODULE_APK_ID = "xposed_module_apk_id";

    public static final String ADD_DEBUG_FLAG = "add_debug_flag";

    public static final String RATEL_ENGINE = "ratel_engine";

    public static final String CERTIFICATE_ID = "certificate_id";

    public static final String LOG_OSS_URL = "log_oss_url";

    public static final String OUTPUT_OSS_URL = "output_oss_url";

    public static final String ADD_TIME = "add_time";

    public static final String FINISH_TIME = "finish_time";

    public static final String TASK_STATUS = "task_status";

    public static final String CONSUMER = "consumer";

    public static final String EXT_PARAM = "ext_param";

    public static final String APP_NAME = "app_name";

    public static final String APP_VERSION = "app_version";

    public static final String APP_VERSION_CODE = "app_version_code";

    public static final String COMMENT = "comment";

    public static final String APP_PACKAGE = "app_package";

    public static final String RATEL_VERSION = "ratel_version";

    public static final String NEED_EXPORT = "need_export";
}
