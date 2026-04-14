package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.constant.PromptConstant;
import cloud.alex.writecontentplatform.exception.BusinessException;
import cloud.alex.writecontentplatform.exception.ErrorCode;
import cloud.alex.writecontentplatform.model.dto.article.ArticleState;
import cloud.alex.writecontentplatform.model.enums.ImageMethodEnum;
import cloud.alex.writecontentplatform.model.enums.SseMessageTypeEnum;
import cloud.alex.writecontentplatform.service.ArticleAgentService;
import cloud.alex.writecontentplatform.service.FileService;
import cloud.alex.writecontentplatform.service.ImageSearchService;
import cloud.alex.writecontentplatform.utils.JsonUtils;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Var;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Service
@Slf4j
public class ArticleAgentServiceImpl implements ArticleAgentService {

    @Resource
    private DashScopeChatModel chatModel;



    @Resource
    private FileService fileService;

    /**
     * Agent执行流程
     * @param state
     * @param streamHandler
     */
    @Override
    public void executeArticleGeneration(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 智能体1：生成标题
            log.info("智能体1：开始生成标题，taskId={}", state.getTaskId());
            generateTitle(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT1_COMPLETE.getValue());

            // 智能体2：生成大纲（流式输出）
            log.info("智能体2：开始生成大纲，taskId={}", state.getTaskId());
            generateOutline(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT2_COMPLETE.getValue());

            // 智能体3：生成正文（流式输出）
            log.info("智能体3：开始生成正文，taskId={}", state.getTaskId());
            generateContent(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT3_COMPLETE.getValue());

            // 智能体4：分析配图需求
            log.info("智能体4：开始分析配图需求，taskId={}", state.getTaskId());
            analyzeImageRequirements(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT4_COMPLETE.getValue());

            // 智能体5： 生成配图
            log.info("智能体5：开始生成配图，taskId={}", state.getTaskId());
            generateImages(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT5_COMPLETE.getValue());

            // 图文合成
            log.info("图文合成，taskId={}", state.getTaskId());
            mergeImagesToContent(state);
            streamHandler.accept(SseMessageTypeEnum.MERGE_COMPLETE.getValue());

            log.info("文章生成完成，taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.error("文章生成失败，taskId={}", state.getTaskId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文章生成失败");
        }
    }

    /**
     * 智能体1：生成标题
     * @param state
     */
    private void generateTitle(ArticleState state) {
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", state.getTopic());
        String content = callLlm(prompt);
        ArticleState.TitleResult titleResult = JsonUtils.parseJsonResponse(content, ArticleState.TitleResult.class, "标题");
        state.setTitle(titleResult);
        log.info("智能体1：标题生成成功，mainTitle={}", titleResult.getMainTitle());
    }

    /**
     * 智能体2：生成大纲（流式输出）
     * @param state
     * @param streamHandler
     */
    private void generateOutline(ArticleState state, Consumer<String> streamHandler) {
        String prompt = PromptConstant.AGENT2_OUTLINE_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{subTitle}", state.getTitle().getSubTitle());
        String content = callLlmWithStreaming(prompt, streamHandler, SseMessageTypeEnum.AGENT2_STREAMING);
        ArticleState.OutlineResult outlineResult = JsonUtils.parseJsonResponse(content, ArticleState.OutlineResult.class, "大纲");
        state.setOutline(outlineResult);
        log.info("智能体2：大纲生成成功，sections={}", outlineResult.getSections().size());
    }

    /**
     * 智能体3：生成正文（流式输出）
     * @param state
     * @param streamHandler
     */
    private void generateContent(ArticleState state, Consumer<String> streamHandler) {
        String outlineText = JSONUtil.toJsonStr(state.getOutline().getSections());
        String prompt = PromptConstant.AGENT3_CONTENT_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{subTitle}", state.getTitle().getSubTitle())
                .replace("{outline}", outlineText);
        String content = callLlmWithStreaming(prompt, streamHandler, SseMessageTypeEnum.AGENT3_STREAMING);
        state.setContent(content);
        log.info("智能体3：正文生成成功，contentLength={}", content.length());
    }

    /**
     * 智能体4：分析配图需求
     * @param state
     */
    private void analyzeImageRequirements(ArticleState state) {
        String prompt = PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{content}", state.getContent());
        String content = callLlm(prompt);
        List<ArticleState.ImageRequirement> imageRequirementList = JsonUtils.parseJsonListResponse(content,
                ArticleState.ImageRequirement.class, "配图需求");
        state.setImageRequirementList(imageRequirementList);
        log.info("智能体4：配图需求分析成功，imageRequirementList={}", imageRequirementList.size());
    }

    /**
     * 智能体5：生成图片
     * @param state
     * @param streamHandler
     */
    private void generateImages(ArticleState state, Consumer<String> streamHandler) {
        List<ArticleState.ImageResult> imageResults = new ArrayList<>();

//        for (ArticleState.ImageRequirement requirement : state.getImageRequirementList()) {
//            log.info("智能体5：开始检索配图, position={}, keywords={}",
//                    requirement.getPosition(), requirement.getKeywords());
//
//            String imageUrl = imageSearchService.searchImage(requirement.getKeywords());
//
//            // 降级策略
//            ImageMethodEnum method = imageSearchService.getMethod();
//            if (imageUrl == null) {
//                // 生成图片失败，触发降级策略
//                imageUrl = imageSearchService.getFallbackImage(requirement.getPosition());
//                method = ImageMethodEnum.PICSUM;
//                log.warn("智能体5：图片检索失败, 使用降级方案，position={}", requirement.getPosition());
//            }
//
//            // 创建配图结果
//            ArticleState.ImageResult imageResult = buildImageResult(requirement, imageUrl, method);
//            imageResults.add(imageResult);
//
//            String completeMessage = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix() + JSONUtil.toJsonStr(imageResult);
//            streamHandler.accept(completeMessage);
//
//            log.info("智能体5：配图检索成功, position={}, method={}",
//                    requirement.getPosition(), method.getValue());
//        }

        state.setImages(imageResults);
        log.info("智能体5：所有配图生成完成, count={}", imageResults.size());
    }

    /**
     * 将图片插入到正文内容中
     * 实现方法：
     *      1. 遇到 ## 开头的标题时，检查配图列表中有没有相匹配的图片。如果有，则将图片Markdown插入到标题后面
     * @param state
     */
    private void mergeImagesToContent(ArticleState state) {
        String content = state.getContent();
        List<ArticleState.ImageResult> images = state.getImages();
        if (images == null || images.isEmpty()) {
            state.setFullContent(content);
            return;
        }
        StringBuilder fullContent = new StringBuilder();

        String[] lines = content.split("\n");
        for (String line : lines) {
            fullContent.append(line).append("\n");

            // 检查是否是章节标题
            if (line.startsWith("## ")) {
                String sectionTitle = line.substring(3).trim();
                insertImageAfterSection(fullContent, images, sectionTitle);
            }
        }
        state.setFullContent(fullContent.toString());
        log.info("图文合成完成, fullContentLength={}", fullContent.length());
    }



    /**
     * 调用LLM
     * @param prompt
     * @return
     */
    private String callLlm(String prompt) {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult().getOutput().getText();
    }

    /**
     * 调用LLM（流式输出）
     * @param prompt
     * @param streamHandler
     * @param messageTypeEnum
     * @return
     */
    private String callLlmWithStreaming(String prompt, Consumer<String> streamHandler,
                                        SseMessageTypeEnum messageTypeEnum) {
        StringBuilder builder = new StringBuilder();

        Flux<ChatResponse> streamResponse = chatModel.stream(new Prompt(new UserMessage(prompt)));
        streamResponse
                .doOnNext(response -> {
                    String chunk = response.getResult().getOutput().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        builder.append(chunk);
                        streamHandler.accept(messageTypeEnum.getStreamingPrefix() + chunk);
                    }
                })
                .doOnError(error -> log.error("LLM流式调用失败， messageType={}", messageTypeEnum, error))
                .blockLast();
        return builder.toString();
    }

    /**
     * 构建配图结果
     * @param requirement
     * @param imageUrl
     * @param methodEnum
     * @return
     */
    private ArticleState.ImageResult buildImageResult(ArticleState.ImageRequirement requirement,
                                                      String imageUrl,
                                                      ImageMethodEnum methodEnum) {
        ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
        imageResult.setPosition(requirement.getPosition());
        imageResult.setUrl(imageUrl);
        imageResult.setMethod(methodEnum.getValue());
        imageResult.setKeywords(requirement.getKeywords());
        imageResult.setSectionTitle(requirement.getSectionTitle());
        return imageResult;
    }

    /**
     * 在某个content中间插入图片
     * @param fullContent
     * @param images
     * @param sectionTitle
     */
    private void insertImageAfterSection(StringBuilder fullContent,
                                         List<ArticleState.ImageResult> images,
                                         String sectionTitle) {
        for (ArticleState.ImageResult image : images) {
            // 排除到第一个封面图片
            if (image.getPosition() > 1
                    && image.getSectionTitle() != null
                    && sectionTitle.contains(image.getSectionTitle().trim())) {
                fullContent.append("\n![").append(image.getDescription()).append("](").append(image.getUrl()).append(")\n");
                break;
            }
        }
    }
}
