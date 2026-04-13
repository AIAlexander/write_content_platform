package cloud.alex.writecontentplatform.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangshuhao
 * @date 2026/4/8
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssConfig {

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

    private String region;

    @Bean
    public OSS ossClient() {
        DefaultCredentials credentials = new DefaultCredentials(accessKeyId, accessKeySecret);
        DefaultCredentialProvider provider = new DefaultCredentialProvider(credentials);
        return OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(provider)
                .region(region)
                .build();
    }
}
