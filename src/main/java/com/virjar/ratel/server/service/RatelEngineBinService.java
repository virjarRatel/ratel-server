package com.virjar.ratel.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virjar.ratel.server.entity.RatelEngineBin;
import com.virjar.ratel.server.mapper.RatelEngineBinMapper;
import com.virjar.ratel.server.util.FileFingerprinter;
import com.virjar.ratel.server.util.ReturnUtil;
import com.virjar.ratel.server.vo.CommonRes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * <p>
 * ratel构建引擎二进制发布包 服务实现类
 * </p>
 *
 * @author virjar
 * @since 2019-08-27
 */
@Service
@Slf4j
public class RatelEngineBinService extends ServiceImpl<RatelEngineBinMapper, RatelEngineBin> implements IService<RatelEngineBin> {

    @Resource
    private OssFileCacheManager ossFileCacheManager;

    @Resource
    private AliOSSHelper aliOSSHelper;

    private RatelEngineBin nowUsedEngine() {
        return getOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.ENABLED, true));
    }

    RatelEngineBin nowEngineDir() throws IOException {
        //first check if has ratel engine bin
        RatelEngineBin ratelEngineBin = nowUsedEngine();
        if (ratelEngineBin == null) {
            log.info("ratel engine not upload now");
            return null;
        }

        File userDirectory = FileUtils.getUserDirectory();

        File engineDirs = new File(userDirectory, "ratelEngines");

        File nowEngineDir = new File(engineDirs, ratelEngineBin.getFileHash());
        if (!nowEngineDir.exists()) {
            File binZipFile = ossFileCacheManager.forceLoadFileFromOss(ratelEngineBin.getOssUrl(), ratelEngineBin.getFileHash());
            FileUtils.forceMkdir(nowEngineDir);

            //unzip ratel engine binary into engine dir
            ZipFile zipFile = new ZipFile(binZipFile);
            Enumeration<ZipEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    FileUtils.forceMkdir(new File(nowEngineDir, zipEntry.getName()));
                    continue;
                }
                FileOutputStream fileOutputStream = new FileOutputStream(new File(nowEngineDir, zipEntry.getName()));
                IOUtils.copy(zipFile.getInputStream(zipEntry), fileOutputStream);
                fileOutputStream.close();
            }
        }
        ratelEngineBin.nowEngineDir = nowEngineDir;
        return ratelEngineBin;
    }

    public CommonRes<RatelEngineBin> uploadInternal(MultipartFile ratelBuilderZip, File targetFile) throws IOException {
        log.info("save new ratel engine bin file to :{}", targetFile.getAbsoluteFile());
        try {
            ratelBuilderZip.transferTo(targetFile);
        } catch (IOException e) {
            log.error("failed to save ratel engine bin file", e);
            return ReturnUtil.failed(e);
        }

        Properties ratelEngineProperties = new Properties();
        boolean hasLoadProperties = false;

        File dexEngineJar = File.createTempFile("dex-builderJar", ".jar");

        try (ZipFile zipFile = new ZipFile(targetFile)) {
            Enumeration<ZipEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.contains("container-builder-repkg")) {
                    if (zipEntryName.endsWith("-dex.jar")) {
                        //这是dex格式的，需要抽取出来，最终还需要下发给手机app使用
                        InputStream builderJarInputStream = zipFile.getInputStream(zipEntry);
                        FileOutputStream fileOutputStream = FileUtils.openOutputStream(dexEngineJar);
                        IOUtils.copy(builderJarInputStream, fileOutputStream);
                        fileOutputStream.close();
                        builderJarInputStream.close();
                    } else {

                        InputStream builderJarInputStream = zipFile.getInputStream(zipEntry);
                        File builderJarTempFile = File.createTempFile("builderJar", ".jar");
                        FileOutputStream fileOutputStream = FileUtils.openOutputStream(builderJarTempFile);
                        IOUtils.copy(builderJarInputStream, fileOutputStream);
                        fileOutputStream.close();
                        builderJarInputStream.close();

                        //now extract ratel_engine.properties
                        try (ZipFile builderJarZipFile = new ZipFile(builderJarTempFile)) {
                            InputStream inputStream = builderJarZipFile.getInputStream(builderJarZipFile.getEntry("ratel_engine.properties"));
                            ratelEngineProperties.load(inputStream);
                            hasLoadProperties = true;
                            inputStream.close();
                        } finally {
                            FileUtils.forceDelete(builderJarTempFile);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("can not open zip file", e);
            return ReturnUtil.failed("can not open zip file");
        }

        if (!hasLoadProperties || !ratelEngineProperties.containsKey("ratel_engine_versionName")) {
            log.warn("can not parse ratel_engine.properties,illegal ratel engine distribution binary file");
            return ReturnUtil.failed("illegal ratel engine distribution binary file");
        }

        //now check hash
        String fileMD5 = FileFingerprinter.getFileMD5(targetFile.getAbsolutePath());
        RatelEngineBin one = getOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.FILE_HASH, fileMD5));
        if (one != null && one.getOssUrl() != null) {
            return ReturnUtil.failed("file upload already");
        }
        if (one == null) {
            one = new RatelEngineBin();
        }

        String engineVersionName = ratelEngineProperties.getProperty("ratel_engine_versionName");
        Long engineVersionCode = NumberUtils.toLong(ratelEngineProperties.getProperty("ratel_engine_versionCode"));
        RatelEngineBin equalsVersionCode = getOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.ENGINE_VERSION_CODE, engineVersionCode));
        if (equalsVersionCode != null && !equalsVersionCode.getFileHash().equals(fileMD5)) {
            return ReturnUtil.failed("duplicate engine version code,please upgrade version code for ratel engine before create ratel binary distribution!!");
        }


        ossFileCacheManager.cacheFile(targetFile, fileMD5);


        one.setFileHash(fileMD5);
        one.setEngineVersion(engineVersionName);
        one.setEngineVersionCode(engineVersionCode);

        RatelEngineBin lastEnable = getOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.ENABLED, true).orderByDesc(RatelEngineBin.ENGINE_VERSION_CODE).last(" limit 1"));
        if (lastEnable != null) {
            if (lastEnable.getEngineVersionCode() < one.getEngineVersionCode()) {
                one.setEnabled(true);
                lastEnable.setEnabled(false);
                updateById(lastEnable);
            } else {
                one.setEnabled(false);
            }
        } else {
            one.setEnabled(true);
        }

        if (one.getId() == null) {
            save(one);
        } else {
            updateById(one);
        }

        aliOSSHelper.uploadToOss(fileMD5, targetFile, finalUrl -> {
            RatelEngineBin needSetUrl = getOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.FILE_HASH, fileMD5));
            needSetUrl.setOssUrl(finalUrl);
            updateById(needSetUrl);
            if (!targetFile.delete()) {
                log.warn("failed to remove file: {}", targetFile);
            }
        });

        if (dexEngineJar.exists() && dexEngineJar.canRead() && dexEngineJar.length() > 1) {
            log.info("the engine file has dex version engine content ,uppload it ");
            String dexEngineJarFileMD5 = FileFingerprinter.getFileMD5(dexEngineJar.getAbsolutePath());
            ossFileCacheManager.cacheFile(dexEngineJar, dexEngineJarFileMD5);
            aliOSSHelper.uploadToOss(dexEngineJarFileMD5, dexEngineJar, finalUrl -> {
                RatelEngineBin needSetUrl = getOne(new QueryWrapper<RatelEngineBin>().eq(RatelEngineBin.FILE_HASH, fileMD5));
                needSetUrl.setDexEngineUrl(finalUrl);
                updateById(needSetUrl);
                if (!dexEngineJar.delete()) {
                    log.warn("failed to remove file: {}", dexEngineJar);
                }
            });
        }

        return CommonRes.success(one);
    }
}
