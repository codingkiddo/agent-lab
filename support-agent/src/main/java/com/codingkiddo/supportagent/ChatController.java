package com.codingkiddo.supportagent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	record ChatRequest(String sessionId, String message) {
	}

	record ChatResponse(String sessionId, String reply, String agentVersion) {
	}

	private final ChatClient chatClient;
	private final AgentConfig config;
	private final ChatMemory chatMemory;

	public ChatController(ChatClient.Builder builder, OrderTools orderTools, AgentConfig config,
			ChatMemory chatMemory, ChatMemoryRepository chatMemoryRepository) {
		this.config = config;
		this.chatMemory = chatMemory;

		// 1. Keep only the tools this version allows
		ToolCallback[] enabledTools = Arrays.stream(ToolCallbacks.from(orderTools))
				.filter(tool -> config.tools().contains(tool.getToolDefinition().name())).toArray(ToolCallback[]::new);

		// 2. Log what was loaded
		log.info("Loaded agent {} | model={} | temperature={} | tools={}", config.version(), config.model(),
				config.temperature(), Arrays.stream(enabledTools).map(t -> t.getToolDefinition().name()).toList());
		
        log.info("Chat memory store: {}", chatMemoryRepository.getClass().getSimpleName());

		// 3. Build the client: config + tools + memory
		this.chatClient = builder.defaultSystem(config.systemPrompt())
				.defaultOptions(ChatOptions.builder().model(config.model()).temperature(config.temperature()))
				.defaultToolCallbacks(enabledTools)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
	}

	@PostMapping("/chat")
	public ChatResponse chat(@RequestBody ChatRequest request) {
		// Use the client's session, or start a new one
		String sessionId = (request.sessionId() != null) ? request.sessionId() : UUID.randomUUID().toString();

		String reply = chatClient.prompt().user(request.message())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId)).call().content();

		return new ChatResponse(sessionId, reply, config.version());
	}

	// Debug window: what the agent "remembers" for a session
	@GetMapping("/sessions/{sessionId}")
	public List<Map<String, String>> session(@PathVariable String sessionId) {
		return chatMemory.get(sessionId).stream()
				.map((Message m) -> Map.of("type", m.getMessageType().toString(), "text", String.valueOf(m.getText())))
				.toList();
	}
}