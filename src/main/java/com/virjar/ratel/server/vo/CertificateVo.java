package com.virjar.ratel.server.vo;

import lombok.Data;

@Data
public class CertificateVo {
    private Long id;
    private String licenceId;
    private int licenceVersion;
    private int licenceProtocolVersion;
    private long expire;
    private int licenceType;
    private String account;
    private String[] packageList;
    private String[] deviceList;
    private String extra;
    private String payload;
}
