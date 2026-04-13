package cloud.alex.writecontentplatform.model.dto.image;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author wangshuhao
 * @date 2026/4/13
 *
 * 图片请求对象
 *
 */
@Data
@Builder
public class ImageRequest {

    /**
     * 图片搜索关键字
     */
    private String keywords;

    /**
     * 图片生成的提示词
     */
    private String prompt;

    /**
     * 图片序号
     */
    private Integer position;

    /**
     * 图片类型（cover/section）
     */
    private String type;

    /**
     * 宽高比（16:9）
     */
    private String aspectRatio;

    /**
     * 图片风格描述
     */
    private String style;

    /**
     * 获取有效的搜索/生成参数
     *
     * AI生图优先使用prompt，图库检索使用keywords
     *
     * @param isAiGenerated
     * @return
     */
    public String getEffectiveParam(boolean isAiGenerated) {
        if (isAiGenerated) {
            // 使用 AI 生成
            return prompt != null && !prompt.isEmpty() ? prompt : keywords;
        }
        return keywords != null && !keywords.isEmpty() ? keywords : prompt;
    }
}
