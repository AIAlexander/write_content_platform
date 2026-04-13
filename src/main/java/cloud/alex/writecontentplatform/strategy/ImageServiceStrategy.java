package cloud.alex.writecontentplatform.strategy;

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


}
