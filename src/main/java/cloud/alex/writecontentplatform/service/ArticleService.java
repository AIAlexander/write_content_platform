package cloud.alex.writecontentplatform.service;

import cloud.alex.writecontentplatform.model.dto.article.ArticleQueryRequest;
import cloud.alex.writecontentplatform.model.dto.article.ArticleState;
import cloud.alex.writecontentplatform.model.entity.Article;
import cloud.alex.writecontentplatform.model.entity.User;
import cloud.alex.writecontentplatform.model.enums.ArticlePhaseEnum;
import cloud.alex.writecontentplatform.model.enums.ArticleStatusEnum;
import cloud.alex.writecontentplatform.model.vo.ArticleVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface ArticleService extends IService<Article> {


    String createArticleTask(String topic, String style, User loginUser);

    Article getByTaskId(String taskId);

    ArticleVO getArticleDetail(String taskId, User loginUser);

    void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage);

    void saveArticleContent(String taskId, ArticleState state);

    Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser);

    boolean deleteArticle(Long id, User loginUser);

    /**
     * 确认标题（用户选择后）
     *
     * @param taskId       任务ID
     * @param mainTitle    选中的主标题
     * @param subTitle     选中的副标题
     * @param userDescription 用户补充描述
     * @param loginUser    当前登录用户
     */
    void confirmTitle(String taskId, String mainTitle, String subTitle, String userDescription, User loginUser);

    /**
     * 确认大纲（用户编辑后）
     *
     * @param taskId    任务ID
     * @param outline   用户编辑后的大纲
     * @param loginUser 当前登录用户
     */
    void confirmOutline(String taskId, List<ArticleState.OutlineSection> outline, User loginUser);

    /**
     * 更新阶段
     *
     * @param taskId 任务ID
     * @param phase  阶段枚举
     */
    void updatePhase(String taskId, ArticlePhaseEnum phase);

    /**
     * 保存标题方案
     *
     * @param taskId       任务ID
     * @param titleOptions 标题方案列表
     */
    void saveTitleOptions(String taskId, List<ArticleState.TitleOption> titleOptions);

    /**
     * AI 修改大纲
     *
     * @param taskId           任务ID
     * @param modifySuggestion 用户修改建议
     * @param loginUser        当前登录用户
     * @return 修改后的大纲
     */
    List<ArticleState.OutlineSection> aiModifyOutline(String taskId, String modifySuggestion, User loginUser);




}
