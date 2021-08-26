package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.virjar.ratel.server.entity.RatelManagerApk;
import com.virjar.ratel.server.mapper.RatelManagerApkMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * ratelManager的发布包 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2019-09-02
 */
@Service
public class RatelManagerApkService extends ServiceImpl<RatelManagerApkMapper, RatelManagerApk> implements IService<RatelManagerApk> {

}
