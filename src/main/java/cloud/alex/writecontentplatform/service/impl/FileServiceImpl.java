package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.config.OssConfig;
import cloud.alex.writecontentplatform.model.dto.image.ImageDto;
import cloud.alex.writecontentplatform.service.FileService;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.UUID;

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

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String uploadImage(ImageDto imageDto, String folder) {
        if (imageDto == null || !imageDto.isValid()) {
            log.warn("ImageDto 无效，无法上传");
            return null;
        }
        try {
            return switch (imageDto.getDataType()) {
                case BYTES -> uploadBytes(imageDto.getBytes(), imageDto.getMimeType(), folder);
                case URL -> uploadFromUrl(imageDto.getUrl(), folder);
                case DATA_URL-> uploadFromBase64(imageDto.getImageBytes(), imageDto.getMimeType(), folder);
            };
        } catch (Exception e) {
            log.error("上传 ImageDto 失败", e);
            return null;
        }
    }

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

    private String uploadBytes(byte[] bytes, String mimeType, String folder) {
        String fileUrl = null;
        try {
            // 定义oss文件名
            String extension = getExtensionFromMimeType(mimeType);
            String fileName = folder + "/" + UUID.randomUUID() + extension;
            // 上传到oss
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(bytes.length);
                metadata.setContentType(mimeType != null ? mimeType : "image/png");
                PutObjectRequest putObjectRequest = new PutObjectRequest(ossConfig.getBucketName(), fileName, inputStream, metadata);
                ossClient.putObject(putObjectRequest);
                fileUrl = getFileUrl(fileName);
                log.info("文件上传OSS成功，地址:{}", fileUrl);
            }
            return fileUrl;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return null;
        }
    }

    private String uploadFromUrl(String url, String folder) {
        if (url == null || url.isEmpty()) {
            log.warn("图片URL为空，不能上传");
            return null;
        }

        try {
            // 先下载图片
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    // 下载失败
                    log.error("下载图片失败: {}, code={}", url, response.code());
                    return null;
                }
                byte[] imageBytes = response.body().bytes();
                String contentType = response.header("Content-Type", "image/jpeg");

                // 上传字节
                return uploadBytes(imageBytes, contentType, folder);
            }
        } catch (Exception e) {
            log.error("上传文件失败（URL）", e);
            return null;
        }
    }

    private String uploadFromBase64(byte[] bytes, String mimeType, String folder) {
        if (bytes == null || bytes.length == 0) {
            log.warn("解码 data URL 失败，无法上传");
            return null;
        }
        return uploadBytes(bytes, mimeType, folder);
    }

    /**
     * 根据 MIME 类型获取文件扩展名
     */
    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".png";
        }
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".png";
        };
    }
}
