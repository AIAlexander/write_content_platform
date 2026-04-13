package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.config.OssConfig;
import cloud.alex.writecontentplatform.service.FileService;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author wangshuhao
 * @date 2026/4/8
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private OssConfig ossConfig;

    @Resource
    private OSS ossClient;


    @Override
    public String uploadLocalFile(String localPath, String ossName) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossConfig.getBucketName(), ossName, new File(localPath));
            ossClient.putObject(putObjectRequest);
            String fileUrl = getFileUrl(ossName);
            log.info("文件上传OSS成功，本地路径：{}， url：{}", localPath, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return null;
        }
    }

    @Override
    public String getFileUrl(String ossName) {
        // 去掉 https://
        String host = ossConfig.getEndpoint()
                .replace("https://", "")
                .replace("http://", "");

        return "https://" + ossConfig.getBucketName() + "." + host + "/" + ossName;
    }
}
