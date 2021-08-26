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
 * 用户
 * </p>
 *
 * @author virjar
 * @since 2019-09-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ratel_user")
@ApiModel(value = "RatelUser对象", description = "用户")
public class RatelUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "账号")
    private String account;

    @ApiModelProperty(value = "密码")
    private String pwd;

    @ApiModelProperty(value = "用户等级")
    private Integer userLevel;

    @ApiModelProperty(value = "余额")
    private Long balance;

    @ApiModelProperty(value = "用户登陆的token，api访问的凭证")
    private String loginToken;

    @ApiModelProperty(value = "用户最后活跃时间")
    private Date lastActive;

    @ApiModelProperty(value = "是否是管理员")
    private Boolean isAdmin;

    @ApiModelProperty(value = "用户昵称")
    private String nickName;


    public static final String ID = "id";

    public static final String ACCOUNT = "account";

    public static final String PWD = "pwd";

    public static final String USER_LEVEL = "user_level";

    public static final String BALANCE = "balance";

    public static final String LOGIN_TOKEN = "login_token";

    public static final String LAST_ACTIVE = "last_active";

    public static final String IS_ADMIN = "is_admin";

    public static final String NICK_NAME = "nick_name";

}
