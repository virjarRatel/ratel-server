package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virjar.ratel.server.entity.*;
import com.virjar.ratel.server.mapper.RatelApkMapper;
import com.virjar.ratel.server.mapper.RatelCertificateMapper;
import com.virjar.ratel.server.mapper.RatelTaskMapper;
import com.virjar.ratel.server.util.Constant;
import com.virjar.ratel.server.util.FileFingerprinter;
import com.virjar.ratel.server.vo.CommonRes;
import com.virjar.ratel.server.vo.CreateRatelTaskBean;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * apk处理任务 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@Service
@Slf4j
public class RatelTaskService extends ServiceImpl<RatelTaskMapper, RatelTask> implements IService<RatelTask> {

    @Resource
    private RatelEngineBinService ratelEngineBinService;

    @Resource
    private OssFileCacheManager ossFileCacheManager;

    @Resource
    private RatelApkMapper ratelApkMapper;

    @Resource
    private RatelCertificateMapper ratelCertificateMapper;

    @Resource
    private AliOSSHelper aliOSSHelper;

    public CommonRes<RatelTask> createTask(RatelUser ratelUser,
                                           RatelApk originApk, RatelApk xposedModuleApk,
                                           RatelCertificate ratelCertificate, CreateRatelTaskBean createRatelTaskBean) {

        RatelTask ratelTask = new RatelTask();
        ratelTask.setUserId(ratelUser.getId());
        ratelTask.setOriginApkId(originApk.getId());
        if (xposedModuleApk != null) {
            ratelTask.setXposedModuleApkId(xposedModuleApk.getId());
        }
        ratelTask.setAddDebugFlag(createRatelTaskBean.getAddDebugFlag());
        ratelTask.setCertificateId(ratelCertificate.getId());
        ratelTask.setRatelEngine(createRatelTaskBean.getRatelEngine());
        if (StringUtils.isBlank(ratelTask.getRatelEngine())) {
            ratelTask.setRatelEngine("rebuildDex");
        }

        ratelTask.setAddTime(new Date());
        ratelTask.setExtParam(createRatelTaskBean.getExtParam());
        ratelTask.setTaskStatus(Constant.ratelTaskStatusInit);

        ratelTask.setAppName(originApk.getAppName());
        ratelTask.setAppVersion(originApk.getAppVersion());
        ratelTask.setAppVersionCode(originApk.getAppVersionCode());
        ratelTask.setAppPackage(originApk.getAppPackage());
        ratelTask.setComment("ratel task for :" + originApk.getAppPackage());

        save(ratelTask);

        return CommonRes.success(ratelTask);
    }

    public RatelTask cloneTask(RatelTask ratelTask) {

        RatelTask newRatelTask = new RatelTask();
        newRatelTask.setUserId(ratelTask.getUserId());
        newRatelTask.setOriginApkId(ratelTask.getOriginApkId());
        newRatelTask.setXposedModuleApkId(ratelTask.getXposedModuleApkId());
        newRatelTask.setAddDebugFlag(ratelTask.getAddDebugFlag());
        newRatelTask.setCertificateId(ratelTask.getCertificateId());
        newRatelTask.setRatelEngine(ratelTask.getRatelEngine());
        newRatelTask.setAddTime(new Date());
        newRatelTask.setTaskStatus(Constant.ratelTaskStatusInit);
        newRatelTask.setExtParam(ratelTask.getExtParam());

        newRatelTask.setAppName(ratelTask.getAppName());
        newRatelTask.setAppVersionCode(ratelTask.getAppVersionCode());
        newRatelTask.setAppVersion(ratelTask.getAppVersion());
        newRatelTask.setAppPackage(ratelTask.getAppPackage());
        newRatelTask.setComment(ratelTask.getComment());
        newRatelTask.setRatelVersion(ratelTask.getRatelVersion());

        save(newRatelTask);
        return newRatelTask;
    }


    @Scheduled(fixedRate = 15000)
    public void handleTask() {
        try {
            // log.info("begin execute ratel task");
            doTask();
            //log.info("end of execute ratel task");
        } catch (Exception e) {
            log.error("handle task failed", e);
        }

    }

