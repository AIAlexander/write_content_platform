package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.config.PexelsConfig;
import cloud.alex.writecontentplatform.constant.ArticleConstant;
import cloud.alex.writecontentplatform.model.enums.ImageMethodEnum;
import cloud.alex.writecontentplatform.service.ImageSearchService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Service
@Slf4j
public class PexelsServiceImpl implements ImageSearchService {

    @Resource
    private PexelsConfig config;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String searchImage(String keywords) {
        try {
            String url = buildSearchUrl(keywords);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", config.getApiKey())
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Pexels API 调用失败:{}", response.code());
                    return null;
                }
                String responseBody = response.body().string();
                return extractImageUrl(responseBody, keywords);
            }
        } catch (IOException e) {
            log.error("Pexels API 调用异常", e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.PEXELS;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(ArticleConstant.PICSUM_URL_TEMPLATE, position);
    }

    /**
     * 构建搜索的url
     * @param keyword
     * @return
     */
    private String buildSearchUrl(String keyword) {
        return String.format("%s?query=%s&per_page=%d&orientation=%s",
                ArticleConstant.PEXELS_API_URL,
                keyword,
                ArticleConstant.PEXELS_PER_PAGE,
                ArticleConstant.PEXELS_ORIENTATION_LANDSCAPE);
    }

    /**
     * 从响应中提取图片URL
     * @param responseBody
     * @param keywords
     * @return
     */
    private String extractImageUrl(String responseBody, String keywords) {
        JSONObject entries = JSONUtil.parseObj(responseBody);
        JSONArray photos = entries.getJSONArray("photos");

        if (photos.isEmpty()) {
            log.warn("Pexels 未检索到图片：{}", keywords);
            return null;
        }
        JSONObject photo = photos.getJSONObject(0);
        JSONObject src = photo.getJSONObject("src");
        return src.getStr("large");
    }
}
