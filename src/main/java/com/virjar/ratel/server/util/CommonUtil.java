package com.virjar.ratel.server.util;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.virjar.ratel.server.entity.RatelCertificate;
import com.virjar.ratel.server.entity.RatelUser;
import com.virjar.ratel.server.vo.CertificateVo;
import com.virjar.ratel.server.vo.RatelPage;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.w3c.dom.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by virjar on 2018/8/4.
 */
@Slf4j
public class CommonUtil {


    public static String getStackTrack(Throwable throwable) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream));
        throwable.printStackTrace(printWriter);
        return byteArrayOutputStream.toString();
    }

    public static String translateSimpleExceptionMessage(Exception exception) {
        String message = exception.getMessage();
        if (StringUtils.isBlank(message)) {
            message = exception.getClass().getName();
        }
        return message;
    }


    public static ApkMeta parseApk(File file) {
        //now parse the file
        try {
            @Cleanup ApkFile apkFile = new ApkFile(file);
            return apkFile.getApkMeta();
        } catch (IOException e) {
            file.delete();
            throw new IllegalStateException("the filed not a apk filed format");
        }
    }

    private static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";
//    private static final String FEATURE_LOAD_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
//    private static final String FEATURE_DISABLE_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
//
//    private static final String NAMESPACES =
//            "http://xml.org/sax/features/namespaces";
//
//    private static final String VALIDATION =
//            "http://xml.org/sax/features/validation";

    /**
     * @param data File to load into Document
     * @return Document
     */
    public static Document loadDocument(String data) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            //android 这里只支持NAMESPACES和VALIDATION这两个 feature
//      docFactory.setFeature(FEATURE_DISABLE_DOCTYPE_DECL, true);
//      docFactory.setFeature(FEATURE_LOAD_DTD, false);

            try {
                docFactory.setAttribute(ACCESS_EXTERNAL_DTD, " ");
                docFactory.setAttribute(ACCESS_EXTERNAL_SCHEMA, " ");
            } catch (IllegalArgumentException ex) {
                log.warn("JAXP 1.5 Support is required to validate XML");
            }

            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            // Not using the parse(File) method on purpose, so that we can control when
            // to close it. Somehow parse(File) does not seem to close the file in all cases.
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes())) {
                return docBuilder.parse(inputStream);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

//    public static RatelUser getOperatorUser(HttpServletRequest httpServletRequest) {
//        return (RatelUser) httpServletRequest.getServletContext().getAttribute(Constant.loginUserKey);
//    }

    public static Map<String, String> parseManifestMap(String androidManifestXmlString) {
        Document document = loadDocument(androidManifestXmlString);
        try {
            Map<String, String> ret = new HashMap<>();
            NodeList nodes = (NodeList) applicationMetaXpath.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();

                if (attrs != null) {
                    Node keyNode = attrs.getNamedItem("android:name");
                    if (keyNode == null) {
                        continue;
                    }
                    String key = keyNode.getNodeValue();

                    Node valueNode = attrs.getNamedItem("android:value");

                    if (valueNode == null) {
                        continue;
                    }
                    ret.put(key, valueNode.getNodeValue());
                }
            }

            return ret;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    private static XPathExpression applicationMetaXpath = compileXpath("/manifest/application/meta-data");

    private static XPathExpression compileXpath(String xpathExpression) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> RatelPage<T> wrapperPage(Pageable pageable) {
        RatelPage<T> page = new RatelPage<>();
        page.setSize(pageable.getPageSize())
                .setCurrent(pageable.getPageNumber());
        Sort sort = pageable.getSort();
        List<String> ascs = Lists.newArrayList();
        List<String> descs = Lists.newArrayList();
        for (Sort.Order order : sort) {
            if (order.getDirection() == Sort.Direction.ASC) {
                ascs.add(order.getProperty());
            } else {
                descs.add(order.getProperty());
            }
        }
        page.setAscs(ascs);
        page.setDescs(descs);
        return page;
    }

    public static CertificateVo certificateToVo(RatelCertificate certificate) {
        String containerJsonString = new String(RatelLicenceEncryptor.standardRSADecrypt(certificate.getContent()), StandardCharsets.UTF_8);
        JSONObject containerJson = JSONObject.parseObject(containerJsonString);
        CertificateVo certificateVo = containerJson.toJavaObject(CertificateVo.class);
        certificateVo.setId(certificate.getId());
        return certificateVo;
    }

}
