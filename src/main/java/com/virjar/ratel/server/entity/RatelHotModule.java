package com.virjar.ratel.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * ratel热发模块
 * </p>
 *
 * @author virjar
 * @since 2020-02-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_hot_module")
@ApiModel(value = "RatelHotModule对象", description = "ratel热发模块")
public class RatelHotModule implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "apk文件hash值")
    private String fileHash;

    @ApiModelProperty(value = "本apk需要上传到oss，用以实现和服务器环境无关的序列化")
    private String ossUrl;

    @ApiModelProperty(value = "上传时间")
    private Date uploadTime;

    @ApiModelProperty(value = "该模块对应的包名")
    private String modulePkgName;

    @ApiModelProperty(value = "apk中提取的版本号")
    private String moduleVersion;

    @ApiModelProperty(value = "apk中提取的版本号的数字号码")
    private Long moduleVersionCode;

    @ApiModelProperty(value = "该模块对应的的证书id")
    private String certificateId;

    @ApiModelProperty(value = "该模块对应的app")
    private String forRatelApp;

    @ApiModelProperty(value = "可选，对应的group，解析自模块AndroidManifest.xml")
    private String ratelGroup;

    @ApiModelProperty(value = "上传用户，ratel网站上面的操作用户")
    private Long userId;

    @ApiModelProperty(value = "上传用户名称，同步自用户表")
    private String userName;

    @ApiModelProperty(value = "是否生效")
    private Boolean enable;

    @ApiModelProperty(value = "文件大小，单位KB")
    private Long fileSize;

    public static final String ID = "id";

    public static final String FILE_HASH = "file_hash";

    public static final String OSS_URL = "oss_url";

    public static final String UPLOAD_TIME = "upload_time";

    public static final String MODULE_PKG_NAME = "module_pkg_name";

    public static final String MODULE_VERSION = "module_version";

    public static final String MODULE_VERSION_CODE = "module_version_code";

    public static final String CERTIFICATE_ID = "certificate_id";

    public static final String FOR_RATEL_APP = "for_ratel_app";

    public static final String RATEL_GROUP = "ratel_group";

    public static final String USER_ID = "user_id";

    public static final String USER_NAME = "user_name";

    public static final String ENABLE = "enable";

    public static final String FILE_SIZE = "file_size";

    public static final String RATEL_GROUP_ALL = "ratel_group_all";

}
