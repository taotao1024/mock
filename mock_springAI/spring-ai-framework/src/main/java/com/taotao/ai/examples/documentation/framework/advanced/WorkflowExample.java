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
package com.taotao.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 工作流（Workflow）示例
 *
 * 演示如何使用StateGraph构建智能工作流，包括：
 * 1. 定义自定义Node
 * 2. Agent作为Node
 * 3. 混合使用Agent Node和普通Node
 * 4. 执行工作流
 *
 * 参考文档: advanced_doc/workflow.md
 */
public class WorkflowExample {

	private final ChatModel chatModel;

	public WorkflowExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示例
	 *
	 * 注意：需要配置ChatModel实例才能运行
	 */
	public static void main(String[] args) {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		if (chatModel == null) {
			System.err.println("错误：请先配置ChatModel实例");
			System.err.println("请设置 AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		WorkflowExample example = new WorkflowExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	/**
	 * 示例1：基础Node定义
	 *
	 * 创建简单的文本处理Node
	 */
	public void example1_basicNode() {
		class TextProcessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				// 1. 从状态中获取输入
				String input = state.value("query", "").toString();

				// 2. 执行业务逻辑
				String processedText = input.toUpperCase().trim();

				// 3. 返回更新后的状态
				Map<String, Object> result = new HashMap<>();
				result.put("processed_text", processedText);
				return result;
			}
		}

		TextProcessorNode processor = new TextProcessorNode();
		System.out.println("基础Node定义示例完成");
	}

	/**
	 * 示例2：带配置的AI Node
	 *
	 * 创建调用LLM的Node
	 */
	public void example2_aiNode() {
		class QueryExpanderNode implements NodeActionWithConfig {
			private final ChatClient chatClient;
			private final PromptTemplate promptTemplate;

			public QueryExpanderNode(ChatClient.Builder chatClientBuilder) {
				this.chatClient = chatClientBuilder.build();
				this.promptTemplate = new PromptTemplate(
						"你是一个搜索优化专家。请为以下查询生成 {number} 个不同的变体。\n" +
								"原始查询：{query}\n\n" +
								"查询变体：\n"
				);
			}

			@Override
			public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
				// 获取输入参数
				String query = state.value("query", "").toString();
				Integer number = (Integer) state.value("expanderNumber", 3);

				// 调用 LLM
				String result = chatClient.prompt()
						.user(user -> user
								.text(promptTemplate.getTemplate())
								.param("query", query)
								.param("number", number))
						.call()
						.content();

				// 处理结果
				String[] variants = result.split("\n");

				// 返回更新的状态
				Map<String, Object> output = new HashMap<>();
				output.put("queryVariants", Arrays.asList(variants));
				return output;
			}
		}

		QueryExpanderNode expander = new QueryExpanderNode(ChatClient.builder(chatModel));
		System.out.println("AI Node示例完成");
	}

