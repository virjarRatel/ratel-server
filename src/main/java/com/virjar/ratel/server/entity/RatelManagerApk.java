package com.virjar.ratel.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * ratelManager的发布包
 * </p>
 *
 * @author virjar
 * @since 2019-09-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_manager_apk")
@ApiModel(value="RatelManagerApk对象", description="ratelManager的发布包")
public class RatelManagerApk implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "apk文件hash值")
    private String fileHash;

    @ApiModelProperty(value = "本apk需要上传到oss，用以实现和服务器环境无关的序列化")
    private String ossUrl;

    @ApiModelProperty(value = "上传时间")
    private LocalDateTime uploadTime;

    @ApiModelProperty(value = "apk中提取的版本号")
    private String appVersion;

    @ApiModelProperty(value = "apk中提取的版本号的数字号码")
    private Long appVersionCode;


    public static final String ID = "id";

    public static final String FILE_HASH = "file_hash";

    public static final String OSS_URL = "oss_url";

    public static final String UPLOAD_TIME = "upload_time";

    public static final String APP_VERSION = "app_version";

    public static final String APP_VERSION_CODE = "app_version_code";

}
