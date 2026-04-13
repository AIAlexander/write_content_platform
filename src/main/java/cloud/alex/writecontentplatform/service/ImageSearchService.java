package cloud.alex.writecontentplatform.service;

import cloud.alex.writecontentplatform.model.dto.image.ImageDto;
import cloud.alex.writecontentplatform.model.dto.image.ImageRequest;
import cloud.alex.writecontentplatform.model.enums.ImageMethodEnum;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ImageSearchService {

    default String getImage(ImageRequest request) {
        // 默认实现
        String param = request.getEffectiveParam(getMethod().isAiGenerated());
        return searchImage(param);
    }

    default ImageDto getImageData(ImageRequest request) {
        // 默认实现，通过 getImage 获取 URL。然后转换为 ImageData
        String url = getImage(request);
        return ImageDto.fromUrl(url);
    }


    /**
     * 根据关键词检索图片
     * @param keywords          搜索关键词
     * @return                  图片URL，搜索失败返回null
     */
    String searchImage(String keywords);

    /**
     * 获取图片检索方法
     * @return
     */
    ImageMethodEnum getMethod();

    /**
     * 获取降级图片 URL
     * @param position
     * @return
     */
    String getFallbackImage(int position);
}
