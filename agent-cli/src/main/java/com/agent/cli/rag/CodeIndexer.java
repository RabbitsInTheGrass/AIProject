package com.agent.cli.rag;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * RAG 代码索引器 - 基于 Lucene 的代码检索
 */
public class CodeIndexer {

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter writer;
    private final Path projectDir;

    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", "node_modules", "target", "build", ".idea", "__pycache__",
            ".vscode", "dist", "out", ".gradle", "venv", ".venv", "env"));

    private static final Set<String> INDEXABLE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".go", ".rs", ".c", ".cpp", ".h",
            ".cs", ".rb", ".php", ".swift", ".kt", ".scala", ".sh", ".bash", ".yml", ".yaml",
            ".json", ".xml", ".html", ".css", ".sql", ".toml", ".ini", ".cfg", ".gradle",
            ".properties", ".vue", ".svelte", ".dart", ".lua", ".r", ".m", ".mm", ".pl",
            ".ex", ".exs", ".hs", ".clj", ".cljs", ".erl", ".elm", ".fs", ".fsx"));

    private static final long MAX_FILE_SIZE = 512 * 1024; // 512KB

    public CodeIndexer(Path projectDir) {
        this.projectDir = projectDir;
        this.directory = new ByteBuffersDirectory();
        this.analyzer = new StandardAnalyzer();
    }

    /**
     * 索引整个项目
     * @return 索引的文件数量
     */
    public int indexProject() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writer = new IndexWriter(directory, config);

        AtomicInteger fileCount = new AtomicInteger(0);

        try (Stream<Path> stream = Files.walk(projectDir, 15)) {
            stream.filter(Files::isRegularFile)
                  .filter(this::shouldIndex)
                  .forEach(file -> {
                      try {
                          indexFile(file);
                          fileCount.incrementAndGet();
                      } catch (Exception e) {
                          // Skip files that can't be indexed
                      }
                  });
        }

        writer.commit();
        writer.close();
        return fileCount.get();
    }

    /**
     * 索引单个文件 - 按代码块分割
     */
    private void indexFile(Path file) throws IOException {
        String relativePath = projectDir.relativize(file).toString();
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String extension = getExtension(file.getFileName().toString());

        List<CodeChunk> chunks = splitIntoChunks(content, relativePath, extension);

        for (int i = 0; i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);
            Document doc = new Document();

            doc.add(new StringField("path", relativePath, Field.Store.YES));
            doc.add(new StringField("extension", extension, Field.Store.YES));
            doc.add(new StoredField("chunkIndex", i));
            doc.add(new StoredField("startLine", chunk.startLine));
            doc.add(new StoredField("endLine", chunk.endLine));

            doc.add(new TextField("content", chunk.content, Field.Store.YES));
            doc.add(new TextField("context", chunk.context, Field.Store.YES));

            String fileName = file.getFileName().toString();
            doc.add(new TextField("filename", fileName, Field.Store.YES));

            writer.addDocument(doc);
        }
    }

    /**
     * 将文件内容分割为代码块
     */
    private List<CodeChunk> splitIntoChunks(String content, String path, String extension) {
        List<CodeChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        int chunkSize = 50;
        int overlap = 5;

        // 小文件直接作为一个块
        if (lines.length <= chunkSize * 2) {
            chunks.add(new CodeChunk(content, path + " [full file]", 1, lines.length));
            return chunks;
        }

        // 滑动窗口分割
        for (int start = 0; start < lines.length; start += (chunkSize - overlap)) {
            int end = Math.min(start + chunkSize, lines.length);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(lines[i]).append("\n");
            }

            String chunkContent = sb.toString();
            String context = path + " lines " + (start + 1) + "-" + end;

            chunks.add(new CodeChunk(chunkContent, context, start + 1, end));

            if (end >= lines.length) break;
        }

        return chunks;
    }

    private boolean shouldIndex(Path path) {
        String name = path.getFileName().toString();
        if (name.startsWith(".")) return false;

        for (Path part : projectDir.relativize(path)) {
            if (SKIP_DIRS.contains(part.toString())) return false;
        }

        String ext = getExtension(name);
        if (!INDEXABLE_EXTENSIONS.contains(ext)) return false;

        try {
            return Files.size(path) <= MAX_FILE_SIZE;
        } catch (IOException e) {
            return false;
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    public void close() throws IOException {
        if (directory != null) {
            directory.close();
        }
    }

    public Directory getDirectory() {
        return directory;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * 代码块
     */
    public static class CodeChunk {
        public final String content;
        public final String context;
        public final int startLine;
        public final int endLine;

        public CodeChunk(String content, String context, int startLine, int endLine) {
            this.content = content;
            this.context = context;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}
