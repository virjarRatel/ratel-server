package com.virjar.ratel.server.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.virjar.ratel.server.entity.RatelApk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 上传的apk文件 Mapper 接口
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
public interface RatelApkMapper extends BaseMapper<RatelApk> {
   // List<RatelApk> selectUserListPage(IPage<RatelApk> page, @Param("user") RatelApk ratelApk);
}
