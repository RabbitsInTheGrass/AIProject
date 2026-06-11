package com.agent.cli.agent.tool;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 代码搜索工具 (基于正则表达式)
 */
public class SearchCodeTool implements AgentTool {

    private final Path workDir;
    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", "node_modules", "target", "build", ".idea", "__pycache__", ".vscode", "dist", "out"));
    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".go", ".rs", ".c", ".cpp", ".h",
            ".cs", ".rb", ".php", ".swift", ".kt", ".scala", ".sh", ".bash", ".yml", ".yaml",
            ".json", ".xml", ".html", ".css", ".sql", ".md", ".txt", ".toml", ".ini", ".cfg",
            ".gradle", ".properties", ".vue", ".svelte"));
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB

    public SearchCodeTool(Path workDir) {
        this.workDir = workDir;
    }

    @Override
    public String getName() { return "search_code"; }

    @Override
    public String getDescription() {
        return "Search for a pattern (regex) in code files within the project. " +
               "Returns matching lines with file path and line number. " +
               "Supports regex patterns and optional file type filter.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> patternProp = new LinkedHashMap<>();
        patternProp.put("type", "string");
        patternProp.put("description", "Regex pattern to search for");
        properties.put("pattern", patternProp);

        Map<String, Object> pathProp = new LinkedHashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Directory to search in (relative to working directory, default: '.')");
        properties.put("path", pathProp);

        Map<String, Object> fileExtProp = new LinkedHashMap<>();
        fileExtProp.put("type", "string");
        fileExtProp.put("description", "File extension filter (e.g., '.java', '.py'). Optional.");
        properties.put("file_extension", fileExtProp);

        Map<String, Object> caseInsensitiveProp = new LinkedHashMap<>();
        caseInsensitiveProp.put("type", "boolean");
        caseInsensitiveProp.put("description", "Case insensitive search (default: true)");
        properties.put("case_insensitive", caseInsensitiveProp);

        schema.put("properties", properties);
        schema.put("required", new String[]{"pattern"});
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        String patternStr = arguments.get("pattern").getAsString();
        String searchPath = arguments.has("path") && !arguments.get("path").isJsonNull()
                ? arguments.get("path").getAsString() : ".";
        String fileExt = arguments.has("file_extension") && !arguments.get("file_extension").isJsonNull()
                ? arguments.get("file_extension").getAsString() : null;
        boolean caseInsensitive = !arguments.has("case_insensitive") || arguments.get("case_insensitive").getAsBoolean();

        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern pattern;
        try {
            pattern = Pattern.compile(patternStr, flags);
        } catch (Exception e) {
            return "Error: Invalid regex pattern: " + e.getMessage();
        }

        Path searchDir = workDir.resolve(searchPath);
        if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
            return "Error: Directory not found: " + searchPath;
        }

        StringBuilder results = new StringBuilder();
        int[] matchCount = {0};
        int maxMatches = 100;

        try (Stream<Path> stream = Files.walk(searchDir, 10)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> {
                      String name = p.getFileName().toString();
                      if (name.startsWith(".")) return false;
                      for (Path part : searchDir.relativize(p)) {
                          if (SKIP_DIRS.contains(part.toString())) return false;
                      }
                      if (fileExt != null && !name.endsWith(fileExt)) return false;
                      String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                      if (!CODE_EXTENSIONS.contains(ext)) return false;
                      try { return Files.size(p) < MAX_FILE_SIZE; } catch (IOException e) { return false; }
                  })
                  .forEach(file -> {
                      if (matchCount[0] >= maxMatches) return;
                      try {
                          List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                          for (int i = 0; i < lines.size() && matchCount[0] < maxMatches; i++) {
                              if (pattern.matcher(lines.get(i)).find()) {
                                  String relPath = searchDir.relativize(file).toString();
                                  results.append(String.format("%s:%d: %s%n", relPath, i + 1, lines.get(i).trim()));
                                  matchCount[0]++;
                              }
                          }
                      } catch (IOException e) {
                          // Skip unreadable files
                      }
                  });
        } catch (IOException e) {
            return "Error searching: " + e.getMessage();
        }

        if (matchCount[0] == 0) {
            return "No matches found for pattern: " + patternStr;
        }

        String header = String.format("Found %d matches%s:%n%n",
                matchCount[0], matchCount[0] >= maxMatches ? " (truncated at " + maxMatches + ")" : "");
        return header + results;
    }
}
