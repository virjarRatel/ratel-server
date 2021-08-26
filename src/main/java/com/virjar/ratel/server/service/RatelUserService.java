package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.mapper.RatelUserMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@Service
public class RatelUserService extends ServiceImpl<RatelUserMapper, RatelUser> implements IService<RatelUser> {
//    private Map<String, RatelUser> tokenMap = new ConcurrentHashMap<>();
//
//    public void registerUserLogin(String token, RatelUser ratelUser) {
//        tokenMap.put(token, ratelUser);
//    }
//
//    public RatelUser queryLoginUser(String token)

    public RatelUser checkLogin(String token) {
        // RatelUser ratelUser = userMapper.selectOne(new QueryWrapper<RatelUser>().eq("loginToken", operatorToken));
        RatelUser loginToken = getOne(new QueryWrapper<RatelUser>().eq(RatelUser.LOGIN_TOKEN, token));
        if (loginToken == null) {
            return null;
        }
        if (System.currentTimeMillis() - loginToken.getLastActive().getTime() > 4 * 60 * 60 * 1000) {
            return null;
        }
        loginToken.setLastActive(new Date());
        updateById(loginToken);
        return loginToken;
    }
}
