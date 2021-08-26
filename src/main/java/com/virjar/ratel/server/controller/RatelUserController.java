package com.virjar.ratel.server.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.virjar.ratel.server.entity.RatelApk;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.mapper.RatelUserMapper;
import com.virjar.ratel.server.system.LoginInterceptor;
import com.virjar.ratel.server.system.LoginRequired;
import com.virjar.ratel.server.util.CommonUtil;
import com.virjar.ratel.server.util.ReturnUtil;
import com.virjar.ratel.server.vo.CommonRes;
import com.virjar.ratel.server.vo.RatelApkVo;
import com.virjar.ratel.server.vo.RatelPage;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@RestController
@RequestMapping("/api/ratel/user")
public class RatelUserController {

    @Resource
    private RatelUserMapper ratelUserMapper;

    @Value("${openRegister}")
    private boolean openRegister;

    @ApiOperation("用户登录")
    @PostMapping("/login")
    @ResponseBody
    public CommonRes<String> login(String userName, String password) {
        RatelUser ratelUser = ratelUserMapper.selectOne(new QueryWrapper<RatelUser>().eq(RatelUser.ACCOUNT, userName));
        if (ratelUser == null) {
            return CommonRes.failed("user not exist");
        }
        if (!ratelUser.getPwd().equals(password)) {
            return CommonRes.failed("password error!!");
        }
        ratelUser.setLoginToken(UUID.randomUUID().toString());
        ratelUser.setLastActive(new Date());
        ratelUserMapper.updateById(ratelUser);

        CommonRes<String> ret = CommonRes.success("OK");
        ret.setToken(ratelUser.getLoginToken());
        return ret;
    }


    @ApiOperation("用户注册")
    @PostMapping("/register")
    @ResponseBody
    public CommonRes<String> register(String userName, String password) {
        if (!openRegister) {
            return CommonRes.failed("user register not opened");
        }
        RatelUser ratelUser = ratelUserMapper.selectOne(new QueryWrapper<RatelUser>().eq(RatelUser.ACCOUNT, userName));
        if (ratelUser != null) {
            return CommonRes.failed("user exist");
        }
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
            return CommonRes.failed("param empty!!");
        }
        if (userName.length() > 20 || password.length() > 50) {
            return CommonRes.failed("username or password length limited!!");
        }

        ratelUser = new RatelUser();
        ratelUser.setAccount(userName);
        ratelUser.setNickName(userName);
        ratelUser.setPwd(password);
        ratelUser.setBalance(0L);
        ratelUser.setUserLevel(1);
        if (userName.equals("ratel_admin")) {
            ratelUser.setIsAdmin(true);
        }
        ratelUser.setLoginToken(UUID.randomUUID().toString());
        ratelUser.setLastActive(new Date());
        ratelUserMapper.insert(ratelUser);

        CommonRes<String> ret = CommonRes.success("OK");
        ret.setToken(ratelUser.getLoginToken());
        return ret;
    }

    @ApiOperation("查询当前用户信息")
    @GetMapping("/userMode")
    @ResponseBody
    @LoginRequired
    public CommonRes<RatelUser> nowUser() {
        RatelUser sessionUser = LoginInterceptor.getSessionUser();
        sessionUser.setPwd(null);
        return CommonRes.success(sessionUser);
    }

    @ApiOperation("修改用户密码")
    @PostMapping("/updatePwd")
    @ResponseBody
    @LoginRequired
    public CommonRes<String> forgetPassword(String oldPassword, String newPassword) {
        RatelUser sessionUser = LoginInterceptor.getSessionUser();
        if (!sessionUser.getPwd().equals(oldPassword)) {
            return CommonRes.failed("password error!! please contact administrator");
        }
        if (StringUtils.isBlank(newPassword)) {
            return CommonRes.failed("new password empty");
        }
        sessionUser.setPwd(newPassword.trim());
        ratelUserMapper.updateById(sessionUser);
        return CommonRes.success("ok");
    }

    @ApiOperation("修改昵称")
    @PostMapping("/updateNickName")
    @ResponseBody
    @LoginRequired
    public CommonRes<String> updateNickName(String nickName) {
        RatelUser sessionUser = LoginInterceptor.getSessionUser();
        if (StringUtils.isBlank(nickName)) {
            return CommonRes.failed("param empty");
        }
        sessionUser.setNickName(nickName.trim());
        ratelUserMapper.updateById(sessionUser);
        return CommonRes.success("ok");
    }


    @ApiOperation("展示用户")
    @LoginRequired(forAdmin = true)
    @GetMapping("listUsers")
    @ResponseBody
    public CommonRes<List<RatelUser>> listUsers(@RequestParam(required = false) String account) {

        QueryWrapper<RatelUser> queryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(account)) {
            queryWrapper = queryWrapper.like(RatelUser.ACCOUNT, "%" + account + "%");
        }

        return CommonRes.success(ratelUserMapper.selectList(queryWrapper));
    }

    //@ApiOperation(hidden = true)
    @LoginRequired(forAdmin = true)
    @GetMapping("setRegistered")
    public CommonRes<String> setUserRegisterStatus(boolean canRegister) {
        openRegister = canRegister;
        return CommonRes.success("ok");
    }

    @LoginRequired(forAdmin = true)
    @GetMapping("queryLoginToken")
    @ApiOperation("查询登陆token，仅限管理员用户支持")
    public CommonRes<String> queryLoginToken(String userName) {
        RatelUser ratelUser = ratelUserMapper.selectOne(new QueryWrapper<RatelUser>().eq(RatelUser.ACCOUNT, userName));
        if (ratelUser == null) {
            return CommonRes.failed("user not exist");
        }

        if (StringUtils.isBlank(ratelUser.getLoginToken())) {
            ratelUser.setLoginToken(UUID.randomUUID().toString());
        }
        ratelUser.setLastActive(new Date());
        ratelUserMapper.updateById(ratelUser);
        return CommonRes.success(ratelUser.getLoginToken());
    }
}
