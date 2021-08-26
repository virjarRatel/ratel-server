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
 * 授权证书
 * </p>
 *
 * @author virjar
 * @since 2019-08-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_certificate")
@ApiModel(value = "RatelCertificate对象", description = "授权证书")
public class RatelCertificate implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属用户")
    private Long userId;

    @ApiModelProperty(value = "证书内容")
    private String content;

    @ApiModelProperty(value = "证书id，来自content解码")
    private String licenceId;

    @ApiModelProperty(value = "证书版本号，来自content解码")
    private Integer licenceVersionCode;


    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String CONTENT = "content";

    public static final String LICENCE_ID = "licence_id";

    public static final String LICENCE_VERSION_CODE = "licence_version_code";

}
