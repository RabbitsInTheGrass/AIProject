package com.agent.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Hybrid Search Service - combines Milvus (vector) + Elasticsearch (BM25) with RRF fusion
 * Currently a stub that degrades gracefully when Milvus/ES are not available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    @Value("${milvus.enabled:false}")
    private boolean milvusEnabled;

    @Value("${app.search.es-enabled:false}")
    private boolean esEnabled;

    /**
     * Search knowledge base with hybrid retrieval
     * @return list of relevant text chunks
     */
    public List<SearchResult> search(Long knowledgeBaseId, String query, int topK) {
        if (!milvusEnabled && !esEnabled) {
            log.debug("Search skipped: Milvus and ES are both disabled");
            return Collections.emptyList();
        }

        // TODO: Implement when Milvus/ES are available
        // 1. Milvus vector search (embedding similarity)
        // 2. ES BM25 keyword search
        // 3. RRF (Reciprocal Rank Fusion) to merge results

        return Collections.emptyList();
    }

    public String buildRAGContext(Long knowledgeBaseId, String query) {
        List<SearchResult> results = search(knowledgeBaseId, query, 5);
        if (results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n=== Relevant Knowledge ===\n");
        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("[%d] %s (score: %.2f)\n", i + 1, results.get(i).content(), results.get(i).score()));
        }
        return sb.toString();
    }

    public record SearchResult(String content, double score, String source) {}
}
