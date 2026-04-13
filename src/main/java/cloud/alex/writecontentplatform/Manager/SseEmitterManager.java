package cloud.alex.writecontentplatform.Manager;

import cloud.alex.writecontentplatform.constant.ArticleConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Component
@Slf4j
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建SseEmitter
     * @param taskId
     * @return
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(ArticleConstant.SSE_TIMEOUT_MS);

        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时, taskId={}", taskId);
            emitterMap.remove(taskId);
        });

        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成, taskId={}", taskId);
            emitterMap.remove(taskId);
        });

        // 设置错误回调
        emitter.onError((e) -> {
            log.error("SSE 连接失败, taskId={}", taskId);
            emitterMap.remove(taskId);
        });

        emitterMap.put(taskId, emitter);
        log.info("SSE 连接已创建, taskId={}", taskId);

        return emitter;
    }

    /**
     * 发送消息
     * @param taskId
     * @param message
     */
    public void send(String taskId, String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在，taskId={}", taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .data(message)
                    .reconnectTime(ArticleConstant.SSE_RECONNECT_TIME_MS));
            log.debug("SSE 消息发送成功，taskId={}, message={}", taskId, message);
        } catch (Exception e) {
            log.error("SSE 消息发送失败，taskId={}, message={}", taskId, message);
            emitterMap.remove(taskId);
        }
    }

    /**
     * 完成连接
     * @param taskId
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在, taskId={}", taskId);
            return;
        }
        try {
            emitter.complete();
            log.info("SSE 连接已完成，taskId={}", taskId);
        } catch (Exception e) {
            log.error("SSE 连接完成，taskId={}", taskId);
        } finally {
            emitterMap.remove(taskId);
        }
    }

    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }

    public SseEmitter getEmitter(String taskId) {
        return emitterMap.get(taskId);
    }

}
