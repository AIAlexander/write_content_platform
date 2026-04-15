package cloud.alex.writecontentplatform.service.impl;

import cloud.alex.writecontentplatform.constant.PromptConstant;
import cloud.alex.writecontentplatform.exception.BusinessException;
import cloud.alex.writecontentplatform.exception.ErrorCode;
import cloud.alex.writecontentplatform.model.dto.article.ArticleState;
import cloud.alex.writecontentplatform.model.dto.image.ImageRequest;
import cloud.alex.writecontentplatform.model.enums.ArticleStyleEnum;
import cloud.alex.writecontentplatform.model.enums.ImageMethodEnum;
import cloud.alex.writecontentplatform.model.enums.SseMessageTypeEnum;
import cloud.alex.writecontentplatform.service.ArticleAgentService;
import cloud.alex.writecontentplatform.service.FileService;
import cloud.alex.writecontentplatform.service.ImageSearchService;
import cloud.alex.writecontentplatform.strategy.ImageServiceStrategy;
import cloud.alex.writecontentplatform.utils.JsonUtils;
import cn.hutool.core.util.StrUtil;
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
    private ImageServiceStrategy imageServiceStrategy;

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

    @Override
    public List<ArticleState.OutlineSection> aiModifyOutline(String mainTitle, String subTitle,
                                                             List<ArticleState.OutlineSection> currentOutline,
                                                             String modifySuggestion) {
        String currentOutlineJson = JSONUtil.toJsonStr(currentOutline);

        String prompt = PromptConstant.AI_MODIFY_OUTLINE_PROMPT
                .replace("{mainTitle}", mainTitle)
                .replace("{subTitle}", subTitle)
                .replace("{currentOutline}", currentOutlineJson)
                .replace("{modifySuggestion}", modifySuggestion);

        String content = callLlm(prompt);
        ArticleState.OutlineResult outlineResult = JsonUtils.parseJsonResponse(content, ArticleState.OutlineResult.class, "修改后的大纲");

        log.info("AI修改大纲成功, sectionsCount={}", outlineResult.getSections().size());
        return outlineResult.getSections();
    }

    /**
     * 阶段1：生成标题方案（3-5个）
     *
     * @param state         文章状态
     * @param streamHandler 流式输出处理器
     */
    @Override
    public void generateTitlesProcess(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 智能体1：生成标题方案
            log.info("阶段1：开始生成标题方案, taskId={}", state.getTaskId());
            generateTitle(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            log.info("阶段1：标题方案生成完成, taskId={}, optionsCount={}",
                    state.getTaskId(), state.getTitleOptions().size());
        } catch (Exception e) {
            log.error("阶段1：标题方案生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("标题方案生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 阶段2：生成大纲（用户选择标题后）
     *
     * @param state         文章状态
     * @param streamHandler 流式输出处理器
     */
    @Override
    public void generateOutlineProcess(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 智能体2：生成大纲（流式输出）
            log.info("阶段2：开始生成大纲, taskId={}", state.getTaskId());
            generateOutline(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            log.info("阶段2：大纲生成完成, taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.error("阶段2：大纲生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("大纲生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 阶段3：生成正文+配图（用户确认大纲后）
     *
     * @param state         文章状态
     * @param streamHandler 流式输出处理器
     */
    @Override
    public void generateContentProcess(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 智能体3：生成正文（流式输出）
            log.info("阶段3：开始生成正文, taskId={}", state.getTaskId());
            generateContent(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT3_COMPLETE.getValue());

            // 智能体4：分析配图需求
            log.info("阶段3：开始分析配图需求, taskId={}", state.getTaskId());
            analyzeImageRequirements(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT4_COMPLETE.getValue());

            // 智能体5：生成配图
            log.info("阶段3：开始生成配图, taskId={}", state.getTaskId());
            generateImages(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT5_COMPLETE.getValue());

            // 图文合成：将配图插入正文
            log.info("阶段3：开始图文合成, taskId={}", state.getTaskId());
            mergeImagesToContent(state);
            streamHandler.accept(SseMessageTypeEnum.MERGE_COMPLETE.getValue());

            log.info("阶段3：正文生成完成, taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.error("阶段3：正文生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("正文生成失败: " + e.getMessage(), e);
        }
    }


    /**
     * 智能体1：生成标题
     * @param state
     */
    private void generateTitle(ArticleState state) {
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", state.getTopic()) + getStylePrompt(state.getStyle());
        String content = callLlm(prompt);
        List<ArticleState.TitleOption> titleResultList = JsonUtils.parseJsonListResponse(content, ArticleState.TitleOption.class, "标题");
        state.setTitleOptions(titleResultList);
        log.info("智能体1：标题生成成功，titleOptionList size={}", titleResultList.size());
    }

    /**
     * 智能体2：生成大纲（流式输出）
     * @param state
     * @param streamHandler
     */
    private void generateOutline(ArticleState state, Consumer<String> streamHandler) {
        // 构建Prompt，根据是否有用户补充描述插入对应部分
        String descriptionSection = "";
        if (state.getUserDescription() != null
                && !state.getUserDescription().trim().isEmpty()) {
            descriptionSection = PromptConstant.AGENT2_DESCRIPTION_SECTION
                    .replace("{userDescription}", state.getUserDescription());
        }
        String prompt = PromptConstant.AGENT2_OUTLINE_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{subTitle}", state.getTitle().getSubTitle())
                .replace("{descriptionSection}", descriptionSection)+ getStylePrompt(state.getStyle());
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
                .replace("{outline}", outlineText) + getStylePrompt(state.getStyle());
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

        ArticleState.AddImageRequest addImageRequest = JsonUtils.parseJsonResponse(content,
                ArticleState.AddImageRequest.class, "配图需求");
        state.setImageRequirements(addImageRequest.getImageRequirements());
        state.setContent(addImageRequest.getContentWithPlaceholders());
        log.info("智能体4：配图需求分析成功，imageRequirementList={}", state.getImageRequirements().size());
    }

    /**
     * 智能体5：生成图片
     * @param state
     * @param streamHandler
     */
    private void generateImages(ArticleState state, Consumer<String> streamHandler) {
        List<ArticleState.ImageResult> imageResults = new ArrayList<>();

        for (ArticleState.ImageRequirement requirement : state.getImageRequirements()) {
            // 获取调用图片的服务
            String imageSource = requirement.getImageSource();

            log.info("智能体5：开始检索配图, position={}, keywords={}, prompt:{}, source={}",
                    requirement.getPosition(), requirement.getKeywords(), requirement.getPrompt(), imageSource);

            ImageRequest imageRequest = ImageRequest.builder()
                    .prompt(requirement.getPrompt())
                    .keywords(requirement.getKeywords())
                    .position(requirement.getPosition())
                    .type(requirement.getType())
                    .build();

            ImageServiceStrategy.ImageResult uploadResult = imageServiceStrategy.getImageAndUpload(imageSource, imageRequest);

            String url = uploadResult.getUrl();
            ImageMethodEnum method = uploadResult.getMethodEnum();

            // 创建配图结果
            ArticleState.ImageResult imageResult = buildImageResult(requirement, url, method);
            imageResults.add(imageResult);

            String completeMessage = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix() + JSONUtil.toJsonStr(imageResult);
            streamHandler.accept(completeMessage);

            log.info("智能体5：配图检索成功, position={}, method={}",
                    requirement.getPosition(), method.getValue());
        }

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
        String fullContent = content;
        for (ArticleState.ImageResult image : state.getImages()) {
            if (image.getPosition() == 1) {
                // 封面图,跳过不处理
                continue;
            }
            String placeholder = image.getPlaceholderId();
            if (placeholder != null && !placeholder.trim().isEmpty()) {
                String imageMarkdown = "![" + image.getDescription() + "](" + image.getUrl() + ")";
                fullContent = fullContent.replace(placeholder, imageMarkdown);
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
        imageResult.setPlaceholderId(requirement.getPlaceholderId());
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

    private String getStylePrompt(String style) {
        if (style == null || style.isEmpty()) {
            return "";
        }
        ArticleStyleEnum styleEnum = ArticleStyleEnum.getEnumByValue(style);
        if (styleEnum == null) {
            return "";
        }

        return switch (styleEnum) {
            case TECH -> PromptConstant.STYLE_TECH_PROMPT;
            case EMOTIONAL -> PromptConstant.STYLE_EMOTIONAL_PROMPT;
            case EDUCATIONAL -> PromptConstant.STYLE_EDUCATIONAL_PROMPT;
            case HUMOROUS -> PromptConstant.STYLE_HUMOROUS_PROMPT;
            default -> StrUtil.EMPTY;
        };
    }
}
