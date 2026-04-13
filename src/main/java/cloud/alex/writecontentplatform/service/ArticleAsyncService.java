package cloud.alex.writecontentplatform.service;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ArticleAsyncService {

    void executeArticle(String taskId, String topic);
}
