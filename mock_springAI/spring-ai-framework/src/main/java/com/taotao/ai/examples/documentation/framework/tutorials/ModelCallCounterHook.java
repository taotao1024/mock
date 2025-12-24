package com.taotao.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * context() 是共享的: 同一个执行流程中的所有 Hook 共享同一个 context
 * 数据持久性: context 中的数据在整个 Agent 执行期间保持有效
 * 类型安全: 需要自己管理 context 中数据的类型转换
 * 命名约定: 建议使用双下划线前缀命名 context key（如 __model_call_count__）以避免与用户数据冲突
 */
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ModelCallCounterHook extends ModelHook {

    private static final String CALL_COUNT_KEY = "__model_call_count__";
    private static final String TOTAL_TIME_KEY = "__total_model_time__";
    private static final String START_TIME_KEY = "__call_start_time__";

    @Override
    public String getName() {
        return "model_call_counter";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 从 context 读取当前计数（如果不存在则默认为 0）
        int currentCount = config.context().containsKey(CALL_COUNT_KEY)
                ? (int) config.context().get(CALL_COUNT_KEY) : 0;

        System.out.println("模型调用 #" + (currentCount + 1));

        // 记录开始时间
        config.context().put(START_TIME_KEY, System.currentTimeMillis());

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 读取当前计数并递增
        int currentCount = config.context().containsKey(CALL_COUNT_KEY)
                ? (int) config.context().get(CALL_COUNT_KEY) : 0;
        config.context().put(CALL_COUNT_KEY, currentCount + 1);

        // 计算本次调用耗时并累加到总耗时
        if (config.context().containsKey(START_TIME_KEY)) {
            long startTime = (long) config.context().get(START_TIME_KEY);
            long duration = System.currentTimeMillis() - startTime;

            long totalTime = config.context().containsKey(TOTAL_TIME_KEY)
                    ? (long) config.context().get(TOTAL_TIME_KEY) : 0L;
            config.context().put(TOTAL_TIME_KEY, totalTime + duration);

            // 输出统计信息
            int newCount = currentCount + 1;
            long newTotalTime = totalTime + duration;
            System.out.println("模型调用完成: " + duration + "ms");
            System.out.println("累计统计 - 调用次数: " + newCount + ", 总耗时: " + newTotalTime + "ms, 平均: " + (newTotalTime / newCount) + "ms");
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}