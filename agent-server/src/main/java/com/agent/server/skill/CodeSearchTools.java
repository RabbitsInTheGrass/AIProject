package com.agent.server.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class CodeSearchTools {

    @Value("${app.work-dir:./workspace}")
    private String workDir;

    @Tool(description = "Search for a text pattern (regex supported) across files in the workspace. Returns matching lines with file paths and line numbers.")
    public String searchCode(
            @ToolParam(description = "Search pattern (regex)") String pattern,
            @ToolParam(description = "File glob filter (e.g., '*.java', '*.py'). Optional.") String fileFilter,
            @ToolParam(description = "Case sensitive search (default: true)") Boolean caseSensitive) {
        try {
            Path dirPath = Paths.get(workDir);
            boolean caseSens = caseSensitive == null || caseSensitive;
            Pattern regex = caseSens ? Pattern.compile(pattern) : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

            List<String> results = new ArrayList<>();
            int maxResults = 100;

            try (Stream<Path> stream = Files.walk(dirPath, 10)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> fileFilter == null || p.getFileName().toString().matches(
                                fileFilter.replace("*", ".*").replace("?", ".")))
                        .filter(p -> !p.toString().contains("node_modules") &&
                                !p.toString().contains(".git") &&
                                !p.toString().contains("target") &&
                                !p.toString().contains("build"))
                        .forEach(p -> {
                            if (results.size() >= maxResults) return;
                            try {
                                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                                for (int i = 0; i < lines.size(); i++) {
                                    if (results.size() >= maxResults) break;
                                    if (regex.matcher(lines.get(i)).find()) {
                                        String rel = dirPath.relativize(p).toString();
                                        results.add(String.format("%s:%d: %s", rel, i + 1, lines.get(i).trim()));
                                    }
                                }
                            } catch (IOException ignored) {}
                        });
            }

            if (results.isEmpty()) {
                return "No matches found for pattern: " + pattern;
            }
            return String.join("\n", results) + "\n\n(" + results.size() + " matches found)";
        } catch (Exception e) {
            return "Error searching code: " + e.getMessage();
        }
    }

    @Tool(description = "Search for class, method, or variable definitions in the codebase.")
    public String searchDefinition(
            @ToolParam(description = "Name of class, method, or variable to search for") String name) {
        // Search for common definition patterns
        String[] patterns = {
                "class\\s+" + name,
                "interface\\s+" + name,
                "def\\s+" + name,
                "function\\s+" + name,
                "public\\s+.*\\s+" + name + "\\s*\\(",
                "private\\s+.*\\s+" + name + "\\s*\\(",
                "protected\\s+.*\\s+" + name + "\\s*\\(",
                name + "\\s*=\\s*",
                "var\\s+" + name,
                "const\\s+" + name,
                "let\\s+" + name
        };

        StringBuilder allResults = new StringBuilder();
        for (String p : patterns) {
            String result = searchCode(p, null, true);
            if (!result.startsWith("No matches")) {
                allResults.append(result).append("\n\n");
            }
        }

        if (allResults.isEmpty()) {
            return "No definition found for: " + name;
        }
        return allResults.toString();
    }
}
