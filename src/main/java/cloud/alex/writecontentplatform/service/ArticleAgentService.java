package cloud.alex.writecontentplatform.service;

import cloud.alex.writecontentplatform.model.dto.article.ArticleState;

import java.util.function.Consumer;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ArticleAgentService {

    void executeArticleGeneration(ArticleState state, Consumer<String> streamHandler);
}
