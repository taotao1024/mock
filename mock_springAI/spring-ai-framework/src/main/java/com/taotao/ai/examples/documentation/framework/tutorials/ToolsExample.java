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
package com.taotao.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Tools Tutorial - 完整代码示例
 * 展示如何创建和使用Tools让Agent与外部系统交互
 *
 * 来源：tools.md
 */
public class ToolsExample {

	// ==================== 基础工具定义 ====================

	/**
	 * 示例1：编程方式规范 - FunctionToolCallback
	 * <p>
	 * name: 工具的名称。AI 模型使用此名称在调用时识别工具。因此，在同一上下文中不允许有两个同名的工具。对于特定的聊天请求，名称在模型可用的所有工具中必须是唯一的。必需。<p>
	 * toolFunction: 表示工具方法的函数对象（Function、Supplier、Consumer 或 BiFunction）。必需。<p>
	 * description: 工具的描述，模型可以使用它来了解何时以及如何调用工具。如果未提供，将使用方法名称作为工具描述。但是，强烈建议提供详细描述，因为这对于模型理解工具的目的和使用方式至关重要。如果未提供良好的描述，可能导致模型在应该使用工具时不使用，或者使用不正确。<p>
	 * inputType: 函数输入的类型。必需。<p>
	 * inputSchema: 工具输入参数的 JSON schema。如果未提供，将根据 inputType 自动生成 schema。你可以使用 @ToolParam 注解提供有关输入参数的附加信息，例如描述或参数是必需还是可选。默认情况下，所有输入参数都被视为必需。<p>
	 * toolMetadata: 定义附加设置的 ToolMetadata 实例，例如是否应将结果直接返回给客户端，以及要使用的结果转换器。你可以使用 ToolMetadata.Builder 类构建它。<p>
	 * toolCallResultConverter: 用于将工具调用结果转换为字符串对象以发送回 AI 模型的 ToolCallResultConverter 实例。如果未提供，将使用默认转换器（DefaultToolCallResultConverter）。<p>
	 */
	public static void programmaticToolSpecification() {
		ToolCallback toolCallback = FunctionToolCallback
				.builder("currentWeather", new WeatherService())
				.description("Get the weather in location")
				.inputType(WeatherRequest.class)
				.build();
	}

	/**
	 * 示例2：添加工具到 ChatClient（使用编程规范）
	 */
	public static void addToolToChatClient() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback toolCallback = FunctionToolCallback
				.builder("currentWeather", new WeatherService())
				.description("Get the weather in location")
				.inputType(WeatherRequest.class)
				.build();

		// Note: ChatClient usage would be shown here in actual implementation
		// This is a simplified example
	}

	/**
	 * 示例3：自定义工具名称
	 */
	public static void customToolName() {
		ToolCallback searchTool = FunctionToolCallback
				.builder("web_search", new SearchFunction())  // 自定义名称
				.description("Search the web for information")
				.inputType(String.class)
				.build();

		System.out.println(searchTool.getToolDefinition().name());  // web_search
	}

	/**
	 * 示例4：自定义工具描述
	 */
	public static void customToolDescription() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback calculatorTool = FunctionToolCallback
				.builder("calculator", new CalculatorFunction())
				.description("Performs arithmetic calculations. Use this for any math problems.")
				.inputType(String.class)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("计算器")
				.model(chatModel)
				.tools(calculatorTool) // 查询工具
				.interceptors(new AgentsExample.ToolErrorInterceptor()) // 工具错误处理
				.build();

