package com.virjar.ratel.server.util;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.virjar.ratel.server.vo.CommonRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Created by virjar on 2018/1/17.<br>
 */
public class ReturnUtil {
    public static <T> CommonRes<T> failed(String message) {
        return failed(message, status_other);
    }


    public static <T> CommonRes<T> failed(Exception exception) {
        return failed(CommonUtil.translateSimpleExceptionMessage(exception), status_other);
    }

    public static <T> CommonRes<T> failed(String message, int status) {
        return new CommonRes<>(status, message, null, null);
    }

    public static <T> CommonRes<T> success(T t) {
        return new CommonRes<>(status_success, "success", t, null);
    }

    public static <T> CommonRes<T> failed(CommonRes<?> source) {
        return failed(source.getMessage(), source.getStatus());
    }

    public static <T> CommonRes<Page<T>> returnPage(IPage<T> iPage, Pageable pageable) {
        Page<T> ts = new PageImpl<>(iPage.getRecords(), pageable, iPage.getTotal());
        return success(ts);
    }

    public static final int status_other = -1;
    public static final int status_success = 0;
    public static final int status_timeout = 1;
}
