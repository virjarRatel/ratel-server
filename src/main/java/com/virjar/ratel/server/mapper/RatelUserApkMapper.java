package com.virjar.ratel.server.mapper;

import com.virjar.ratel.server.entity.RatelUserApk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 某个用户下面的apk，因为apk可以被不同用户重复上传，所以用户见到的是一个文件映射 Mapper 接口
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
public interface RatelUserApkMapper extends BaseMapper<RatelUserApk> {

}
