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

    PEXELS("pexels", "Pexels图片生成"),

    PICSUM("picsum", "Picsum图片生成")

    ;

    /**
     * 消息类型值
     */
    private final String value;

    /**
     * 消息类型描述
     */
    private final String description;

    ImageMethodEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }



}
