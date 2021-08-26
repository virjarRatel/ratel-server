package com.virjar.ratel.server.util;

/**
 * Created by virjar on 2018/8/25.
 */
public interface Constant {

    String HERMES_EXTERNAL_WRAPPER_FLAG_KEY = "hermes_target_package";

    String loginUserKey = "loginUserKey";

    int ratelTaskStatusInit = 0;
    int ratelTaskStatusRunning = 1;
    int ratelTaskStatusFailed = 2;
    int ratelTaskStatusSuccess = 3;
    int ratelTaskStatusUploading = 4;
    int rateltaskStatusBadcase = 5;

    int MAX_PKG_LENGTH = 127;

}
