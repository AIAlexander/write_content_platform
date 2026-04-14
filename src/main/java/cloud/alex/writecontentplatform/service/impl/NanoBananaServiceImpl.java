package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.config.GrsaiConfig;
import cloud.alex.writecontentplatform.constant.ArticleConstant;
import cloud.alex.writecontentplatform.model.dto.image.ImageDto;
import cloud.alex.writecontentplatform.model.dto.image.ImageRequest;
import cloud.alex.writecontentplatform.model.enums.ImageMethodEnum;
import cloud.alex.writecontentplatform.service.ImageSearchService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.JsonParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author wangshuhao
 * @date 2026/4/14
 */
@Service
@Slf4j
public class NanoBananaServiceImpl implements ImageSearchService {

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build();
    @Resource
    private GrsaiConfig grsaiConfig;

    @Override
    public ImageDto getImageData(ImageRequest imageRequest) {
        String prompt = imageRequest.getEffectiveParam(true);
        String url = generateImage(prompt);
        return ImageDto.fromUrl(url);
    }

    private String generateImage(String prompt) {
        String host = grsaiConfig.getHost();
        GrsaiRequest grsaiRequest = new GrsaiRequest(grsaiConfig.getModel(),
                prompt, "auto", grsaiConfig.getImageSize());
        String json = JSONUtil.toJsonStr(grsaiRequest);
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );
        Request request = new Request.Builder()
                .url(host)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + grsaiConfig.getApiKey())
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Grsai API 调用失败: " + response.code());
            }
            String responseStr = response.body().string();
            return extractImageUrl(responseStr);
        } catch (Exception e) {
            log.error("Grsai API 调用异常", e);
            return null;
        }

    }

    /**
     * 从响应中提取图片URL
     * @param responseBody
     * @return
     */
    private String extractImageUrl(String responseBody) {
        String data = parseLastData(responseBody);
        JSONObject entries = JSONUtil.parseObj(data);
        JSONArray results = entries.getJSONArray("results");

        if (results.isEmpty()) {
            log.warn("Grsai 未生成图片");
            return null;
        }
        JSONObject result = results.getJSONObject(0);
        return result.getStr("url");
    }

    private static String parseLastData(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String[] lines = content.split("\n");

        // 倒序找最后一个 data:
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();

            if (line.startsWith("data:")) {
                return line.substring(5).trim(); // 去掉 "data:"
            }
        }

        return null;
    }

    @Override
    public String searchImage(String keywords) {
        return null;
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.NANO_BANANA;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(ArticleConstant.PICSUM_URL_TEMPLATE, position);
    }

    private static class GrsaiRequest {
        private final String model;
        private final String prompt;
        private final String aspectRatio;
        private final String imageSize;

        public GrsaiRequest(String model, String prompt, String aspectRatio, String imageSize) {
            this.model = model;
            this.prompt = prompt;
            this.aspectRatio = aspectRatio;
            this.imageSize = imageSize;
        }

        public String getModel() {
            return model;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getAspectRatio() {
            return aspectRatio;
        }

        public String getImageSize() {
            return imageSize;
        }
    }
}
