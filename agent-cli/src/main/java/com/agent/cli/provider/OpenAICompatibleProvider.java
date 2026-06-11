package com.agent.cli.provider;

import com.agent.cli.model.ChatMessage;
import com.agent.cli.model.ChatResponse;
import com.agent.cli.model.ProviderConfig;
import com.google.gson.*;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议的 LLM Provider
 * 支持: DeepSeek, GLM, Moonshot(Kimi), StepFun(阶跃星辰), Spark(讯飞星辰), OpenAI 等
 */
public class OpenAICompatibleProvider implements LLMProvider {

    private final ProviderConfig config;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public OpenAICompatibleProvider(ProviderConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, List<ChatMessage.ToolDefinition> tools) {
        JsonObject requestBody = buildRequestBody(messages, tools, false);

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/chat/completions")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new RuntimeException("API error (" + response.code() + "): " + errorBody);
            }
            String body = response.body().string();
            return parseResponse(body);
        } catch (IOException e) {
            throw new RuntimeException("Request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(List<ChatMessage> messages, List<ChatMessage.ToolDefinition> tools,
                           Consumer<String> onContent, Consumer<ChatResponse> onComplete,
                           Consumer<Throwable> onError) {
        JsonObject requestBody = buildRequestBody(messages, tools, true);

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/chat/completions")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .build();

        // 在后台线程中处理 SSE 流
        Thread streamThread = new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown";
                    onError.accept(new RuntimeException("Stream error (" + response.code() + "): " + errorBody));
                    return;
                }

                StringBuilder fullContent = new StringBuilder();
                Map<Integer, ChatMessage.ToolCall> toolCallMap = new ConcurrentHashMap<>();

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    onError.accept(new RuntimeException("Empty response body"));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;

                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;

                        try {
                            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                            JsonArray choices = json.getAsJsonArray("choices");
                            if (choices == null || choices.size() == 0) continue;

                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject delta = choice.getAsJsonObject("delta");
                            if (delta == null) continue;

                            // 处理文本内容
                            if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                String content = delta.get("content").getAsString();
                                fullContent.append(content);
                                onContent.accept(content);
                            }

                            // 处理 tool_calls
                            if (delta.has("tool_calls") && !delta.get("tool_calls").isJsonNull()) {
                                JsonArray tcArray = delta.getAsJsonArray("tool_calls");
                                for (JsonElement tcElem : tcArray) {
                                    JsonObject tc = tcElem.getAsJsonObject();
                                    int index = tc.has("index") ? tc.get("index").getAsInt() : 0;

                                    if (tc.has("id") && !tc.get("id").isJsonNull()) {
                                        ChatMessage.ToolCall toolCall = new ChatMessage.ToolCall();
                                        toolCall.setId(tc.get("id").getAsString());
                                        toolCall.setType("function");
                                        ChatMessage.ToolCall.FunctionCall fc = new ChatMessage.ToolCall.FunctionCall();
                                        if (tc.has("function")) {
                                            JsonObject fn = tc.getAsJsonObject("function");
                                            fc.setName(fn.has("name") ? fn.get("name").getAsString() : "");
                                            fc.setArguments(fn.has("arguments") ? fn.get("arguments").getAsString() : "");
                                        }
                                        toolCall.setFunction(fc);
                                        toolCallMap.put(index, toolCall);
                                    } else if (tc.has("function")) {
                                        ChatMessage.ToolCall existing = toolCallMap.get(index);
                                        if (existing != null) {
                                            JsonObject fn = tc.getAsJsonObject("function");
                                            if (fn.has("arguments") && !fn.get("arguments").isJsonNull()) {
                                                String args = existing.getFunction().getArguments();
                                                args = (args == null ? "" : args) + fn.get("arguments").getAsString();
                                                existing.getFunction().setArguments(args);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore parse errors for individual chunks
                        }
                    }
                }

                // 完成
                ChatResponse resp = new ChatResponse();
                resp.setContent(fullContent.toString());
                if (!toolCallMap.isEmpty()) {
                    List<ChatMessage.ToolCall> sorted = new ArrayList<>(toolCallMap.values());
                    resp.setToolCalls(sorted);
                }
                onComplete.accept(resp);

            } catch (IOException e) {
                onError.accept(e);
            }
        });
        streamThread.setDaemon(true);
        streamThread.start();
    }

    private JsonObject buildRequestBody(List<ChatMessage> messages, List<ChatMessage.ToolDefinition> tools, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.addProperty("temperature", config.getTemperature());
        body.addProperty("max_tokens", config.getMaxTokens());
        body.addProperty("stream", stream);

        JsonArray msgArray = new JsonArray();
        for (ChatMessage msg : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", msg.getRole().getValue());

            if (msg.getContent() != null) {
                msgObj.addProperty("content", msg.getContent());
            }

            if (msg.getToolCallId() != null) {
                msgObj.addProperty("tool_call_id", msg.getToolCallId());
            }

            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                JsonArray tcArray = new JsonArray();
                for (ChatMessage.ToolCall tc : msg.getToolCalls()) {
                    JsonObject tcObj = new JsonObject();
                    tcObj.addProperty("id", tc.getId());
                    tcObj.addProperty("type", tc.getType());
                    JsonObject fnObj = new JsonObject();
                    fnObj.addProperty("name", tc.getFunction().getName());
                    fnObj.addProperty("arguments", tc.getFunction().getArguments());
                    tcObj.add("function", fnObj);
                    tcArray.add(tcObj);
                }
                msgObj.add("tool_calls", tcArray);
            }

            msgArray.add(msgObj);
        }
        body.add("messages", msgArray);

        if (tools != null && !tools.isEmpty()) {
            JsonArray toolsArray = new JsonArray();
            for (ChatMessage.ToolDefinition tool : tools) {
                JsonObject toolObj = new JsonObject();
                toolObj.addProperty("type", "function");
                JsonObject fnObj = new JsonObject();
                fnObj.addProperty("name", tool.getFunction().getName());
                fnObj.addProperty("description", tool.getFunction().getDescription());
                fnObj.add("parameters", gson.toJsonTree(tool.getFunction().getParameters()));
                toolObj.add("function", fnObj);
                toolsArray.add(toolObj);
            }
            body.add("tools", toolsArray);
        }

        return body;
    }

    private ChatResponse parseResponse(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        ChatResponse response = new ChatResponse();

        if (json.has("id")) {
            response.setId(json.get("id").getAsString());
        }

        JsonArray choices = json.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");

            if (message.has("content") && !message.get("content").isJsonNull()) {
                response.setContent(message.get("content").getAsString());
            }

            if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
                response.setFinishReason(choice.get("finish_reason").getAsString());
            }

            if (message.has("tool_calls") && !message.get("tool_calls").isJsonNull()) {
                List<ChatMessage.ToolCall> toolCalls = new ArrayList<>();
                JsonArray tcArray = message.getAsJsonArray("tool_calls");
                for (JsonElement tcElem : tcArray) {
                    JsonObject tc = tcElem.getAsJsonObject();
                    ChatMessage.ToolCall toolCall = new ChatMessage.ToolCall();
                    toolCall.setId(tc.get("id").getAsString());
                    toolCall.setType(tc.has("type") ? tc.get("type").getAsString() : "function");
                    JsonObject fn = tc.getAsJsonObject("function");
                    toolCall.setFunction(new ChatMessage.ToolCall.FunctionCall(
                            fn.get("name").getAsString(),
                            fn.get("arguments").getAsString()
                    ));
                    toolCalls.add(toolCall);
                }
                response.setToolCalls(toolCalls);
            }
        }

        if (json.has("usage")) {
            JsonObject usageObj = json.getAsJsonObject("usage");
            ChatResponse.Usage usage = new ChatResponse.Usage();
            usage.setPromptTokens(usageObj.get("prompt_tokens").getAsInt());
            usage.setCompletionTokens(usageObj.get("completion_tokens").getAsInt());
            usage.setTotalTokens(usageObj.get("total_tokens").getAsInt());
            response.setUsage(usage);
        }

        return response;
    }
}
