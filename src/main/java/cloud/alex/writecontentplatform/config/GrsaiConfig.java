package cloud.alex.writecontentplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */

@Configuration
@ConfigurationProperties(prefix = "grsai")
@Data
public class GrsaiConfig {

    /**
     * api-key
     */
    private String apiKey;

    /**
     * host
     */
    private String host;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 图片比例
     */
    private String aspectRatio;

    /**
     * 图片大小
     */
    private String imageSize;

}
