import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

/**
 * ========================================
 * 第三课：Embedding 向量嵌入 + 语义搜索
 * ========================================
 * 
 * 这是 RAG（检索增强生成）的基石！
 * 
 * 学完这一课，你将理解：
 * 1. Embedding 把文本变成什么？（高维向量）
 * 2. 为什么"猫"和"小猫"的向量很近，但和"汽车"很远？
 * 3. 如何用余弦相似度实现"语义搜索"
 * 4. 这跟 Agent 的 RAG 有什么关系？
 * 
 * RAG 的核心流程（后续会深入学）：
 *   文档 → 分块 → Embedding向量化 → 存入向量数据库
 *   用户提问 → Embedding向量化 → 搜索最相似的文档块 → 注入Prompt
 * 
 * 运行前请设置环境变量 OPENAI_API_KEY
 */
public class Lesson03_Embedding {
    
    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    
    public static void main(String[] args) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("错误：请先设置环境变量 OPENAI_API_KEY");
            return;
        }
        
        System.out.println("=".repeat(60));
        System.out.println("  AI Agent 学习 - 第三课：Embedding + 语义搜索");
        System.out.println("=".repeat(60));
        
        // ========== 示例1：生成 Embedding 向量 ==========
        System.out.println("\n【示例1】生成 Embedding 向量\n");
        
        String text1 = "Java是一种面向对象的编程语言";
        String text2 = "Python是一种解释型编程语言";
        String text3 = "今天中午吃什么好呢";
        
        double[] emb1 = getEmbedding(text1);
        double[] emb2 = getEmbedding(text2);
        double[] emb3 = getEmbedding(text3);
        
        System.out.println("向量维度: " + emb1.length);
        System.out.println("向量的前5个值: [" 
            + String.format("%.4f, %.4f, %.4f, %.4f, %.4f",
                emb1[0], emb1[1], emb1[2], emb1[3], emb1[4]) + "]");
        
        // ========== 示例2：计算余弦相似度 ==========
        System.out.println("\n【示例2】余弦相似度计算\n");
        
        double sim12 = cosineSimilarity(emb1, emb2);
        double sim13 = cosineSimilarity(emb1, emb3);
        double sim23 = cosineSimilarity(emb2, emb3);
        
        System.out.printf("  '%s'%n  vs '%s'%n  → 相似度: %.4f (语义相近！)%n%n",
            text1, text2, sim12);
        System.out.printf("  '%s'%n  vs '%s'%n  → 相似度: %.4f (语义无关)%n%n",
            text1, text3, sim13);
        System.out.printf("  '%s'%n  vs '%s'%n  → 相似度: %.4f%n",
            text2, text3, sim23);
        
        // ========== 示例3：语义搜索 ==========
        System.out.println("\n\n【示例3】语义搜索演示\n");
        System.out.println("这是 RAG 的核心原理！\n");
        
        String query = "哪种编程语言适合初学者学习";
        String[] documents = {
            "Java的面向对象特性使其适合大型企业级应用开发",
            "今天天气很好，适合出去散步锻炼身体",
            "Python语法简洁直观，是入门编程的最佳选择之一",
            "股票市场今日大跌，投资者情绪恐慌",
            "学习编程最重要的是多动手实践，不要只看教程",
            "量子计算是一种全新的计算范式"
        };
        
        System.out.println("查询: \"" + query + "\"\n");
        System.out.println("搜索结果（按语义相似度排序）：");
        System.out.println("-".repeat(60));
        
        // 计算每个文档与查询的相似度
        double[] queryEmb = getEmbedding(query);
        double[][] results = new double[documents.length][2];
        
        for (int i = 0; i < documents.length; i++) {
            double[] docEmb = getEmbedding(documents[i]);
            results[i][0] = i;
            results[i][1] = cosineSimilarity(queryEmb, docEmb);
        }
        
        // 按相似度降序排列
        Arrays.sort(results, (a, b) -> Double.compare(b[1], a[1]));
        
        for (double[] r : results) {
            int idx = (int) r[0];
            String marker = r[1] > 0.5 ? " ✅ 相关" : (r[1] > 0.3 ? " ⚠️ 弱相关" : " ❌ 无关");
            System.out.printf("  [%.4f]%s  %s%n", r[1], marker, documents[idx]);
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  这就是 RAG 的核心！");
        System.out.println("  在 Agent 中，搜索到的相关文档会被注入到 Prompt 中，");
        System.out.println("  让 LLM 基于你的知识库来回答问题。");
        System.out.println("=".repeat(60));
    }
    
    /**
     * 调用 Embedding API 获取文本的向量表示
     * 
     * 与 Chat API 的区别：
     * - Chat API 返回文本（对话）
     * - Embedding API 返回浮点数数组（向量）
     * 
     * 常用 Embedding 模型：
     * - text-embedding-3-small：便宜，1536维，适合一般场景
     * - text-embedding-3-large：更精确，3072维，适合精度要求高的场景
     * - bge-large-zh：中文专用，开源可本地部署
     */
    static double[] getEmbedding(String text) throws Exception {
        String requestBody = String.format("""
            {
                "model": "%s",
                "input": "%s"
            }
            """, EMBEDDING_MODEL, escapeJson(text));
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/embeddings"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API Error: " + response.body());
        }
        
        return parseEmbeddingArray(response.body());
    }
    
    /**
     * 余弦相似度
     * 
     * 衡量两个向量的"方向一致性"：
     *   1.0 = 完全相同方向（语义相同）
     *   0.0 = 垂直（语义无关）
     *  -1.0 = 完全相反方向（语义相反）
     * 
     * 公式：cos(θ) = (A·B) / (|A| × |B|)
     * 
     * 在 RAG 中，我们用余弦相似度来：
     * 1. 找到与用户问题最相关的文档块
     * 2. 过滤掉不相关的内容
     * 3. 按相关性排序，取 Top-K 结果
     */
    static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];  // 对应位置相乘再求和
            normA += a[i] * a[i];       // A 的模长平方
            normB += b[i] * b[i];       // B 的模长平方
        }
        
        // 除以模长的乘积 = 归一化（只看方向，不看长度）
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0) return 0.0;
        
        return dotProduct / denominator;
    }
    
    /**
     * 解析 Embedding API 返回的向量数组
     * 
     * 响应格式：
     * {
     *   "data": [{
     *     "embedding": [0.0023064255, -0.009327292, ...],
     *     "index": 0
     *   }],
     *   "model": "text-embedding-3-small",
     *   "usage": { "prompt_tokens": 8, "total_tokens": 8 }
     * }
     */
    static double[] parseEmbeddingArray(String json) {
        int start = json.indexOf("\"embedding\": [") + 14;
        int end = json.indexOf("]", start);
        
        String[] parts = json.substring(start, end).split(",");
        double[] result = new double[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        
        return result;
    }
    
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
