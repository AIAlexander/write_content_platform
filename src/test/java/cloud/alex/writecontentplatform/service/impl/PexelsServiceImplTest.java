package cloud.alex.writecontentplatform.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@SpringBootTest
class PexelsServiceImplTest {

    @Autowired
    private PexelsServiceImpl pexelsService;

    @Test
    void searchImage() {
        String s = pexelsService.searchImage("a mountain");
        Assertions.assertNotNull(s);

    }
}