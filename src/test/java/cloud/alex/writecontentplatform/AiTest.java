package cloud.alex.writecontentplatform;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@SpringBootTest
public class AiTest {

    @Resource
    private DashScopeChatModel chatModel;

    @Test
    public void testChat() {
        // 同步调用
//        String response = chatModel.call("你好，请介绍一下你自己?");
//        System.out.println(response);

        // 流式调用
        Flux<ChatResponse> streamResponse = chatModel.stream(
                new Prompt("用一句话介绍Spring Ai")
        );
        streamResponse.subscribe(c -> {
            System.out.println(c.getResult().getOutput().getText());
        });
    }

}
