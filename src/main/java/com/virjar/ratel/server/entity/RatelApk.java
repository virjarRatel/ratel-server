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
 * 上传的apk文件
 * </p>
 *
 * @author virjar
 * @since 2019-08-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_apk")
@ApiModel(value = "RatelApk对象", description = "上传的apk文件")
public class RatelApk implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "文件名称，一般为用户上传的时候设定的")
    private String fileName;

    @ApiModelProperty(value = "文件hash，用于唯一定位apk内容")
    private String fileHash;

    @ApiModelProperty(value = "apk的package")
    private String appPackage;

    @ApiModelProperty(value = "apk中提取的name label")
    private String appName;

    @ApiModelProperty(value = "apk中提取的版本号")
    private String appVersion;

    @ApiModelProperty(value = "apk中提取的版本号的数字号码")
    private Long appVersionCode;

    @ApiModelProperty(value = "本apk需要上传到oss，用以实现和服务器环境无关的序列化")
    private String ossUrl;

    @ApiModelProperty(value = "上传时间")
    private Date uploadTime;

    @ApiModelProperty(value = "最后访问时间")
    private Date lastUsedTime;

    @ApiModelProperty(value = "当前apk是否为xposed模块，在前端可以通过这个字段对apk分组")
    private Boolean isXposedModule;


    public static final String ID = "id";

    public static final String FILE_NAME = "file_name";

    public static final String FILE_HASH = "file_hash";

    public static final String APP_PACKAGE = "app_package";

    public static final String APP_NAME = "app_name";

    public static final String APP_VERSION = "app_version";

    public static final String APP_VERSION_CODE = "app_version_code";

    public static final String OSS_URL = "oss_url";

    public static final String UPLOAD_TIME = "upload_time";

    public static final String LAST_USED_TIME = "last_used_time";

    public static final String IS_XPOSED_MODULE = "is_xposed_module";

}
