package cloud.alex.writecontentplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("cloud.alex.writecontentplatform.mapper")
public class WriteContentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(WriteContentPlatformApplication.class, args);
    }

}
