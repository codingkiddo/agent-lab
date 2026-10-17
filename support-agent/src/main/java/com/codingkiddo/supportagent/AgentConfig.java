package com.codingkiddo.supportagent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "agent")
public record AgentConfig(
        String version,
        String model,
        Double temperature,
        String systemPrompt,
        List<String> tools) {
}