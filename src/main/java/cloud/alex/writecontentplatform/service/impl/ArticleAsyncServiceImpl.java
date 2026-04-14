package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.Manager.SseEmitterManager;
import cloud.alex.writecontentplatform.model.dto.article.ArticleState;
import cloud.alex.writecontentplatform.model.entity.Article;
import cloud.alex.writecontentplatform.model.enums.ArticlePhaseEnum;
import cloud.alex.writecontentplatform.model.enums.ArticleStatusEnum;
import cloud.alex.writecontentplatform.model.enums.SseMessageTypeEnum;
import cloud.alex.writecontentplatform.service.ArticleAgentService;
import cloud.alex.writecontentplatform.service.ArticleAsyncService;
import cloud.alex.writecontentplatform.service.ArticleService;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Service
@Slf4j
public class ArticleAsyncServiceImpl implements ArticleAsyncService {

    @Autowired
    private ArticleAgentService articleAgentService;

    @Autowired
    private SseEmitterManager sseEmitterManager;

    @Autowired
    private ArticleService articleService;

    /**
     * 异步调用Agent生成文章
     * @param taskId
     * @param topic
     */
    @Async("articleExecutor")
    @Override
    public void executeArticle(String taskId, String style, String topic) {
        log.info("异步任务开始，taskId={}, topic={}", taskId, topic);
        try {
            SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
            // 更新状态为处理中...
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            // 创建AritcleState
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);
            // 提交生成任务
            articleAgentService.executeArticleGeneration(state, message -> {
                handleAgentMessage(taskId, message, state);
            });
            // 保存完整文件到数据库
            articleService.saveArticleContent(taskId, state);
            // 更新状态为已完成
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            // 推送完成消息
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            // 关闭SSE连接
            sseEmitterManager.complete(taskId);
            log.info("异步任务完成，taskId={}", taskId);
        } catch (Exception e) {
            log.error("异步任务完成，taskId={}", taskId);
            // 更新状态为失败
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            // 推送错误消息
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            // 关闭SSE连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 阶段1：异步生成标题方案
     *
     * @param taskId 任务ID
     * @param topic  选题
     * @param style  文章风格（可为空）
     */
    @Async("articleExecutor")
    @Override
    public void executePhase1(String taskId, String topic, String style) {
        log.info("阶段1异步任务开始, taskId={}, topic={}, style={}", taskId, topic, style);

        try {
            // 更新状态和阶段
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_GENERATING);

            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);

            // 执行阶段1：生成标题方案
            articleAgentService.generateTitlesProcess(state, message -> {
                handleAgentMessage(taskId, message, state);
            });

            // 保存标题方案到数据库
            articleService.saveTitleOptions(taskId, state.getTitleOptions());

            // 更新阶段为等待选择标题
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_SELECTING);

            // 推送标题方案生成完成消息
            Map<String, Object> data = new HashMap<>();
            data.put("titleOptions", state.getTitleOptions());
            sendSseMessage(taskId, SseMessageTypeEnum.TITLES_GENERATED, data);

            log.info("阶段1异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("阶段1异步任务失败, taskId={}", taskId, e);

            // 更新状态为失败
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());

