/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taotao.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 核心库概念指南示例
 * 演示 State、Nodes、Edges 的基本用法
 */
public class CoreLibraryExample {

	/**
	 * 示例 A: 使用 AppendStrategy
	 */
	public static KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};
	}

	/**
	 * 示例 B: 自定义 KeyStrategyFactory
	 */
	public static KeyStrategyFactory createCustomKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("property", (oldValue, newValue) ->
					((String) newValue).toUpperCase()
			);
			return keyStrategyMap;
		};
	}

	/**
	 * 基本节点示例
	 */
	public static void basicNodeExample() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

		var myNode = node_async(state -> {
			System.out.println("In myNode: ");
			String input = (String) state.value("input").orElse("");
			return Map.of("results", "Hello " + input);
		});

		var myOtherNode = node_async(state -> Map.of());

		StateGraph builder = new StateGraph(keyStrategyFactory)
				.addNode("myNode", myNode)
				.addNode("myOtherNode", myOtherNode)
				.addEdge(START, "myNode")
				.addEdge("myNode", "myOtherNode")
				.addEdge("myOtherNode", END);

		CompiledGraph graph = builder.compile();
		RunnableConfig config = RunnableConfig.builder()
				.threadId("replace-strategy-demo")
				.build();

		// 执行图
		Optional<OverAllState> result = graph.invoke(Map.of(
				"input", "请分析2024年AI行业发展趋势"
		), config);
		// 执行结果
		result.ifPresent(state -> {
			state.value("input").ifPresent(System.out::println);
			state.value("results").ifPresent(System.out::println);
		});
		System.out.println("Graph compiled successfully");
	}

	/**
	 * 使用 RemoveByHash 删除消息示例
	 */
	public static void removeMessagesExample() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

		var workflow = new StateGraph(keyStrategyFactory)
				.addNode("agent_1", node_async(state ->
						Map.of("messages", "message1")))
				.addNode("agent_2", node_async(state ->
						Map.of("messages", "message2.1")))
				.addNode("agent_3", node_async(state ->
						Map.of("messages", RemoveByHash.of("message2.1"))))
				.addEdge(START, "agent_1")
				.addEdge("agent_1", "agent_2")
				.addEdge("agent_2", "agent_3")
				.addEdge("agent_3", END);

		CompiledGraph graph = workflow.compile();
		Optional<OverAllState> result = graph.invoke(Map.of(), RunnableConfig.builder()
				.threadId("replace-strategy-demo")
				.build());
		result.ifPresent(state -> {
			state.value("input").ifPresent(System.out::println);
			state.value("messages").ifPresent(System.out::println);
		});
		System.out.println("Remove messages graph compiled successfully");
	}

	public static void replaceStrategyExample2() throws GraphStateException {
		// 定义状态策略，使用 ReplaceStrategy
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("value", new ReplaceStrategy()); // 使用替换策略
			return keyStrategyMap;
		};

		// 节点 A：返回 value = "初始值"
		var nodeA = node_async(state -> {
			return Map.of("value", "初始值");
		});

		// 节点 B：返回 value = "更新后的值"（会覆盖节点 A 的值）
		var nodeB = node_async(state -> {
			return Map.of("value", "更新后的值");
		});

		// 构建图
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("node_a", nodeA)
				.addNode("node_b", nodeB)
				.addEdge(START, "node_a")
				.addEdge("node_a", "node_b")
				.addEdge("node_b", END);

		// 编译并执行
		CompiledGraph graph = stateGraph.compile();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("replace-strategy-demo")
				.build();

		// 执行图
		Optional<OverAllState> stateOptional = graph.invoke(Map.of(), config);

		// 获取最终状态
		System.out.println("最终状态中的 value: " + stateOptional.get().value("value"));
		// 输出: 最终状态中的 value: 更新后的值
		// 注意：节点 A 的值 "初始值" 已被节点 B 的值 "更新后的值" 完全替换
	}

	public static void appendStrategyExample3() throws GraphStateException {
		// 定义状态策略，使用 AppendStrategy
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy()); // 使用追加策略
			return keyStrategyMap;
		};

		// 节点 A：返回 messages = "消息1"
		var nodeA = node_async(state -> {
			return Map.of("messages", "消息1");
		});

		// 节点 B：返回 messages = "消息2"（会追加到节点 A 的值之后）
		var nodeB = node_async(state -> {
			return Map.of("messages", "消息2");
		});

		// 节点 C：返回 messages = "消息3"（会追加到之前的消息之后）
		var nodeC = node_async(state -> {
			return Map.of("messages", "消息3");
		});

		// 构建图
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("node_a", nodeA)
				.addNode("node_b", nodeB)
				.addNode("node_c", nodeC)
				.addEdge(START, "node_a")
				.addEdge("node_a", "node_b")
				.addEdge("node_b", "node_c")
				.addEdge("node_c", END);

		// 编译并执行
		CompiledGraph graph = stateGraph.compile();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("append-strategy-demo")
				.build();

		// 执行图
		Optional<OverAllState> stateOptional = graph.invoke(Map.of(), config);

		// 获取最终状态
		List<String> messages = (List<String>) stateOptional.get().value("messages").orElse(List.of());
		System.out.println("最终状态中的 messages: " + messages);
		// 输出: 最终状态中的 messages: [消息1, 消息2, 消息3]
		// 注意：所有节点的值都被追加到列表中，而不是被替换
	}

	/**
	 * 条件边示例
	 */
	public static void conditionalEdgesExample() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

		var workflow = new StateGraph(keyStrategyFactory)
				.addNode("nodeA", node_async(state -> Map.of("data", "A")))
				.addNode("nodeB", node_async(state -> Map.of("data", "B")))
				.addNode("nodeC", node_async(state -> Map.of("data", "C")))
				.addEdge(START, "nodeA")
				// 添加条件边
				.addConditionalEdges("nodeA", edge_async(state -> "nodeB"),
						Map.of("nodeB", "nodeB", "nodeC", "nodeC"))
				.addEdge("nodeB", END)
				.addEdge("nodeC", END);

		CompiledGraph graph = workflow.compile();
		System.out.println("Conditional edges graph compiled successfully");
	}

	public static void main(String[] args) {
		System.out.println("=== 核心库概念示例 ===\n");

		try {
			// 示例 1: 基本节点示例
			System.out.println("示例 1: 基本节点示例");
			basicNodeExample();
			System.out.println();

			// 示例 2: 使用 RemoveByHash 删除消息示例
			System.out.println("示例 2: 使用 RemoveByHash 删除消息示例");
			removeMessagesExample();
			System.out.println();
			// 示例 2: 使用 ReplaceStrategy 覆盖策略
			System.out.println("示例 2: 使用 ReplaceStrategy 覆盖消息示例");
			replaceStrategyExample2();
			System.out.println();
			// 示例 2: 使用 AppendStrategy 追加策略
			System.out.println("示例 2: 使用 AppendStrategy 追加消息示例");
			appendStrategyExample3();
			System.out.println();

			// 示例 3: 条件边示例
			System.out.println("示例 3: 条件边示例");
			conditionalEdgesExample();
			System.out.println();

			System.out.println("所有示例执行完成");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

