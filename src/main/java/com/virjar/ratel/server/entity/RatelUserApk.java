package com.virjar.ratel.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 某个用户下面的apk，因为apk可以被不同用户重复上传，所以用户见到的是一个文件映射
 * </p>
 *
 * @author virjar
 * @since 2019-08-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_user_apk")
@ApiModel(value = "RatelUserApk对象", description = "某个用户下面的apk，因为apk可以被不同用户重复上传，所以用户见到的是一个文件映射")
public class RatelUserApk implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属用户")
    private Long userId;

    @ApiModelProperty(value = "apk文件id")
    private Long apkId;

    @ApiModelProperty(value = "上传文件名称")
    private String apkFileName;

    @ApiModelProperty(value = "别名")
    private String alias;


    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String APK_ID = "apk_id";

    public static final String APK_FILE_NAME = "apk_file_name";

    public static final String ALIAS = "alias";

}
