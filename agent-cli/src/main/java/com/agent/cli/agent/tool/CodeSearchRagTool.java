package com.agent.cli.agent.tool;

import com.agent.cli.rag.CodeIndexer;
import com.agent.cli.rag.CodeSearcher;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 代码语义搜索工具 - 基于 Lucene 索引的智能代码检索
 */
public class CodeSearchRagTool implements AgentTool {

    private final CodeSearcher searcher;

    public CodeSearchRagTool(CodeSearcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public String getName() { return "code_search"; }

    @Override
    public String getDescription() {
        return "Semantic code search using RAG (Retrieval Augmented Generation). " +
               "Search the indexed codebase for relevant code snippets by keywords or concepts. " +
               "Returns matching code blocks with file paths and line numbers. " +
               "Use this to find related code, understand implementations, or locate specific functionality.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "Search query - keywords, function names, class names, or concepts to find in code");
        properties.put("query", queryProp);

        Map<String, Object> maxResultsProp = new LinkedHashMap<>();
        maxResultsProp.put("type", "integer");
        maxResultsProp.put("description", "Maximum number of results to return (default: 5)");
        properties.put("max_results", maxResultsProp);

        schema.put("properties", properties);
        schema.put("required", new String[]{"query"});
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        String query = arguments.get("query").getAsString();
        int maxResults = arguments.has("max_results") ? arguments.get("max_results").getAsInt() : 5;

        try {
            List<CodeSearcher.SearchResult> results = searcher.search(query, maxResults);

            if (results.isEmpty()) {
                return "No relevant code found for query: " + query;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Found %d relevant code snippets for: \"%s\"%n%n", results.size(), query));

            for (int i = 0; i < results.size(); i++) {
                CodeSearcher.SearchResult result = results.get(i);
                sb.append(String.format("─── Result %d/%d ───%n", i + 1, results.size()));
                sb.append(result.toFormattedString());
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error searching code: " + e.getMessage();
        }
    }
}