    private void doTask() throws Exception {
        File userDirectory = FileUtils.getUserDirectory();
        RatelEngineBin ratelEngineBin = ratelEngineBinService.nowEngineDir();
        if (ratelEngineBin == null) {
            log.info("ratel engine not upload now");
            return;
        }
        File nowEngineDir = ratelEngineBin.nowEngineDir;
        //get or gen a host unique id
        File hostIdFile = new File(userDirectory, "ratel_host_id.txt");
        String hostId;
        if (hostIdFile.exists()) {
            hostId = FileUtils.readFileToString(hostIdFile, StandardCharsets.UTF_8);
        } else {
            hostId = UUID.randomUUID().toString();
            FileUtils.writeStringToFile(hostIdFile, hostId, StandardCharsets.UTF_8);
        }

        //select one task
        //first load running task ,witch be interrupted before
        RatelTask oneTask = getOne(new QueryWrapper<RatelTask>().eq(RatelTask.TASK_STATUS, Constant.ratelTaskStatusRunning).eq(RatelTask.CONSUMER, hostId).last(" limit 1"));
        if (oneTask == null) {
            // and then load task status initial normally
            oneTask = getOne(new QueryWrapper<RatelTask>().eq(RatelTask.TASK_STATUS, Constant.ratelTaskStatusInit).orderByAsc(RatelTask.ADD_TIME).last(" limit 1"));
        }
        if (oneTask == null) {
            log.info("no task need to be handle");
            return;
        }
        //mark consumer as this server
        if (oneTask.getConsumer() == null) {
            boolean succeed = update(new UpdateWrapper<RatelTask>().eq(RatelTask.ID, oneTask.getId()).isNull(RatelTask.CONSUMER).set(RatelTask.CONSUMER, hostId));
            if (!succeed) {
                //当部署的服务器非常多的时候，这里的逻辑会出现问题，会出现大量机器无法mask机器,但是很明显的是，我们这个是一个低频业务，并不会出现这种case
                log.warn("consume task failed");
                return;
            }
            oneTask = getById(oneTask.getId());
        }

        if (oneTask.getTaskStatus() != Constant.ratelTaskStatusRunning) {
            oneTask.setTaskStatus(Constant.ratelTaskStatusRunning);
            updateById(oneTask);
        }
        buildLogger = new StringBuilder();

        File certificateTempFile = File.createTempFile("ratel_certificate", ".txt");


        try {
            if (!doApkBuildTask(oneTask, nowEngineDir, certificateTempFile, ratelEngineBin.getEngineVersion())) {
                oneTask.setTaskStatus(Constant.ratelTaskStatusFailed);
                oneTask.setFinishTime(new Date());
                oneTask.setRatelVersion(ratelEngineBin.getEngineVersion());
                updateById(oneTask);
            }
        } catch (Exception e) {
            outLog("build ratel apk failed", e);
            oneTask.setTaskStatus(Constant.ratelTaskStatusFailed);
            oneTask.setFinishTime(new Date());
            updateById(oneTask);
            throw e;
        } finally {

            //remove files
            if (!certificateTempFile.delete()) {
                outLog("failed to remove ratel_certificate temp file");
            }

            File ratelBuildLog = File.createTempFile("ratel_build_log", ".log");
            FileUtils.writeStringToFile(ratelBuildLog, buildLogger.toString(), StandardCharsets.UTF_8);
            String fileMD5 = FileFingerprinter.getFileMD5(ratelBuildLog.getAbsolutePath());
            RatelTask finalOneTask = oneTask;
            aliOSSHelper.uploadToOss(fileMD5, ratelBuildLog, finalUrl -> {
                finalOneTask.setLogOssUrl(finalUrl);
                updateById(finalOneTask);
                if (!ratelBuildLog.delete()) {
                    log.warn("failed to remove temp file:{}", ratelBuildLog.getAbsoluteFile());
                }
            });
        }

    }


