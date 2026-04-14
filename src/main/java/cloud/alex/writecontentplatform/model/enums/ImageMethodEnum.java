package cloud.alex.writecontentplatform.model.enums;

import lombok.Getter;

/**
 * @author wangshuhao
 * @date 2026/4/13
 *
 * 图片生成的枚举
 *
 */
@Getter
public enum ImageMethodEnum {

    PEXELS("PEXELS", "Pexels图片生成", false, false),

    NANO_BANANA("NANO_BANANA", "Nano Banana AI 生图", true, false),

    MERMAID("MERMAID", "Mermaid 流程图生成", true, false),

    ICONIFY("ICONIFY", "Iconify 图标库", false, false),

    EMOJI_PACK("EMOJI_PACK", "表情包检索", false, false),

    SVG_DIAGRAM("SVG_DIAGRAM", "SVG 概念示意图", true, false),

    PICSUM("picsum", "Picsum图片生成", false, true)

    ;

    /**
     * 消息类型值
     */
    private final String value;

    /**
     * 消息类型描述
     */
    private final String description;

    /**
     * 是否为AI生图方式
     * true：使用prompt生成图片
     * false：使用keywords检索图片
     */
    private final boolean aiGenerated;

    /**
     * 是否为降级方案
     */
    private final boolean fallback;

    ImageMethodEnum(String value, String description, boolean aiGenerated, boolean fallback) {
        this.value = value;
        this.description = description;
        this.aiGenerated = aiGenerated;
        this.fallback = fallback;
    }

    public static ImageMethodEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ImageMethodEnum imageMethodEnum : values()) {
            if (imageMethodEnum.getValue().equals(value)) {
                return imageMethodEnum;
            }
        }
        return null;
    }

    public static ImageMethodEnum getDefaultSearchMethod() {
        return PEXELS;
    }

    public static ImageMethodEnum getDefaultAiMethod() {
        return NANO_BANANA;
    }

    public static ImageMethodEnum getFallbackMethod() {
        return PICSUM;
    }
}
