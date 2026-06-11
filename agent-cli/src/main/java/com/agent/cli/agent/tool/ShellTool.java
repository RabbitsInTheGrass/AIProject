package com.agent.cli.agent.tool;

import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shell 命令执行工具
 */
public class ShellTool implements AgentTool {

    private final Path workDir;
    private static final int DEFAULT_TIMEOUT = 30; // seconds
    private static final int MAX_OUTPUT = 10000; // characters

    public ShellTool(Path workDir) {
        this.workDir = workDir;
    }

    @Override
    public String getName() { return "shell_exec"; }

    @Override
    public String getDescription() {
        return "Execute a shell command in the working directory. " +
               "Returns stdout and stderr. Use with caution for destructive commands. " +
               "Default timeout is 30 seconds.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> cmdProp = new LinkedHashMap<>();
        cmdProp.put("type", "string");
        cmdProp.put("description", "Shell command to execute");
        properties.put("command", cmdProp);

        Map<String, Object> timeoutProp = new LinkedHashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Timeout in seconds (default: 30)");
        properties.put("timeout", timeoutProp);

        schema.put("properties", properties);
        schema.put("required", new String[]{"command"});
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        String command = arguments.get("command").getAsString();
        int timeout = arguments.has("timeout") ? arguments.get("timeout").getAsInt() : DEFAULT_TIMEOUT;

        // Safety check
        String lower = command.toLowerCase().trim();
        if (lower.startsWith("rm -rf /") || lower.equals("rm -rf /") ||
            lower.contains("format c:") || lower.contains("mkfs")) {
            return "Error: Dangerous command blocked for safety.";
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = new Thread(() -> readOutput(process.getInputStream(), stdout));
            Thread errThread = new Thread(() -> readOutput(process.getErrorStream(), stderr));
            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            outThread.join(5000);
            errThread.join(5000);

            StringBuilder result = new StringBuilder();

            if (!finished) {
                process.destroyForcibly();
                result.append("⚠ Command timed out after ").append(timeout).append(" seconds\n\n");
            }

            int exitCode = finished ? process.exitValue() : -1;
            result.append("Exit code: ").append(exitCode).append("\n\n");

            if (stdout.length() > 0) {
                String out = stdout.length() > MAX_OUTPUT ?
                        stdout.substring(0, MAX_OUTPUT) + "\n... (truncated)" : stdout.toString();
                result.append("STDOUT:\n").append(out).append("\n");
            }

            if (stderr.length() > 0) {
                String err = stderr.length() > MAX_OUTPUT ?
                        stderr.substring(0, MAX_OUTPUT) + "\n... (truncated)" : stderr.toString();
                result.append("STDERR:\n").append(err);
            }

            return result.toString();
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    private void readOutput(java.io.InputStream is, StringBuilder sb) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() < MAX_OUTPUT * 2) {
                    sb.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