    private boolean doApkBuildTask(RatelTask oneTask, File nowEngineDir, File certificateTempFile, String engineVersion) throws Exception {
        //prepare file resource
        RatelApk originApk = ratelApkMapper.selectById(oneTask.getOriginApkId());
        if (originApk == null) {
            outLog("broken task config,can not get origin apk resource from task entity!!");
            return false;
        }

        outLog("load origin apk");
        File originAPK = ossFileCacheManager.forceLoadFileFromOss(originApk.getOssUrl(), originApk.getFileHash());

        File xposeModuleApk = null;
        if (oneTask.getXposedModuleApkId() != null) {
            outLog("has embed module");
            RatelApk xposedModuleApk = ratelApkMapper.selectById(oneTask.getXposedModuleApkId());
            if (xposedModuleApk == null) {
                outLog("broken task config,can not get xposed module apk resource from task entity!!");
                return false;
            }
            xposeModuleApk = ossFileCacheManager.forceLoadFileFromOss(xposedModuleApk.getOssUrl(), xposedModuleApk.getFileHash());
        }


        outLog("prepare certificate");
        RatelCertificate ratelCertificate = ratelCertificateMapper.selectById(oneTask.getCertificateId());
        if (ratelCertificate == null) {
            outLog("broken task config,can not get xposed module apk resource from task entity!!");
            return false;
        }


        outLog("write certificate content into temp file: " + certificateTempFile.getAbsolutePath() + " content: " + ratelCertificate.getContent());
        FileUtils.writeStringToFile(certificateTempFile, ratelCertificate.getContent(), StandardCharsets.UTF_8);

        File shFile = new File(nowEngineDir, "ratel.sh");
        if (!shFile.canExecute()) {
            if (shFile.setExecutable(true)) {
                outLog("set up execute permission failed");
            }
        }

        //now real doing task
        StringBuilder paramBuilder = new StringBuilder(shFile.getAbsolutePath());

        paramBuilder.append(" ");

        //-d -w /Users/virjar/.ratel-working-repkg -c /opt/ratel/res/monthly_temp.txt /Users/virjar/Downloads/app-release-190247-o_1cgmlvsk9lm0i7u13gk13ia1dijq-uid-528097.apk
        if (BooleanUtils.isTrue(oneTask.getAddDebugFlag())) {
            paramBuilder.append("-d ");
        }

        //add certificate config
        paramBuilder.append("-c ").append(certificateTempFile.getAbsolutePath()).append(" ");

        //engine config
        String ratelEngine = oneTask.getRatelEngine();
        if (StringUtils.isNotBlank(ratelEngine)) {
            paramBuilder.append("-e ").append(ratelEngine).append(" ");
        }

        File outputTempFile = File.createTempFile("ratel_output", ".apk");
        paramBuilder.append("-o ").append(outputTempFile.getAbsolutePath()).append(" ");

        if (StringUtils.isNotBlank(oneTask.getExtParam())) {
            paramBuilder.append(oneTask.getExtParam()).append(" ");
        }

        //origin apk
        paramBuilder.append(originAPK.getAbsolutePath()).append(" ");

        if (xposeModuleApk != null) {
            paramBuilder.append(xposeModuleApk.getAbsolutePath()).append(" ");
        }

        String params = paramBuilder.toString();
        outLog("the build params: " + params);

        String[] envp = new String[]{"LANG=zh_CN.UTF-8", "LANGUAGE=zh_CN.UTF-8"};
        Process process = Runtime.getRuntime().exec(params, envp, null);
        autoFillBuildLog(process.getInputStream(), "stand");
        autoFillBuildLog(process.getErrorStream(), "error");
        //process.getOutputStream().write("\nexit\n".getBytes(StandardCharsets.UTF_8));
        //wait build task 20 minutes
        if (!process.waitFor(20, TimeUnit.MINUTES)) {
            throw new IllegalStateException("task timeout");
        }


        try {
            if (new ApkFile(outputTempFile).getApkMeta() == null) {
                throw new IllegalStateException("check apk build  output failed,output apk illegal!!");
            }
        } catch (Exception e) {
            outLog("check apk build  output failed,output apk illegal!!", e);
            throw e;
        }
        outLog("apk rebuild success!!");

        oneTask.setTaskStatus(Constant.ratelTaskStatusUploading);
        updateById(oneTask);

        String fileMD5 = FileFingerprinter.getFileMD5(outputTempFile.getAbsolutePath());

        String finalUrl = aliOSSHelper.uploadFileToAliOSSWithHash(fileMD5, outputTempFile);
        outLog("upload to oss success");
        oneTask.setOutputOssUrl(finalUrl);
        oneTask.setFinishTime(new Date());
        oneTask.setTaskStatus(Constant.ratelTaskStatusSuccess);
        oneTask.setRatelVersion(engineVersion);
        updateById(oneTask);

        if (!outputTempFile.delete()) {
            log.warn("can not remove temp file:{}", outputTempFile.getAbsolutePath());
        }
        return true;


    }

    private void autoFillBuildLog(InputStream inputStream, String type) {
        new Thread("read-" + type) {
            @Override
            public void run() {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        outLog(type + " : " + line);
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    outLog("read " + type + " error", e);
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void outLog(String logContent) {
        outLog(logContent, null);
    }

    private synchronized void outLog(String logContent, Exception e) {
        buildLogger.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        buildLogger.append(" ");
        buildLogger.append(logContent);
        buildLogger.append("\n");


        if (e == null) {
            log.info(logContent);
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream);
            e.printStackTrace(new PrintWriter(outputStreamWriter));
            buildLogger.append(byteArrayOutputStream.toString());
            buildLogger.append("\n");

            log.warn(logContent, e);
        }
    }

    private StringBuilder buildLogger;


}
