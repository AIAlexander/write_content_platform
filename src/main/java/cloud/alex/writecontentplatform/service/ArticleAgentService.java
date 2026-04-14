package cloud.alex.writecontentplatform.service;

import cloud.alex.writecontentplatform.model.dto.article.ArticleState;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ArticleAgentService {

    void executeArticleGeneration(ArticleState state, Consumer<String> streamHandler);

    List<ArticleState.OutlineSection> aiModifyOutline(String mainTitle, String subTitle,
                                                      List<ArticleState.OutlineSection> currentOutline,
                                                      String modifySuggestion);

    void generateTitlesProcess(ArticleState state, Consumer<String> streamHandler);

    void generateOutlineProcess(ArticleState state, Consumer<String> streamHandler);

    void generateContentProcess(ArticleState state, Consumer<String> streamHandler);
}
