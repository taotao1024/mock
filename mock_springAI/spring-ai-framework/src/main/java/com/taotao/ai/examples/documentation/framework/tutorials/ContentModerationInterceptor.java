package com.taotao.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class ContentModerationInterceptor extends ModelInterceptor {

    private static final List<String> BLOCKED_WORDS =
            List.of("敏感词1", "敏感词2", "敏感词3");

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 检查输入
        for (Message msg : request.getMessages()) {
            String content = msg.getText().toLowerCase();
            for (String blocked : BLOCKED_WORDS) {
                if (content.contains(blocked)) {
//                    return ModelResponse.blocked(
//                            "检测到不适当的内容，请修改您的输入"
                    return new ModelResponse("检测到不适当的内容，请修改您的输入");
                }
            }
        }

        // 执行模型调用
        ModelResponse response = handler.call(request);

        // 检查输出
//        String output = response.getContent();
//        for (String blocked : BLOCKED_WORDS) {
//            if (output.contains(blocked)) {
//                // 清理输出
//                output = output.replaceAll(blocked, "[已过滤]");
//                return response.withContent(output);
//            }
//        }

        return response;
    }

    @Override
    public String getName() {
        return "ContentModerationInterceptor";
    }
}