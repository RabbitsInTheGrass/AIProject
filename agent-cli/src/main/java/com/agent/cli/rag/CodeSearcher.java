package com.agent.cli.rag;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 代码搜索器 - 基于 Lucene 的语义代码检索
 */
public class CodeSearcher {

    private final CodeIndexer indexer;

    public CodeSearcher(CodeIndexer indexer) {
        this.indexer = indexer;
    }

    /**
     * 搜索代码
     */
    public List<SearchResult> search(String query, int maxResults) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        IndexReader reader = DirectoryReader.open(indexer.getDirectory());
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = indexer.getAnalyzer();

        String escapedQuery = QueryParser.escape(query);

        // 多字段搜索 - 为每个字段创建独立的 QueryParser
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        try {
            // content 字段搜索
            QueryParser contentParser = new QueryParser("content", analyzer);
            contentParser.setDefaultOperator(QueryParser.Operator.OR);
            Query contentQuery = contentParser.parse(escapedQuery);
            queryBuilder.add(new BoostQuery(contentQuery, 1.0f), BooleanClause.Occur.SHOULD);

            // filename 字段搜索 (更高权重)
            QueryParser filenameParser = new QueryParser("filename", analyzer);
            filenameParser.setDefaultOperator(QueryParser.Operator.OR);
            Query filenameQuery = filenameParser.parse(escapedQuery);
            queryBuilder.add(new BoostQuery(filenameQuery, 3.0f), BooleanClause.Occur.SHOULD);

            // context 字段搜索
            QueryParser contextParser = new QueryParser("context", analyzer);
            contextParser.setDefaultOperator(QueryParser.Operator.OR);
            Query contextQuery = contextParser.parse(escapedQuery);
            queryBuilder.add(new BoostQuery(contextQuery, 1.5f), BooleanClause.Occur.SHOULD);
        } catch (Exception e) {
            // Fallback: 直接对 content 搜索
            QueryParser fallbackParser = new QueryParser("content", analyzer);
            Query fallbackQuery = fallbackParser.parse(escapedQuery);
            queryBuilder.add(fallbackQuery, BooleanClause.Occur.MUST);
        }

        Query finalQuery = queryBuilder.build();
        TopDocs topDocs = searcher.search(finalQuery, maxResults);

        // 高亮
        QueryScorer scorer = new QueryScorer(finalQuery);
        Highlighter highlighter = new Highlighter(
                new SimpleHTMLFormatter(">>", "<<"),
                scorer);
        highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 200));

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String path = doc.get("path");
            String content = doc.get("content");
            String context = doc.get("context");
            int startLine = doc.getField("startLine").numericValue().intValue();
            int endLine = doc.getField("endLine").numericValue().intValue();

            // 生成高亮片段
            String highlight = content;
            try {
                String fragment = highlighter.getBestFragment(analyzer, "content", content);
                if (fragment != null) {
                    highlight = fragment;
                }
            } catch (Exception e) {
                // Use original content
            }

            String preview = content.length() > 500 ? content.substring(0, 500) + "..." : content;

            results.add(new SearchResult(
                    path, startLine, endLine, scoreDoc.score, highlight, preview, context
            ));
        }

        reader.close();
        return results;
    }

    /**
     * 搜索结果
     */
    public static class SearchResult {
        private final String filePath;
        private final int startLine;
        private final int endLine;
        private final float score;
        private final String highlight;
        private final String preview;
        private final String context;

        public SearchResult(String filePath, int startLine, int endLine, float score,
                           String highlight, String preview, String context) {
            this.filePath = filePath;
            this.startLine = startLine;
            this.endLine = endLine;
            this.score = score;
            this.highlight = highlight;
            this.preview = preview;
            this.context = context;
        }

        public String getFilePath() { return filePath; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public float getScore() { return score; }
        public String getHighlight() { return highlight; }
        public String getPreview() { return preview; }
        public String getContext() { return context; }

        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  \uD83D\uDCC4 %s (lines %d-%d) [score: %.2f]%n", filePath, startLine, endLine, score));
            sb.append("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n");

            String[] lines = preview.split("\n");
            int maxLines = Math.min(lines.length, 8);
            for (int i = 0; i < maxLines; i++) {
                String line = lines[i].length() > 80 ? lines[i].substring(0, 77) + "..." : lines[i];
                sb.append(String.format("  %4d \u2502 %s%n", startLine + i, line));
            }
            if (lines.length > maxLines) {
                sb.append("       \u2502 ...\n");
            }
            sb.append("\n");
            return sb.toString();
        }
    }
}
