package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.model.dto.image.ImageDto;
import cloud.alex.writecontentplatform.model.dto.image.ImageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wangshuhao
 * @date 2026/4/14
 */
@SpringBootTest
class IconifyServiceImplTest {

    @Autowired
    private IconifyServiceImpl iconifyService;

    @Test
    public void test() {
        ImageRequest request = ImageRequest.builder().prompt("ri:24-hours-line").build();
        ImageDto imageData = iconifyService.getImageData(request);
        String url = imageData.getUrl();
        Assertions.assertNotNull(url);
        System.out.println(url);
    }

}