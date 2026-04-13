package cloud.alex.writecontentplatform.service;

import cloud.alex.writecontentplatform.model.dto.article.ArticleQueryRequest;
import cloud.alex.writecontentplatform.model.dto.article.ArticleState;
import cloud.alex.writecontentplatform.model.entity.Article;
import cloud.alex.writecontentplatform.model.entity.User;
import cloud.alex.writecontentplatform.model.enums.ArticleStatusEnum;
import cloud.alex.writecontentplatform.model.vo.ArticleVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ArticleService extends IService<Article> {


    String createArticleTask(String topic, User loginUser);

    Article getByTaskId(String taskId);

    ArticleVO getArticleDetail(String taskId, User loginUser);

    void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage);

    void saveArticleContent(String taskId, ArticleState state);

    Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser);

    boolean deleteArticle(Long id, User loginUser);



}
