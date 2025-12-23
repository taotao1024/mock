package com.taotao.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

public class AgentExample {

    public static void main(String[] args) throws Exception {
        // 创建模型实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 创建 Agent
        // 构建具有推理和行动能力的智能代理，遵循 ReAct（推理 + 行动）范式，用于迭代解决问题
        ReactAgent agent = ReactAgent.builder()
                .name("weather_agent")
                .model(chatModel) // 设置大模型
                .instruction("你是个很有帮助的天气预报助理")
                .build();

        // 运行 Agent
        AssistantMessage call = agent.call("2024年2月31号，杭州的天气如何？");
        System.out.println(call.getText());
    }
}