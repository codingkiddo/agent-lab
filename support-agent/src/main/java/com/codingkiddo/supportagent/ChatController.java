package com.codingkiddo.supportagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	record ChatRequest(String message) {
	}

	record ChatResponse(String reply, String agentVersion) {
	} // ← now includes the version

	private final ChatClient chatClient;
	private final AgentConfig config;

	public ChatController(ChatClient.Builder builder, OrderTools orderTools, AgentConfig config) {
		this.config = config;

		// 1. Keep only the tools this version allows
		ToolCallback[] enabledTools = Arrays.stream(ToolCallbacks.from(orderTools))
				.filter(tool -> config.tools().contains(tool.getToolDefinition().name())).toArray(ToolCallback[]::new);

		// 2. Log exactly what was loaded, so you can always tell which version is
		// running
		log.info("Loaded agent {} | model={} | temperature={} | tools={}", config.version(), config.model(),
				config.temperature(), Arrays.stream(enabledTools).map(t -> t.getToolDefinition().name()).toList());

		// 3. Build the client from config, not from hard-coded values
		this.chatClient = builder.defaultSystem(config.systemPrompt())
				.defaultOptions(ChatOptions.builder().model(config.model()).temperature(config.temperature()))
				.defaultToolCallbacks(enabledTools).build();
	}

	@PostMapping("/chat")
	public ChatResponse chat(@RequestBody ChatRequest request) {
		String reply = chatClient.prompt().user(request.message()).call().content();
		return new ChatResponse(reply, config.version());
	}
}