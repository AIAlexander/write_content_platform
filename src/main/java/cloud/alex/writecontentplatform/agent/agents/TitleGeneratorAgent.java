package cloud.alex.writecontentplatform.agent.agents;

import cloud.alex.writecontentplatform.agent.constant.StateConstant;
import cloud.alex.writecontentplatform.constant.PromptConstant;
import cloud.alex.writecontentplatform.exception.BusinessException;
import cloud.alex.writecontentplatform.exception.ErrorCode;
import cloud.alex.writecontentplatform.model.dto.article.ArticleState;
import cloud.alex.writecontentplatform.model.enums.ArticleStyleEnum;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author wsh
 * @date 2026/4/14
 *
 * 标题生成Agent
 * 生成 3-5个爆款标题方案
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TitleGeneratorAgent implements NodeAction {

    private final DashScopeChatModel dashScopeChatModel;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 从状态上下文中读取内容
        String topic = state.value(StateConstant.INPUT_TOPIC)
                .map(Object::toString)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "缺少topic参数"));

        String style = state.value(StateConstant.INPUT_STYLE)
                .map(Object::toString)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "缺少style参数"));
        log.info("TitleGenerator 开始执行，topic={}, style={}", topic, style);

        // 构建prompt
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", topic) + getStylePrompt(style);
        ChatResponse response = dashScopeChatModel.call(new Prompt(new UserMessage(prompt)));
        String text = response.getResult().getOutput().getText();

        List<ArticleState.TitleOption> list = JSONUtil.toList(text, ArticleState.TitleOption.class);
        log.info("TitleGenerator 执行完成，生成标题数量：{}", list.size());

        return Map.of(StateConstant.OUTPUT_TITLE_OPTION, list);
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
