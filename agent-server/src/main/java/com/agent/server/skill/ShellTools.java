package com.agent.server.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class ShellTools {

    @Value("${app.work-dir:./workspace}")
    private String workDir;

    @Tool(description = "Execute a shell command and return the output. Use with caution - commands run in the workspace directory.")
    public String executeShell(
            @ToolParam(description = "Shell command to execute") String command,
            @ToolParam(description = "Timeout in seconds (default: 30)") Integer timeoutSeconds) {
        try {
            int timeout = timeoutSeconds != null ? timeoutSeconds : 30;

            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }

            pb.directory(new java.io.File(workDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (lineCount >= 500) {
                        output.append("\n... (output truncated at 500 lines)");
                        break;
                    }
                    output.append(line).append("\n");
                    lineCount++;
                }
            }

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Command timed out after " + timeout + " seconds.\n" + output;
            }

            int exitCode = process.exitValue();
            String result = output.toString();
            if (exitCode != 0) {
                result = "Exit code: " + exitCode + "\n" + result;
            }
            return result.isEmpty() ? "(no output)" : result;

        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    @Tool(description = "Get the current working directory path and basic system info.")
    public String getSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Working Directory: ").append(new java.io.File(workDir).getAbsolutePath()).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append("\n");
        sb.append("User: ").append(System.getProperty("user.name")).append("\n");

        Runtime rt = Runtime.getRuntime();
        sb.append("Available Processors: ").append(rt.availableProcessors()).append("\n");
        sb.append("Free Memory: ").append(rt.freeMemory() / (1024 * 1024)).append(" MB\n");
        sb.append("Max Memory: ").append(rt.maxMemory() / (1024 * 1024)).append(" MB\n");

        return sb.toString();
    }
}