//		ChatOptions chatOptions = ToolCallingChatOptions.builder()
//				.toolCallbacks(calculatorTool)
//				.build();
//		Prompt prompt = new Prompt("1+3=?", chatOptions);
//		chatModel.call(prompt);
		System.out.println(agent.call("1+3=?").getText());
	}

	/**
	 * 示例5：高级模式定义
	 */
	public static void advancedSchemaDefinition() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback weatherTool = FunctionToolCallback
				.builder("get_weather", new WeatherFunction())
				.description("Get current weather and optional forecast")
				.inputType(WeatherInput.class)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("天气助手")
				.model(chatModel)
				.tools(weatherTool) // 查询工具
				.interceptors(new AgentsExample.ToolErrorInterceptor()) // 工具错误处理
				.build();

		WeatherInput weatherInput = new WeatherInput("俄罗斯",Unit.C,true);

		System.out.println(agent.call(weatherInput.toString()).getText());
	}

	/**
	 * 示例6：访问状态
	 */
	public static void accessingState() {
		// 创建工具
		ToolCallback summaryTool = FunctionToolCallback
				.builder("summarize_conversation", new ConversationSummaryTool())
				.description("Summarize the conversation so far")
				.inputType(String.class)
				.build();
	}

	// ==================== 自定义工具属性 ====================

	/**
	 * 示例7：访问上下文
	 */
	public static void accessingContext() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback accountTool = FunctionToolCallback
				.builder("get_account_info", new AccountInfoTool())
				.description("Get the current user's account information")
				.inputType(String.class)
				.build();

		// 在 ReactAgent 中使用
		ReactAgent agent = ReactAgent.builder()
				.name("financial_assistant")
				.model(chatModel)
				.tools(accountTool)
				.systemPrompt("You are a financial assistant.")
				.build();

		// 调用时传递上下文
		RunnableConfig config = RunnableConfig.builder()
				.addMetadata("user_id", "user123")
				.build();

		agent.call("question", config);
	}

	/**
	 * 示例8：使用存储访问跨对话的持久数据
	 */
	public static void accessingMemoryStore() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 配置持久化存储
		MemorySaver memorySaver = new MemorySaver();

		// 创建工具
		ToolCallback saveUserInfoTool = createSaveUserInfoTool();
		ToolCallback getUserInfoTool = createGetUserInfoTool();

		// 创建带有持久化记忆的 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(saveUserInfoTool, getUserInfoTool)
				.saver(memorySaver)
				.build();

		// 第一个会话：保存用户信息
		RunnableConfig config1 = RunnableConfig.builder()
				.threadId("session_1")
				.build();

		AssistantMessage call1 = agent.call("Save user: userid: abc123, name: Foo, age: 25, email: foo@example.com", config1);
		System.out.println(call1.getText());

		// 第二个会话：获取用户信息，注意这里用的是不同的 threadId
		RunnableConfig config2 = RunnableConfig.builder()
				.threadId("session_2")
				.build();

		AssistantMessage call2 = agent.call("Get user info for user with id 'abc123'", config2);
		System.out.println(call2.getText());
		// 输出：User information has been successfully saved:
		// - User ID: abc123
		// - Name: Foo
		// - Age: 25
		// - Email: foo@example.com
		// The information for user with ID 'abc123' has been retrieved. Let me know if you need further details or assistance!
	}

	/**
	 * 示例9：在 ReactAgent 中使用工具
	 */
	public static void toolsInReactAgent() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具
		ToolCallback weatherTool = FunctionToolCallback
				.builder("get_weather", new WeatherFunction())
				.description("Get weather for a given city")
				.inputType(WeatherInput.class)
				.build();

		ToolCallback searchTool = FunctionToolCallback
				.builder("search", new SearchFunction())
				.description("Search for information")
				.inputType(String.class)
				.build();

		// 创建带有工具的 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(weatherTool, searchTool)
				// 你是一个有用的助理，可以使用天气和搜索工具。
				.systemPrompt("You are a helpful assistant with access to weather and search tools.")
				.saver(new MemorySaver())
				.build();

		// 使用 Agent
		AssistantMessage response = agent.call("What's the weather like in San Francisco?");
		System.out.println(response.getText());
		// 输出: The current weather in San Francisco is 22 degrees Fahrenheit. It's quite cold, so make sure to bundle up if you're heading out!
	}

	/**
	 * 示例10：完整的工具使用示例
	 */
	public static void comprehensiveToolExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 定义多个工具
		ToolCallback weatherTool = FunctionToolCallback
				.builder("get_weather", new WeatherFunction())
				.description("Get current weather and optional forecast for a city")
				.inputType(WeatherInput.class)
				.build();

		ToolCallback calculatorTool = FunctionToolCallback
				.builder("calculator", new CalculatorFunction())
				.description("Perform arithmetic calculations")
				.inputType(String.class)
				.build();

		ToolCallback searchTool = FunctionToolCallback
				.builder("web_search", new SearchFunction())
				.description("Search the web for information")
				.inputType(String.class)
				.build();

		// 创建 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.tools(weatherTool, calculatorTool, searchTool)
				.systemPrompt("""
						You are a helpful AI assistant with access to multiple tools:
						- Weather information
						- Calculator for math operations
						- Web search for general information
						
						Use the appropriate tool based on the user's question.
						""")
				.saver(new MemorySaver())
				.build();

		// 使用不同的工具
		RunnableConfig config = RunnableConfig.builder()
				.threadId("session_1")
				.build();

		AssistantMessage call1 = agent.call("What's the weather in New York?", config);
		AssistantMessage call2 = agent.call("Calculate 25 * 4 + 10", config);
		AssistantMessage call3 = agent.call("Search for latest AI news", config);

		System.out.println(call1.getText());
		System.out.println(call2.getText());
		System.out.println(call3.getText());
	}

	// ==================== 高级模式定义 ====================

	/**
	 * 创建保存用户信息工具
	 */
	private static ToolCallback createSaveUserInfoTool() {
		return FunctionToolCallback.builder("save_user_info", (String input) -> {
					// 简化的实现
					return "User info saved: " + input;
				})
				.description("Save user information")
				.inputType(String.class)
				.build();
	}

	/**
	 * 创建获取用户信息工具
	 */
	private static ToolCallback createGetUserInfoTool() {
		return FunctionToolCallback.builder("get_user_info", (String userId) -> {
					// 简化的实现
					return "User info for: " + userId;
				})
				.description("Get user information by ID")
				.inputType(String.class)
				.build();
	}

	public static void main(String[] args) {
		System.out.println("=== Tools Tutorial Examples ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
//			System.out.println("\n--- 示例1：编程式工具规范 ---");
//			programmaticToolSpecification();
//
//			System.out.println("\n--- 示例2：添加工具到 ChatClient ---");
//			addToolToChatClient();
//
//			System.out.println("\n--- 示例3：自定义工具名称 ---");
//			customToolName();
//
//			System.out.println("\n--- 示例4：自定义工具描述 ---");
//			customToolDescription();

			System.out.println("\n--- 示例5：高级 Schema 定义 ---");
			advancedSchemaDefinition();

//			System.out.println("\n--- 示例6：访问状态 ---");
//			accessingState();
//
//			System.out.println("\n--- 示例7：访问上下文 ---");
//			accessingContext();
//
//			System.out.println("\n--- 示例8：访问内存存储 ---");
//			accessingMemoryStore();
//
//			System.out.println("\n--- 示例9：ReactAgent 中的工具 ---");
//			toolsInReactAgent();
//
//			System.out.println("\n--- 示例10：综合工具示例 ---");
//			comprehensiveToolExample();

			System.out.println("\n=== 所有示例执行完成 ===");
		}
		catch (Exception e) {
			System.err.println("执行示例时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public enum Unit {C, F}

	// ==================== 访问上下文 ====================

	public enum UnitType {CELSIUS, FAHRENHEIT}

	/**
	 * 天气服务
	 */
	public static class WeatherService implements Function<WeatherRequest, WeatherResponse> {
		@Override
		public WeatherResponse apply(WeatherRequest request) {
			return new WeatherResponse(30.0, Unit.C);
		}
	}

	// ==================== Context（上下文） ====================

	public record WeatherRequest(
			@ToolParam(description = "城市或坐标") String location,
			Unit unit
	) { }

	public record WeatherResponse(double temp, Unit unit) { }

	// ==================== Memory（存储） ====================

	/**
	 * 搜索函数
	 */
	public static class SearchFunction implements Function<String, String> {
		@Override
		public String apply(String query) {
			return "Search results for: " + query;
		}
	}

	// ==================== 在 ReactAgent 中使用工具 ====================

	/**
	 * 计算器函数
	 */
	public static class CalculatorFunction implements Function<String, String> {
		@Override
		public String apply(String expression) {
			// 简化的计算逻辑
			return "Result: " + expression;
		}
	}

	// ==================== 完整示例 ====================

	/**
	 * 天气输入（使用记录类）
	 */
	public record WeatherInput(
			@ToolParam(description = "City name or coordinates") String location,
			@ToolParam(description = "Temperature unit preference") Unit units,
			@ToolParam(description = "Include 5-day forecast") boolean includeForecast
	) { }

	// ==================== 辅助方法 ====================

	/**
	 * 天气函数（高级版）
	 */
	public static class WeatherFunction implements Function<WeatherInput, String> {
		@Override
		public String apply(WeatherInput input) {
			double temp = input.units() == Unit.F ? 22 : 72;
			String result = String.format(
					"Current weather in %s: %.0f degrees %s",
					input.location(),
					temp,
					input.units().toString().substring(0, 1).toUpperCase()
			);

			if (input.includeForecast()) {
				result += "\nNext 5 days: Sunny";
			}

			return result;
		}
	}

	/**
	 * 对话摘要工具
	 */
	public static class ConversationSummaryTool implements BiFunction<String, ToolContext, String> {

		@Override
		public String apply(String input, ToolContext toolContext) {
			OverAllState state = (OverAllState) toolContext.getContext().get("state");
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");

			// 从state中获取消息
			Optional<Object> messagesOpt = state.value("messages");
			List<Message> messages = messagesOpt.isPresent()
					? (List<Message>) messagesOpt.get()
					: new ArrayList<>();

			if (messages.isEmpty()) {
				return "No conversation history available";
			}

			long userMsgs = messages.stream()
					.filter(m -> m.getMessageType().getValue().equals("user"))
					.count();
			long aiMsgs = messages.stream()
					.filter(m -> m.getMessageType().getValue().equals("assistant"))
					.count();
			long toolMsgs = messages.stream()
					.filter(m -> m.getMessageType().getValue().equals("tool"))
					.count();

			return String.format(
					"Conversation has %d user messages, %d AI responses, and %d tool results",
					userMsgs, aiMsgs, toolMsgs
			);
		}
	}

	// ==================== Main 方法 ====================

	/**
	 * 账户信息工具
	 */
	public static class AccountInfoTool implements BiFunction<String, ToolContext, String> {

		private static final Map<String, Map<String, Object>> USER_DATABASE = Map.of(
				"user123", Map.of(
						"name", "Alice Johnson",
						"account_type", "Premium",
						"balance", 5000,
						"email", "alice@example.com"
				),
				"user456", Map.of(
						"name", "Bob Smith",
						"account_type", "Standard",
						"balance", 1200,
						"email", "bob@example.com"
				)
		);

		@Override
		public String apply(String query, ToolContext toolContext) {
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
			String userId = (String) config.metadata("user_id").orElse(null);

			if (userId == null) {
				return "User ID not provided";
			}

			Map<String, Object> user = USER_DATABASE.get(userId);
			if (user != null) {
				return String.format(
						"Account holder: %s\nType: %s\nBalance: $%d",
						user.get("name"),
						user.get("account_type"),
						user.get("balance")
				);
			}

			return "User not found";
		}
	}
}

