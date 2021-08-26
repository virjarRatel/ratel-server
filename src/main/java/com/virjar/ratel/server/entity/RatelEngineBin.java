package com.virjar.ratel.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;

/**
 * <p>
 * ratel构建引擎二进制发布包
 * </p>
 *
 * @author virjar
 * @since 2019-08-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_engine_bin")
@ApiModel(value = "RatelEngineBin对象", description = "ratel构建引擎二进制发布包")
public class RatelEngineBin implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "文件hash，发布包是一个zip包")
    private String fileHash;

    @ApiModelProperty(value = "引擎版本")
    private String engineVersion;

    @ApiModelProperty(value = "引擎版本号")
    private Long engineVersionCode;

    @ApiModelProperty(value = "当前版本的引擎是否启用")
    private Boolean enabled;

    @ApiModelProperty(value = "上传到oss")
    private String ossUrl;

    @ApiModelProperty(value = "可以运行在Android上面的dex格式的engine的oss下载地址")
    private String dexEngineUrl;

    @TableField(exist = false)
    public File nowEngineDir;



    public static final String ID = "id";

    public static final String FILE_HASH = "file_hash";

    public static final String ENGINE_VERSION = "engine_version";

    public static final String ENGINE_VERSION_CODE = "engine_version_code";

    public static final String ENABLED = "enabled";

    public static final String OSS_URL = "oss_url";

    public static final String DEX_ENGINE_URL = "dex_engine_url";

}
