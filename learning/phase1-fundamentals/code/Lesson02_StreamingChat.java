import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ========================================
 * 第二课：SSE 流式传输（打字机效果）
 * ========================================
 * 
 * 为什么需要流式传输？
 * 
 * 假设你问 LLM 一个复杂问题，它需要 15 秒生成完整回答。
 * - 不用流式：用户盯着空白屏幕等 15 秒 → 体验极差
 * - 用流式：用户立即看到文字逐字出现 → 体验很好
 * 
 * SSE (Server-Sent Events) 是什么？
 * - 服务器单向持续推送数据的 HTTP 协议
 * - 每推送一条 data: 行，包含一小段增量文本
 * - 最后发送 data: [DONE] 表示结束
 * 
 * SSE 数据流示例：
 *   data: {"choices":[{"delta":{"content":"你"}}]}
 *   data: {"choices":[{"delta":{"content":"好"}}]}
 *   data: {"choices":[{"delta":{"content":"！"}}]}
 *   data: [DONE]
 * 
 * 运行前请设置环境变量 OPENAI_API_KEY
 */
public class Lesson02_StreamingChat {
    
    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String MODEL = "gpt-4o-mini";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    
    public static void main(String[] args) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("错误：请先设置环境变量 OPENAI_API_KEY");
            return;
        }
        
        System.out.println("=".repeat(60));
        System.out.println("  AI Agent 学习 - 第二课：SSE 流式传输");
        System.out.println("=".repeat(60));
        
        // ========== 示例1：基础流式输出 ==========
        System.out.println("\n【示例1】基础流式输出（打字机效果）\n");
        System.out.println("AI 正在思考...\n");
        
        streamChat(
            null,  // 无 system prompt
            "用Java写一个二分查找算法，并逐步解释每一步的作用"
        );
        
        // ========== 示例2：带角色的流式输出 ==========
        System.out.println("\n\n【示例2】带系统提示词的流式输出\n");
        System.out.println("（系统提示词：你是一位幽默的编程老师）\n");
        
        streamChat(
            "你是一位幽默风趣的编程老师，善于用生活中的比喻解释技术概念",
            "为什么 Java 程序员总是分不清万圣节和圣诞节？"
        );
        
        System.out.println("\n\n✅ 流式传输演示完成！");
    }
    
    /**
     * 流式对话 - SSE 实现
     * 
     * 与非流式的关键区别：
     * 1. 请求体中 "stream": true
     * 2. 响应不是完整 JSON，而是持续的 SSE 事件流
     * 3. 需要逐行读取，解析每个 delta（增量文本）
     * 
     * 在 Agent 开发中，流式传输还有更深的意义：
     * - Agent 调用工具可能需要几秒到几十秒
     * - 流式让用户能看到 "AI 正在调用 xxx 工具..." 等中间状态
     * - 这就是为什么你的 agent-server 用 SseEmitter
     */
    static void streamChat(String systemPrompt, String userMessage) throws Exception {
        // 构建消息列表
        StringBuilder messagesJson = new StringBuilder("[");
        if (systemPrompt != null) {
            messagesJson.append(String.format(
                "{\"role\": \"system\", \"content\": \"%s\"},",
                escapeJson(systemPrompt)
            ));
        }
        messagesJson.append(String.format(
            "{\"role\": \"user\", \"content\": \"%s\"}",
            escapeJson(userMessage)
        ));
        messagesJson.append("]");
        
        // 关键：stream 设为 true
        String requestBody = String.format("""
            {
                "model": "%s",
                "messages": %s,
                "temperature": 0.7,
                "max_tokens": 1000,
                "stream": true
            }
            """, MODEL, messagesJson.toString());
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        // 关键：用 InputStream 而不是 String，这样可以逐块读取
        HttpResponse<java.io.InputStream> response = client.send(request,
            HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes());
            throw new RuntimeException("API Error " + response.statusCode() + ": " + errorBody);
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(response.body(), "UTF-8"));
        
        StringBuilder fullContent = new StringBuilder();
        String line;
        int chunkCount = 0;
        long startTime = System.currentTimeMillis();
        
        while ((line = reader.readLine()) != null) {
            // SSE 格式：每条事件以 "data: " 开头
            if (!line.startsWith("data: ")) {
                continue;
            }
            
            String data = line.substring(6).trim();
            
            // [DONE] 表示流结束
            if ("[DONE]".equals(data)) {
                break;
            }
            
            // 解析增量内容
            String delta = extractDelta(data);
            if (delta != null && !delta.isEmpty()) {
                System.out.print(delta);  // 实时打印每个字符/词
                System.out.flush();       // 立即刷新，不缓冲
                fullContent.append(delta);
                chunkCount++;
            }
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("\n\n  [统计] 共 %d 个 chunk, %d 个字符, 耗时 %.1f 秒%n",
            chunkCount, fullContent.length(), elapsed / 1000.0);
    }
    
    /**
     * 从 SSE data 中提取增量文本
     * 
     * 流式响应的每个 chunk 格式：
     * {
     *   "id": "chatcmpl-xxx",
     *   "object": "chat.completion.chunk",
     *   "choices": [{
     *     "index": 0,
     *     "delta": { "content": "你" },
     *     "finish_reason": null
     *   }]
     * }
     * 
     * 注意：第一个 chunk 的 delta 通常是 {"role": "assistant"}，没有 content
     * 最后一个 chunk 的 delta 为空，finish_reason 为 "stop"
     */
    private static String extractDelta(String json) {
        // 查找 "content":"xxx" 模式
        String contentKey = "\"content\":\"";
        int start = json.indexOf(contentKey);
        if (start == -1) return null;
        
        start += contentKey.length();
        
        StringBuilder result = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n': result.append('\n'); i++; break;
                    case '"': result.append('"'); i++; break;
                    case '\\': result.append('\\'); i++; break;
                    case 't': result.append('\t'); i++; break;
                    default: result.append(c); break;
                }
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
