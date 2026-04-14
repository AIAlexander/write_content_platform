package cloud.alex.writecontentplatform.model.dto.image;


import lombok.Builder;
import lombok.Data;

import java.util.Base64;

/**
 * @author wangshuhao
 * @date 2026/4/13
 *
 * 图片数据封装类
 * 用于统一处理不同来源的图片（字节，URL，Base64）
 *
 */
@Data
@Builder
public class ImageDto {

    /**
     * 图片字节数据
     */
    private byte[] bytes;

    /**
     * 图片url
     */
    private String url;

    /**
     * MIME类型（如：iamge/png, image/jpeg, iamge/svg+xml）
     */
    private String mimeType;

    private DataType dataType;

    public enum DataType {
        BYTES,
        URL,
        DATA_URL
    }

    public static ImageDto fromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // 判断是否为 base64 data URL
        if (url.startsWith("data:")) {
            return fromDataUrl(url);
        }

        return ImageDto.builder()
                .url(url)
                .dataType(DataType.URL)
                .build();
    }

    /**
     * 从 base64 data url创建 ImageDto
     * @param dataUrl
     * @return
     */
    public static ImageDto fromDataUrl(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            return null;
        }
        String mimeType = "image/png";
        int mimeEnd = dataUrl.indexOf(";");
        if (mimeEnd > 5) {
            mimeType = dataUrl.substring(5, mimeEnd);
        }
        return ImageDto.builder()
                .url(dataUrl)
                .mimeType(mimeType)
                .dataType(DataType.DATA_URL)
                .build();
    }

    /**
     * 从字节数据创建 ImageData
     * @param bytes
     * @param mimeType
     * @return
     */
    public static ImageDto fromBytes(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return ImageDto.builder()
                .bytes(bytes)
                .mimeType(mimeType != null ? mimeType : "image/png")
                .dataType(DataType.BYTES)
                .build();
    }

    /**
     * 获取图片字节数据
     * 如果时 data URL，会解码base64
     *
     * @return
     */
    public byte[] getImageBytes() {
        if (dataType == DataType.BYTES) {
            return bytes;
        }

        if (dataType == DataType.DATA_URL && url != null) {
            // 解析 base64 data url
            int base64start = url.indexOf(",");
            if (base64start > 0) {
                String base64Data = url.substring(base64start + 1);
                return Base64.getDecoder().decode(base64Data);
            }
        }
        return null;
    }

    /**
     * 判断是否时有效数据
     * @return
     */
    public boolean isValid() {
        return switch (dataType) {
            case BYTES -> bytes != null && bytes.length > 0;
            case URL, DATA_URL -> url != null && !url.isEmpty();
        };
    }

    public String getFileExtension() {
        if (mimeType == null) {
            return ".png";
        }
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".png";
        };

    }
}
