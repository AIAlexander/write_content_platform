package cloud.alex.writecontentplatform.strategy;

import cloud.alex.writecontentplatform.model.dto.image.ImageDto;
import cloud.alex.writecontentplatform.model.dto.image.ImageRequest;
import cloud.alex.writecontentplatform.model.enums.ImageMethodEnum;
import cloud.alex.writecontentplatform.service.FileService;
import cloud.alex.writecontentplatform.service.ImageSearchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Service
@Slf4j
public class ImageServiceStrategy {

    @Resource
    private List<ImageSearchService> imageSearchServices;

    @Resource
    private FileService fileService;

    private final Map<ImageMethodEnum, ImageSearchService> serviceMap = new EnumMap<>(ImageMethodEnum.class);

    @PostConstruct
    public void init() {
        // 将ImageSearchService 实现注册到Map中
        for (ImageSearchService imageSearchService : imageSearchServices) {
            ImageMethodEnum methodEnum = imageSearchService.getMethod();
            serviceMap.put(methodEnum, imageSearchService);
            log.info("注册图片服务:{} -> {} (AI生图:{}, 降级:{})",
                    methodEnum.getValue(),
                    imageSearchService.getClass().getSimpleName(),
                    methodEnum.isAiGenerated(),
                    methodEnum.isFallback());
        }
    }

    /**
     * 获取图片并上传到 COS（推荐方法）
     * 统一处理所有图片来源的上传逻辑
     *
     * @param imageSource 图片来源
     * @param request     图片请求对象
     * @return 图片获取结果（包含 COS URL）
     */
    public ImageResult getImageAndUpload(String imageSource, ImageRequest request) {
        ImageMethodEnum method = resolveMethod(imageSource);
        ImageSearchService service = serviceMap.get(method);

        if (service == null || !service.isAvailable()) {
            log.warn("图片服务不可用: {}, 尝试降级", method);
            return handleFallbackWithUpload(request.getPosition());
        }

        try {
            // 1. 获取图片数据
            ImageDto imageData = service.getImageData(request);

            if (imageData == null || !imageData.isValid()) {
                log.warn("图片数据获取失败, 使用降级方案, method={}", method);
                return handleFallbackWithUpload(request.getPosition());
            }

            // 2. 上传到 COS
            String folder = getFolderForMethod(method);
            String cosUrl = fileService.uploadImage(imageData, folder);

            if (cosUrl != null && !cosUrl.isEmpty()) {
                log.info("图片获取并上传成功, method={}, cosUrl={}", method, cosUrl);
                return new ImageResult(cosUrl, method);
            } else {
                log.warn("图片上传 COS 失败, 使用降级方案, method={}", method);
                return handleFallbackWithUpload(request.getPosition());
            }
        } catch (Exception e) {
            log.error("获取图片并上传异常, method={}", method, e);
            return handleFallbackWithUpload(request.getPosition());
        }
    }

    /**
     * 根据图片方法获取 COS 文件夹
     */
    private String getFolderForMethod(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "pexels";
            case NANO_BANANA -> "nano-banana";
            case MERMAID -> "mermaid";
            case ICONIFY -> "iconify";
            case EMOJI_PACK -> "emoji-pack";
            case SVG_DIAGRAM -> "svg-diagram";
            case PICSUM -> "picsum";
        };
    }

    /**
     * 解析图片来源，处理未知值
     */
    private ImageMethodEnum resolveMethod(String imageSource) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(imageSource);
        if (method == null) {
            log.warn("未知的图片来源: {}, 默认使用 {}", imageSource, ImageMethodEnum.getDefaultSearchMethod());
            return ImageMethodEnum.getDefaultSearchMethod();
        }
        return method;
    }

    /**
     * 处理降级逻辑（含上传）
     */
    private ImageResult handleFallbackWithUpload(Integer position) {
        int pos = position != null ? position : 1;
        String fallbackUrl = getFallbackImage(pos);

        // 将降级图片也上传到 COS
        ImageDto fallbackData = ImageDto.fromUrl(fallbackUrl);
        String cosUrl = fileService.uploadImage(fallbackData, "fallback");

        // 如果上传失败，直接使用原始 URL
        String finalUrl = (cosUrl != null && !cosUrl.isEmpty()) ? cosUrl : fallbackUrl;
        return new ImageResult(finalUrl, ImageMethodEnum.getFallbackMethod());
    }

    /**
     * 获取指定方法的图片服务
     */
    public ImageSearchService getService(ImageMethodEnum method) {
        return serviceMap.get(method);
    }

    /**
     * 获取降级图片
     */
    public String getFallbackImage(int position) {
        ImageSearchService defaultService = serviceMap.get(ImageMethodEnum.getDefaultSearchMethod());
        if (defaultService != null) {
            return defaultService.getFallbackImage(position);
        }
        return String.format("https://picsum.photos/800/600?random=%d", position);
    }

    /**
     * 获取所有已注册的图片服务类型
     */
    public List<ImageMethodEnum> getRegisteredMethods() {
        return List.copyOf(serviceMap.keySet());
    }



    public static class ImageResult {
        private final String url;
        private final ImageMethodEnum methodEnum;

        public ImageResult(String url, ImageMethodEnum methodEnum) {
            this.url = url;
            this.methodEnum = methodEnum;
        }

        public String getUrl() {
            return url;
        }

        public ImageMethodEnum getMethodEnum() {
            return methodEnum;
        }

        public boolean isSuccess() {
            return url != null && !url.isEmpty();
        }
    }


}
