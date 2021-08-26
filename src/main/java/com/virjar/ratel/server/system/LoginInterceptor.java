package com.virjar.ratel.server.system;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.service.RatelUserService;
import com.virjar.ratel.server.vo.CommonRes;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static ThreadLocal<RatelUser> threadLocal = new ThreadLocal<>();

    @Resource
    private RatelUserService ratelUserService;

    private static byte[] needLoginResponse = JSONObject.toJSONString(CommonRes.failed(CommonRes.statusNeedLogin, "need login")).getBytes(Charsets.UTF_8);
    private static byte[] loginExpire = JSONObject.toJSONString(CommonRes.failed(CommonRes.statusLoginExpire, "login expire")).getBytes(Charsets.UTF_8);
    private static byte[] onlyForAdminResponse = JSONObject.toJSONString(CommonRes.failed(CommonRes.statusLoginExpire, "only available for administrator")).getBytes(Charsets.UTF_8);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Method method = ((HandlerMethod) handler).getMethod();
        LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
        if (loginRequired == null) {
            return true;
        }

        String operatorToken = request.getParameter("operatorToken");
        if (StringUtils.isBlank(operatorToken)) {
            response.addHeader("content-type", "application/json; charset=utf-8");
            response.getOutputStream().write(needLoginResponse);
            return false;
        }

        RatelUser ratelUser = ratelUserService.checkLogin(operatorToken);
        if (ratelUser == null) {
            response.addHeader("content-type", "application/json; charset=utf-8");
            response.getOutputStream().write(loginExpire);
            return false;
        }

        if (loginRequired.forAdmin()) {
            if (!BooleanUtils.isTrue(ratelUser.getIsAdmin())) {
                response.addHeader("content-type", "application/json; charset=utf-8");
                response.getOutputStream().write(onlyForAdminResponse);
                return false;
            }
        }

        //request.getServletContext().setAttribute(Constant.loginUserKey, ratelUser);
        threadLocal.set(ratelUser);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        threadLocal.remove();
        // modelAndView.getModelMap()
    }

    public static RatelUser getSessionUser() {
        return threadLocal.get();
    }
}
