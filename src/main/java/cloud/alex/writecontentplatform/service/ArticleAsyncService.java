package cloud.alex.writecontentplatform.service;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ArticleAsyncService {

    void executeArticle(String taskId, String style, String topic);

    void executePhase1(String taskId, String topic, String style);

    void executePhase2(String taskId);

    void executePhase3(String taskId);
}
