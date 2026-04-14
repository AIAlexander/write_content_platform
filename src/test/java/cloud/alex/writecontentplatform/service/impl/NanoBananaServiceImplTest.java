package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.model.dto.image.ImageDto;
import cloud.alex.writecontentplatform.model.dto.image.ImageRequest;
import cloud.alex.writecontentplatform.service.FileService;
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
class NanoBananaServiceImplTest {

    @Autowired
    private NanoBananaServiceImpl nanoBananaService;

    @Autowired
    private FileService fileService;

    @Test
    void getImageData() {

        ImageRequest request = ImageRequest.builder()
                        .prompt("这是一只可爱的猫咪在草地上玩耍")
                                .build();

        ImageDto imageData = nanoBananaService.getImageData(request);
        Assertions.assertNotNull(imageData.getUrl());

        String s = fileService.uploadImage(imageData, "test");
        System.out.println(s);
        Assertions.assertNotNull(s);

    }
}