	/**
	 * 示例3：条件评估Node
	 *
	 * 用于工作流中的条件分支判断
	 */
	public void example3_conditionNode() {
		class ConditionEvaluatorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString().toLowerCase();

				// 根据输入内容决定路由
				String route;
				if (input.contains("错误") || input.contains("异常")) {
					route = "error_handling";
				}
				else if (input.contains("数据") || input.contains("分析")) {
					route = "data_processing";
				}
				else if (input.contains("报告") || input.contains("总结")) {
					route = "report_generation";
				}
				else {
					route = "default";
				}

				Map<String, Object> result = new HashMap<>();
				result.put("_condition_result", route);
				return result;
			}
		}

		ConditionEvaluatorNode evaluator = new ConditionEvaluatorNode();
		System.out.println("条件评估Node示例完成");
	}

	/**
	 * 示例4：并行结果聚合Node
	 *
	 * 用于收集和聚合并行执行的多个Node的结果
	 */
	public void example4_aggregatorNode() {
		class ParallelResultAggregatorNode implements NodeAction {
			private final String outputKey;

			public ParallelResultAggregatorNode(String outputKey) {
				this.outputKey = outputKey;
			}

			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				// 收集所有并行任务的结果
				List<String> results = new ArrayList<>();

				// 假设并行任务将结果存储在不同的键中
				state.value("result_1").ifPresent(r -> results.add(r.toString()));
				state.value("result_2").ifPresent(r -> results.add(r.toString()));
				state.value("result_3").ifPresent(r -> results.add(r.toString()));

				// 聚合结果
				String aggregatedResult = String.join("\n---\n", results);

				Map<String, Object> output = new HashMap<>();
				output.put(outputKey, aggregatedResult);
				return output;
			}
		}

		ParallelResultAggregatorNode aggregator = new ParallelResultAggregatorNode("merged_results");
		System.out.println("聚合Node示例完成");
	}

	/**
	 * 示例5：集成自定义Node到StateGraph
	 * <p>
	 * 构建包含自定义Node的工作流
	 * <p>
	 * Node 开发最佳实践
	 * 单一职责：每个 Node 应该只负责一个明确的任务
	 * 状态不可变：不要直接修改输入的 state，而是返回新的状态更新
	 * 异常处理：在 Node 内部处理可预见的异常，避免中断整个流程
	 * 日志记录：添加适当的日志以便调试和监控
	 * 参数验证：在处理前验证从状态中获取的参数
	 */
	public void example5_buildWorkflowWithCustomNodes() throws Exception {
		// 定义状态管理策略
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                HashMap<String, KeyStrategy> strategies = new HashMap<>();
                strategies.put("query", new ReplaceStrategy()); // 查询
                strategies.put("processed_text", new ReplaceStrategy()); // 处理过的文本
                strategies.put("queryVariants", new ReplaceStrategy()); // 查询变体
                strategies.put("final_result", new ReplaceStrategy()); // 最终结果
                return strategies;
            }
        };

		// 创建Node实例
		class TextProcessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("query", "").toString();
				String processed = input.toUpperCase().trim();
				return Map.of("processed_text", processed);
			}
		}

		class ConditionNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("processed_text", "").toString();
				String route = input.length() > 10 ? "long" : "short";
				return Map.of("_condition_result", route);
			}
		}

		class RobustNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				try {
					// 参数验证
					String input = (String) state.value("input")
							.orElseThrow(() -> new IllegalArgumentException("Missing 'input' in state"));

					System.out.printf("Processing input: %s \n", input);

					// 业务逻辑
					String result = processInput(input);

					// 返回结果
					Map<String, Object> output = new HashMap<>();
					output.put("output", result);
					return output;

				} catch (Exception e) {
					System.out.printf("Error in RobustNode: %s \n", e);
					// 返回错误信息而不是抛出异常
					Map<String, Object> errorOutput = new HashMap<>();
					errorOutput.put("error", e.getMessage());
					return errorOutput;
				}
			}

			private String processInput(String input) {
				// 具体业务逻辑
				return input;
			}
		}

		// 构建 StateGraph
		StateGraph graph = new StateGraph(keyStrategyFactory);

		// 添加自定义 Node
		graph.addNode("processor", node_async(new TextProcessorNode()));
		graph.addNode("condition", node_async(new ConditionNode()));

		// 定义边（流程连接）
		graph.addEdge(StateGraph.START, "processor");
		graph.addEdge("processor", "condition");

		// 条件边：根据 condition node 的结果路由
		graph.addConditionalEdges(
				"condition",
				edge_async(state -> state.value("_condition_result", "short").toString()),
				Map.of(
						"long", "processor",  // 长文本重新处理
						"short", StateGraph.END  // 短文本结束
				)
		);

		System.out.println("自定义Node工作流构建完成");
	}

	/**
	 * 示例6：Agent作为SubGraph Node
	 *
	 * 将ReactAgent嵌入到工作流中
	 */
	public void example6_agentAsNode() throws Exception {
		// 创建专门的数据分析 Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("analysis")
				.model(chatModel)
				.instruction("你是一个数据分析专家，负责分析数据并提供洞察")
//				.instruction("你是一个数据分析专家，负责分析数据并提供洞察，请分析以下输入数据：{input}")
				.build();

		// 创建报告生成 Agent
		ReactAgent reportAgent = ReactAgent.builder()
				.name("reporting")
				.model(chatModel)
				.instruction("你是一个报告生成专家，负责将分析结果转化为专业报告")
//				.instruction("你是一个报告生成专家，负责将分析结果 {analysis_result} 转化为专业报告")
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy()); // 输入
			strategies.put("analysis_result", new ReplaceStrategy()); // 分析结果
			strategies.put("final_report", new ReplaceStrategy()); // 最终报告
			return strategies;
		};

		// 构建包含 Agent 的工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 将 Agent 作为 SubGraph Node 添加
		workflow.addNode("analysis", analysisAgent.asNode(
				true,                     // includeContents: 是否传递父图的消息历史
				false,                    // returnReasoningContents: 是否返回推理过程
				"analysis_result"         // outputKeyToParent: 输出键名
		));

		workflow.addNode("reporting", reportAgent.asNode(
				true,
				false,
				"final_report"
		));

		// 定义流程
		workflow.addEdge(StateGraph.START, "analysis");
		workflow.addEdge("analysis", "reporting");
		workflow.addEdge("reporting", StateGraph.END);

		System.out.println("Agent作为Node工作流构建完成");
		// 编译并执行工作流
		CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
		String inputString = "2025年全年销量100亿，毛利率 23%，净利率 13%。2024年全年销量80亿，毛利率 20%，净利率 8%。";
		NodeOutput lastOutput = compiledGraph.stream(Map.of("input", inputString))
				.doOnNext(output -> {
					if (output instanceof StreamingOutput<?> streamingOutput) {
						if (streamingOutput.message() != null) {
							// streaming output from streaming llm node
							System.out.println("Streaming output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
						} else {
							// output from normal node, investigate the state to get the node data
							System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.state().data());
						}
					}
				})
				.blockLast();

		System.out.println("最终结果，包含所有节点状态：" + lastOutput.state().data());

		/*// 配置运行参数
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId("workflow-001")
				.build();
		// 准备输入
		Map<String, Object> input = Map.of(
				"input", "2025年全年销量100亿，毛利率 23%，净利率 13%。2024年全年销量80亿，毛利率 20%，净利率 8%。"
		);
		// 执行工作流（同步调用）
		Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

		// 处理结果
		result.ifPresent(state -> {
			System.out.println("输入: " + state.value("input").orElse("无"));
			System.out.println("输出: " + state.value("output").orElse("无"));
		});*/
	}

	/**
	 * 示例7：混合使用Agent Node和普通Node
	 * <p>
	 * 在工作流中结合Agent和自定义Node
	 * <p>
	 * Agent Node
	 * 1.明确角色定位：为每个 Agent 设置清晰的职责和指令，使用 .instruction() 方法定义 Agent 的角色
	 * 2.合理配置工具：只为 Agent 提供其角色所需的工具，避免工具过多导致选择困难
	 * 3.设置输出键名：使用 .outputKey() 方法为 Agent 设置输出键名，便于在工作流中访问结果
	 * 4.控制上下文传递：根据需要配置 includeContents 参数，避免不必要的上下文传递
	 * 5.优化输出格式：使用 returnReasoningContents 控制返回内容的详细程度
	 * 6.启用日志记录：使用 .enableLogging(true) 启用日志，便于调试和监控
	 * 7.错误处理：在 Agent 外层添加错误处理 Node，确保流程的健壮性
	 * 8.流式处理：使用 compiledGraph.stream() 进行流式处理，实时获取节点输出；使用 compiledGraph.invoke() 进行同步调用
	 *
	 * 性能优化建议
	 * 对于包含多个 Agent 的复杂工作流：
	 * <p>
	 * 1.并行执行：对于相互独立的 Agent，使用并行节点提高效率
	 * 2.缓存结果：对于重复计算，考虑使用状态缓存
	 * 3.超时控制：为每个 Agent 设置合理的超时时间
	 * 4.资源管理：合理配置 ChatModel 的连接池和并发参数
	 */
	public void example7_hybridWorkflow() throws Exception {
		// 创建 Agent
		ReactAgent qaAgent = ReactAgent.builder()
				.name("qa_agent")
				.model(chatModel)
				.instruction("你是一个问答专家")
				.build();

		// 创建自定义 Node
		class PreprocessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString();
				String cleaned = input.trim().toLowerCase();
				return Map.of("cleaned_input", cleaned);
			}
		}

		class ValidatorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String result = state.value("qa_result", "").toString();
				boolean isValid = result.length() > 50; // 简单验证
				return Map.of("is_valid", isValid);
			}
		}

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("cleaned_input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			strategies.put("is_valid", new ReplaceStrategy());
			return strategies;
		};

		// 构建混合工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加普通 Node
		workflow.addNode("preprocess", node_async(new PreprocessorNode()));
		workflow.addNode("validate", node_async(new ValidatorNode()));

		// 添加 Agent Node
		workflow.addNode("qa", qaAgent.asNode(
				true,
				false,
				"qa_result"
		));

		// 定义流程：预处理 -> Agent处理 -> 验证
		workflow.addEdge(StateGraph.START, "preprocess");
		workflow.addEdge("preprocess", "qa");
		workflow.addEdge("qa", "validate");

		// 条件边：验证通过则结束，否则重新处理
		workflow.addConditionalEdges(
				"validate",
				edge_async(state -> (Boolean) state.value("is_valid", false) ? "end" : "qa"),
				Map.of("end", StateGraph.END, "qa", "qa")
		);

		System.out.println("混合工作流构建完成");
	}

	/**
	 * 示例8：执行工作流
	 *
	 * 编译并执行StateGraph工作流
	 */
	public void example8_executeWorkflow() throws Exception {
		// 创建简单的工作流
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("output", new ReplaceStrategy());
			return strategies;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory);

		class SimpleNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString();
				return Map.of("output", "Processed: " + input);
			}
		}

		workflow.addNode("process", node_async(new SimpleNode()));
		workflow.addEdge(StateGraph.START, "process");
		workflow.addEdge("process", StateGraph.END);

		// 编译工作流
		CompileConfig compileConfig = CompileConfig.builder().build();
		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		// 准备输入
		Map<String, Object> input = Map.of(
				"input", "请分析2024年AI行业发展趋势"
		);

		// 配置运行参数
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId("workflow-001")
				.build();

		// 执行工作流
		Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

		// 处理结果
		result.ifPresent(state -> {
			System.out.println("输入: " + state.value("input").orElse("无"));
			System.out.println("输出: " + state.value("output").orElse("无"));
		});

		System.out.println("工作流执行完成");
	}

	/**
	 * 示例9：多Agent协作工作流
	 *
	 * 构建完整的研究工作流
	 */
	public void example9_multiAgentResearchWorkflow() throws Exception {
		// 创建工具（示例）
		ToolCallback searchTool = FunctionToolCallback.builder("search", (args) -> "搜索结果")
				.description("搜索工具")
				.inputType(String.class)
				.build();

		ToolCallback analysisTool = FunctionToolCallback.builder("analysis", (args) -> "分析结果")
				.description("分析工具")
				.inputType(String.class)
				.build();

		ToolCallback summaryTool = FunctionToolCallback.builder("summary", (args) -> "总结结果")
				.description("总结工具")
				.inputType(String.class)
				.build();

		// 1. 创建信息收集 Agent
		ReactAgent researchAgent = ReactAgent.builder()
				.name("researcher")
				.model(chatModel)
				.instruction("你是一个研究专家，负责收集和整理相关信息")
				.tools(searchTool)
				.build();

		// 2. 创建数据分析 Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("analyst")
				.model(chatModel)
				.instruction("你是一个分析专家，负责深入分析研究数据")
				.tools(analysisTool)
				.build();

		// 3. 创建总结 Agent
		ReactAgent summaryAgent = ReactAgent.builder()
				.name("summarizer")
				.model(chatModel)
				.instruction("你是一个总结专家，负责将分析结果提炼为简洁的结论")
				.tools(summaryTool)
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("research_data", new ReplaceStrategy());
			strategies.put("analysis_result", new ReplaceStrategy());
			strategies.put("final_summary", new ReplaceStrategy());
			return strategies;
		};

		// 4. 构建工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加 Agent 节点
		workflow.addNode(researchAgent.name(), researchAgent.asNode(
				true,    // 包含历史消息
				false,   // 不返回推理过程
				"research_data"
		));

		workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
				true,
				false,
				"analysis_result"
		));

		workflow.addNode(summaryAgent.name(), summaryAgent.asNode(
				true,
				true,    // 返回完整推理过程
				"final_summary"
		));

		// 定义顺序执行流程
		workflow.addEdge(StateGraph.START, researchAgent.name());
		workflow.addEdge(researchAgent.name(), analysisAgent.name());
		workflow.addEdge(analysisAgent.name(), summaryAgent.name());
		workflow.addEdge(summaryAgent.name(), StateGraph.END);

		System.out.println("多Agent研究工作流构建完成");

		// 编译并执行工作流
		CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
		NodeOutput finalOutput = compiledGraph.stream(Map.of("input", "帮我做一份关于AI Agent的研究报告"))
				.doOnNext(output -> {
					if (output instanceof StreamingOutput<?> streamingOutput) {
						System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
					}
				})
				.blockLast();

		System.out.println("多Agent研究工作流构建完成");
		System.out.println("最终输出: " + finalOutput.state().value("final_summary").orElse("无"));
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 工作流（Workflow）示例 ===\n");

		try {
//			System.out.println("示例1: 基础Node定义");
//			example1_basicNode();
//			System.out.println();
//
//			System.out.println("示例2: 带配置的AI Node");
//			example2_aiNode();
//			System.out.println();
//
//			System.out.println("示例3: 条件评估Node");
//			example3_conditionNode();
//			System.out.println();
//
//			System.out.println("示例4: 并行结果聚合Node");
//			example4_aggregatorNode();
//			System.out.println();
//
//			System.out.println("示例5: 集成自定义Node到StateGraph");
//			example5_buildWorkflowWithCustomNodes();
//			System.out.println();

//			System.out.println("示例6: Agent作为SubGraph Node");
//			example6_agentAsNode();
//			System.out.println();
//
			System.out.println("示例7: 混合使用Agent Node和普通Node");
			example7_hybridWorkflow();
			System.out.println();
//
			System.out.println("示例8: 执行工作流");
			example8_executeWorkflow();
			System.out.println();
//
//			System.out.println("示例9: 多Agent协作工作流");
//			example9_multiAgentResearchWorkflow();
//			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