            // 推送错误消息
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));

            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 阶段2：异步生成大纲（用户确认标题后调用）
     *
     * @param taskId 任务ID
     */
    @Async("articleExecutor")
    @Override
    public void executePhase2(String taskId) {
        log.info("阶段2异步任务开始, taskId={}", taskId);

        try {
            // 获取文章信息
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new RuntimeException("文章不存在");
            }

            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());
            state.setUserDescription(article.getUserDescription());

            // 设置标题
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);

            // 执行阶段2：生成大纲
            articleAgentService.generateOutlineProcess(state, message -> {
                handleAgentMessage(taskId, message, state);
            });

            // 保存大纲到数据库
            Article articleToUpdate = articleService.getByTaskId(taskId);
            articleToUpdate.setOutline(JSONUtil.toJsonStr(state.getOutline().getSections()));
            articleService.updateById(articleToUpdate);

            // 更新阶段为等待编辑大纲
            articleService.updatePhase(taskId, ArticlePhaseEnum.OUTLINE_EDITING);

            // 推送大纲生成完成消息
            Map<String, Object> data = new HashMap<>();
            data.put("outline", state.getOutline().getSections());
            sendSseMessage(taskId, SseMessageTypeEnum.OUTLINE_GENERATED, data);

            log.info("阶段2异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("阶段2异步任务失败, taskId={}", taskId, e);

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 阶段3：异步生成正文+配图（用户确认大纲后调用）
     *
     * @param taskId 任务ID
     */
    @Async("articleExecutor")
    @Override
    public void executePhase3(String taskId) {
        log.info("阶段3异步任务开始, taskId={}", taskId);

        try {
            // 获取文章信息
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new RuntimeException("文章不存在");
            }

            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());

            // 设置标题
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);

            // 设置大纲

            List<ArticleState.OutlineSection> outlineSections =
                    JSONUtil.toList(article.getOutline(), ArticleState.OutlineSection.class);

            ArticleState.OutlineResult outlineResult = new ArticleState.OutlineResult();
            outlineResult.setSections(outlineSections);
            state.setOutline(outlineResult);

            // 执行阶段3：生成正文+配图
            articleAgentService.generateContentProcess(state, message -> {
                handleAgentMessage(taskId, message, state);
            });

            // 保存完整文章到数据库
            articleService.saveArticleContent(taskId, state);

            // 更新状态为已完成
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);

            // 推送完成消息
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));

            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);

            log.info("阶段3异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("阶段3异步任务失败, taskId={}", taskId, e);

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }


    /**
     * 处理智能体消息并推送
     */
    private void handleAgentMessage(String taskId, String message, ArticleState state) {
        Map<String, Object> data = buildMessageData(message, state);
        if (data != null) {
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(data));
        }
    }

    /**
     * 构建消息数据
     */
    private Map<String, Object> buildMessageData(String message, ArticleState state) {
        // 处理流式消息（带冒号分隔符）
        String streamingPrefix2 = SseMessageTypeEnum.AGENT2_STREAMING.getStreamingPrefix();
        String streamingPrefix3 = SseMessageTypeEnum.AGENT3_STREAMING.getStreamingPrefix();
        String imageCompletePrefix = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix();

        if (message.startsWith(streamingPrefix2)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT2_STREAMING,
                    message.substring(streamingPrefix2.length()));
        }

        if (message.startsWith(streamingPrefix3)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT3_STREAMING,
                    message.substring(streamingPrefix3.length()));
        }

        if (message.startsWith(imageCompletePrefix)) {
            String imageJson = message.substring(imageCompletePrefix.length());
            return buildImageCompleteData(imageJson);
        }

        // 处理完成消息（枚举值）
        return buildCompleteMessageData(message, state);
    }

    /**
     * 构建流式输出数据
     */
    private Map<String, Object> buildStreamingData(SseMessageTypeEnum type, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.put("content", content);
        return data;
    }

    /**
     * 构建图片完成数据
     */
    private Map<String, Object> buildImageCompleteData(String imageJson) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", SseMessageTypeEnum.IMAGE_COMPLETE.getValue());
        data.put("image", JSONUtil.toBean(imageJson, ArticleState.ImageResult.class));
        return data;
    }

    /**
     * 构建完成消息数据
     */
    private Map<String, Object> buildCompleteMessageData(String message, ArticleState state) {
        Map<String, Object> data = new HashMap<>();

        if (SseMessageTypeEnum.AGENT1_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            data.put("title", state.getTitle());
        } else if (SseMessageTypeEnum.AGENT2_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            data.put("outline", state.getOutline().getSections());
        } else if (SseMessageTypeEnum.AGENT3_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT3_COMPLETE.getValue());
        } else if (SseMessageTypeEnum.AGENT4_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
            data.put("imageRequirements", state.getImageRequirements());
        } else if (SseMessageTypeEnum.AGENT5_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
            data.put("images", state.getImages());
        } else if (SseMessageTypeEnum.MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.MERGE_COMPLETE.getValue());
            data.put("fullContent", state.getFullContent());
        } else {
            return null;
        }

        return data;
    }

    /**
     * 发送 SSE 消息
     */
    private void sendSseMessage(String taskId, SseMessageTypeEnum type, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.putAll(additionalData);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(data));
    }

}
