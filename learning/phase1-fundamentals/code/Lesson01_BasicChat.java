import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ========================================
 * 第一课：用最原始的方式调用 OpenAI API
 * ========================================
 * 
 * 不依赖任何框架（不用 Spring AI、不用 LangChain4j），
 * 用纯 Java HTTP 调用，帮你理解底层到底发生了什么。
 * 
 * 学完这一课，你将理解：
 * 1. LLM API 的请求结构是什么样的
 * 2. Message 角色体系（system/user/assistant）的作用
 * 3. 多轮对话为什么需要发送完整历史
 * 
 * 运行前请设置环境变量：
 *   Windows: set OPENAI_API_KEY=sk-xxx
 *   Linux:   export OPENAI_API_KEY=sk-xxx
 * 
 * 如果使用国内模型（DeepSeek/Qwen），修改 BASE_URL 即可。
 */
public class Lesson01_BasicChat {
    
    // ========================================
    // 配置区 - 根据你使用的模型修改
    // ========================================
    
    // OpenAI 官方
    private static final String BASE_URL = "https://api.openai.com/v1";
    
    // 如果用 DeepSeek，改为：
    // private static final String BASE_URL = "https://api.deepseek.com/v1";
    
    // 如果用通义千问(Qwen)，改为：
    // private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    
    // 如果用 Ollama 本地部署，改为：
    // private static final String BASE_URL = "http://localhost:11434/v1";
    
    private static final String MODEL = "gpt-4o-mini";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    
    public static void main(String[] args) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("错误：请先设置环境变量 OPENAI_API_KEY");
            System.err.println("Windows: set OPENAI_API_KEY=sk-xxx");
            System.err.println("Linux:   export OPENAI_API_KEY=sk-xxx");
            return;
        }
        
        System.out.println("=".repeat(60));
        System.out.println("  AI Agent 学习 - 第一课：基础对话");
        System.out.println("=".repeat(60));
        
        // ========== 示例1：最简单的单轮对话 ==========
        System.out.println("\n【示例1】最简单的单轮对话\n");
        String response1 = chat("你好，请用一句话介绍自己");
        System.out.println("AI回复: " + response1);
        
        // ========== 示例2：带系统提示词的对话 ==========
        System.out.println("\n【示例2】带系统提示词的对话\n");
        System.out.println("（System Prompt 决定了 AI 的"人格"和行为）\n");
        String response2 = chatWithSystem(
            "你是一位资深Java架构师，回答要简洁专业，用技术术语",
            "Spring Boot 的核心优势是什么？"
        );
        System.out.println("架构师回答: " + response2);
        
        // ========== 示例3：多轮对话 ==========
        System.out.println("\n【示例3】多轮对话（模拟上下文）\n");
        System.out.println("（注意：LLM 本身不记忆对话！每次都要发完整历史）\n");
        
        String[][] messages = {
            {"system", "你是一个编程老师，用通俗的比喻解释概念"},
            {"user", "什么是递归？"},
            {"assistant", "递归就像俄罗斯套娃——一个函数调用自己来解决问题，"
                + "每次处理更小的部分，直到最简单的情况（最小的套娃）。"},
            {"user", "能给个Java例子吗？"}
        };
        String response3 = chatMultiTurn(messages);
        System.out.println("老师举例: " + response3);
    }
    
    /**
     * 示例1：最简单的单轮对话
     * 
     * 这是最基础的调用方式：
     * - 只发一条 user 消息
     * - 没有 system prompt（AI 用默认行为）
     * - 没有上下文（每次都是全新的对话）
     */
    static String chat(String userMessage) throws Exception {
        String requestBody = """
            {
                "model": "%s",
                "messages": [
                    {"role": "user", "content": "%s"}
                ],
                "temperature": 0.7,
                "max_tokens": 500
            }
            """.formatted(MODEL, escapeJson(userMessage));
        
        return sendRequest(requestBody);
    }
    
    /**
     * 示例2：带系统提示词的对话
     * 
     * System Prompt 是 Agent 的灵魂——它决定了 AI 的"人格"和行为边界。
     * 
     * 为什么 system 消息如此重要？
     * - 它影响模型后续所有生成的概率分布
     * - 比如设为"Java专家"后，模型更倾向于输出 Java 相关的词汇
     * - 在 Agent 开发中，system prompt 还会包含工具列表、行为规则等
     */
    static String chatWithSystem(String systemPrompt, String userMessage) throws Exception {
        String requestBody = """
            {
                "model": "%s",
                "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                ],
                "temperature": 0.7,
                "max_tokens": 500
            }
            """.formatted(MODEL, escapeJson(systemPrompt), escapeJson(userMessage));
        
        return sendRequest(requestBody);
    }
    
    /**
     * 示例3：多轮对话
     * 
     * 核心概念：LLM 本身是无状态的！
     * 
     * 很多人误以为 API 会"记住"之前的对话。事实上：
     * - 每次 API 调用都是独立的
     * - 你必须把完整的对话历史都发过去
     * - 如果你漏掉了某条历史消息，AI 就像失忆了一样
     * 
     * 这也是为什么第二阶段要学"上下文窗口管理"——
     * 对话轮数多了，历史消息会超出上下文窗口限制。
     */
    static String chatMultiTurn(String[][] messages) throws Exception {
        StringBuilder messagesJson = new StringBuilder("[");
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) messagesJson.append(",");
            messagesJson.append(String.format(
                "{\"role\": \"%s\", \"content\": \"%s\"}",
                messages[i][0], escapeJson(messages[i][1])
            ));
        }
        messagesJson.append("]");
        
        String requestBody = String.format("""
            {
                "model": "%s",
                "messages": %s,
                "temperature": 0.7,
                "max_tokens": 800
            }
            """, MODEL, messagesJson.toString());
        
        return sendRequest(requestBody);
    }
    
    /**
     * 发送 HTTP 请求并解析响应
     * 
     * 这是所有 LLM 框架（Spring AI、LangChain4j）底层都在做的事情。
     * 手动实现一遍，你就知道框架帮你封装了什么。
     */
    private static String sendRequest(String requestBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error " + response.statusCode() 
                + ": " + response.body());
        }
        
        String body = response.body();
        
        // ---- 解析 content ----
        // 实际响应的 JSON 结构：
        // {
        //   "choices": [{
        //     "message": { "role": "assistant", "content": "..." },
        //     "finish_reason": "stop"
        //   }],
        //   "usage": { "prompt_tokens": 25, "completion_tokens": 150 }
        // }
        
        String content = extractJsonValue(body, "content");
        
        // ---- 提取 Token 用量（了解成本） ----
        int promptTokens = extractIntValue(body, "prompt_tokens");
        int completionTokens = extractIntValue(body, "completion_tokens");
        int totalTokens = promptTokens + completionTokens;
        
        System.out.printf("  [Token消耗] 输入:%d + 输出:%d = 总计:%d%n",
            promptTokens, completionTokens, totalTokens);
        
        return content;
    }
    
    // ========================================
    // 简易 JSON 解析（生产环境请用 Jackson/Gson）
    // ========================================
    
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        start += searchKey.length();
        
        StringBuilder result = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == 'n') { result.append('\n'); i++; }
                else if (next == '"') { result.append('"'); i++; }
                else if (next == '\\') { result.append('\\'); i++; }
                else { result.append(c); }
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    private static int extractIntValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return 0;
        start += searchKey.length();
        
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Integer.parseInt(json.substring(start, end));
    }
    
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
