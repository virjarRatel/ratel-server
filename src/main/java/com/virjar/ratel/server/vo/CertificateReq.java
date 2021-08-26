package com.virjar.ratel.server.vo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author by fury.
 * version 2019/12/5.
 */
@Data
public class CertificateReq {

    @ApiModelProperty(value = "必须. 账户: 一个用户可以拥有多个证书")
    private String account;

    @ApiModelProperty(value = "必须. 过期时间格式: yyyy-MM-dd")
    private String expireDate;

    @ApiModelProperty(value = "升级接口必须. 证书内容")
    private String certificateContent;

    @ApiModelProperty(value = "不用填, 根据之前版本号生成. 证书版本号")
    private Integer licenceVersion;

    @ApiModelProperty(value = "非必须. 包名限定")
    private String[] packageList;

    @ApiModelProperty(value = "非必须. 设备号限定")
    private String[] deviceList;

    @Override
    public String toString() {
        String simpleCertificate = "";
        if (!Strings.isNullOrEmpty(certificateContent)) {
            String start = certificateContent.substring(0, 10);
            int length = certificateContent.length();
            String end = certificateContent.substring(length - 10, length);
            simpleCertificate = start + "..." + end;
        }
        return MoreObjects.toStringHelper(this)
                .add("account", account)
                .add("expireDate", expireDate)
                .add("simpleCertificate", simpleCertificate)
                .add("licenceVersion", licenceVersion)
                .toString();
    }
}
