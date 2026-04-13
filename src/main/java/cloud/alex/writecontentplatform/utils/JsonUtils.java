package cloud.alex.writecontentplatform.utils;

import cloud.alex.writecontentplatform.exception.BusinessException;
import cloud.alex.writecontentplatform.exception.ErrorCode;
import cn.hutool.json.JSONUtil;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Slf4j
public class JsonUtils {


    public static <T> T parseJsonResponse(String content, Class<T> clazz, String name) {
        try {
            return JSONUtil.toBean(content, clazz);
        } catch (Exception e) {
            log.error("{}解析失败, content={}", name, content);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, name + "解析失败");
        }
    }

    public static <T> List<T> parseJsonListResponse(String content, Class<T> clazz, String name) {
        try {
            return JSONUtil.toList(content, clazz);
        } catch (Exception e) {
            log.error("{}解析失败, content={}", name, content);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, name + "解析失败");
        }
    }
}
