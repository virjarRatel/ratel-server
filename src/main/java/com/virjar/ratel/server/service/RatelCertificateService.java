package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.virjar.ratel.server.entity.RatelCertificate;
import com.virjar.ratel.server.mapper.RatelCertificateMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 授权证书 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@Service
public class RatelCertificateService extends ServiceImpl<RatelCertificateMapper, RatelCertificate> implements IService<RatelCertificate> {

}
