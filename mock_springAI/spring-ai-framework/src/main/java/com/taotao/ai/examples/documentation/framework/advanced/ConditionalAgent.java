package com.taotao.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 条件路由Agent：根据条件函数选择不同的Agent分支
 */
public class ConditionalAgent extends FlowAgent {

    private final Predicate<Map<String, Object>> condition;
    private final Agent trueAgent;
    private final Agent falseAgent;

    protected ConditionalAgent(ConditionalAgentBuilder builder) throws GraphStateException {
        super(builder.name, builder.description, builder.compileConfig,
                List.of(builder.trueAgent, builder.falseAgent));
        this.condition = builder.condition;
        this.trueAgent = builder.trueAgent;
        this.falseAgent = builder.falseAgent;
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
            throws GraphStateException {
        // 使用 FlowGraphBuilder 构建自定义图结构
        return FlowGraphBuilder.buildGraph(FlowAgentEnum.SEQUENTIAL.getType(), config);
        // 使用 FlowGraphBuilder 构建自定义图结构
        // return FlowGraphBuilder.buildConditionalGraph(
        //         config,
        //         this.condition,
        //         this.trueAgent,
        //         this.falseAgent
        // );
    }

    public static ConditionalAgentBuilder builder() {
        return new ConditionalAgentBuilder();
    }

    /**
     * Builder for ConditionalAgent
     */
    public static class ConditionalAgentBuilder
            extends FlowAgentBuilder<ConditionalAgent, ConditionalAgentBuilder> {

        private Predicate<Map<String, Object>> condition;
        private Agent trueAgent;
        private Agent falseAgent;

        public ConditionalAgentBuilder condition(Predicate<Map<String, Object>> condition) {
            this.condition = condition;
            return this;
        }

        public ConditionalAgentBuilder trueAgent(Agent trueAgent) {
            this.trueAgent = trueAgent;
            return this;
        }

        public ConditionalAgentBuilder falseAgent(Agent falseAgent) {
            this.falseAgent = falseAgent;
            return this;
        }

        @Override
        public ConditionalAgent build() throws GraphStateException {
            if (condition == null || trueAgent == null || falseAgent == null) {
                throw new IllegalStateException(
                        "Condition, trueAgent and falseAgent must be set");
            }
            return new ConditionalAgent(this);
        }

        @Override
        protected ConditionalAgentBuilder self() {
            return this;
        }
    }
}