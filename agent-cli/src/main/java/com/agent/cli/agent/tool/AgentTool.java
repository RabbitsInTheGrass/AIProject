package com.agent.cli.agent.tool;

import com.agent.cli.model.ChatMessage;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Agent 工具接口
 */
public interface AgentTool {

    /**
     * 工具名称
     */
    String getName();

    /**
     * 工具描述
     */
    String getDescription();

    /**
     * 参数定义 (JSON Schema)
     */
    Map<String, Object> getParameterSchema();

    /**
     * 执行工具
     * @param arguments 参数 JSON
     * @return 执行结果
     */
    String execute(JsonObject arguments);

    /**
     * 转换为 ToolDefinition 供模型使用
     */
    default ChatMessage.ToolDefinition toToolDefinition() {
        return new ChatMessage.ToolDefinition(getName(), getDescription(), getParameterSchema());
    }
}
