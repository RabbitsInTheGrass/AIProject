package com.agent.cli;

import com.agent.cli.agent.AgentCore;
import com.agent.cli.model.ProviderConfig;
import com.agent.cli.provider.LLMProvider;
import com.agent.cli.provider.ProviderFactory;
import com.agent.cli.ui.TerminalUI;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Agent CLI - AI 编程助手命令行工具
 *
 * 支持多种大模型提供商:
 *   DeepSeek, GLM (智谱), Moonshot (Kimi), StepFun (阶跃星辰), Spark (讯飞星辰), OpenAI
 */
@Command(name = "agent-cli",
         mixinStandardHelpOptions = true,
         version = "Agent CLI v1.0.0",
         description = "AI Agent Command Line Tool - Multi-Model / RAG / Tool-Use")
public class AgentCLI implements Callable<Integer> {

    @Option(names = {"-p", "--provider"},
            description = "LLM provider (deepseek, glm, moonshot, stepfun, spark, openai)")
    private String provider = "deepseek";

    @Option(names = {"-k", "--api-key"},
            description = "API key for the LLM provider")
    private String apiKey;

    @Option(names = {"-m", "--model"},
            description = "Model name (overrides default)")
    private String model;

    @Option(names = {"-w", "--work-dir"},
            description = "Working directory (default: current directory)")
    private String workDir;

    @Option(names = {"--base-url"},
            description = "Custom API base URL")
    private String baseUrl;

    @Option(names = {"--rag"},
            description = "Enable RAG code search indexing on startup")
    private boolean enableRag = false;

    @Option(names = {"--list-providers"},
            description = "List supported providers")
    private boolean listProviders = false;

    @Option(names = {"-t", "--temperature"},
            description = "Temperature for generation (default: 0.7)")
    private double temperature = 0.7;

    @Option(names = {"--max-tokens"},
            description = "Max tokens for response (default: 4096)")
    private int maxTokens = 4096;

    @Override
    public Integer call() {
        if (listProviders) {
            System.out.println(ProviderFactory.getProviderListDescription());
            return 0;
        }

        String resolvedApiKey = resolveApiKey();
        if (resolvedApiKey == null || resolvedApiKey.isEmpty()) {
            System.err.println("Error: API key is required. Use -k/--api-key or set environment variable.");
            System.err.println("  Environment variables: DEEPSEEK_API_KEY, GLM_API_KEY, MOONSHOT_API_KEY,");
            System.err.println("                         STEPFUN_API_KEY, SPARK_API_KEY, OPENAI_API_KEY");
            return 1;
        }

        Path resolvedWorkDir = workDir != null ? Paths.get(workDir) : Paths.get(System.getProperty("user.dir"));

        TerminalUI ui = new TerminalUI();

        try {
            LLMProvider llmProvider = createProvider(resolvedApiKey);

            ui.showBanner(llmProvider.getName(), llmProvider.getModelName(),
                    resolvedWorkDir.toAbsolutePath().toString());

            AgentCore agent = new AgentCore(llmProvider, ui, resolvedWorkDir);

            if (enableRag) {
                agent.initializeRag();
            }

            runRepl(agent, ui);

        } catch (Exception e) {
            ui.showError("Fatal error: " + e.getMessage());
            return 1;
        } finally {
            ui.cleanup();
        }

        return 0;
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }

        String envVar;
        switch (provider.toLowerCase()) {
            case "deepseek": envVar = "DEEPSEEK_API_KEY"; break;
            case "glm": envVar = "GLM_API_KEY"; break;
            case "moonshot": envVar = "MOONSHOT_API_KEY"; break;
            case "stepfun": envVar = "STEPFUN_API_KEY"; break;
            case "spark": envVar = "SPARK_API_KEY"; break;
            case "openai": envVar = "OPENAI_API_KEY"; break;
            default: envVar = "API_KEY";
        }
        return System.getenv(envVar);
    }

    private LLMProvider createProvider(String resolvedApiKey) {
        if (baseUrl != null) {
            ProviderConfig config = new ProviderConfig(
                    provider, resolvedApiKey, baseUrl,
                    model != null ? model : "default"
            );
            config.setTemperature(temperature);
            config.setMaxTokens(maxTokens);
            return ProviderFactory.create(config);
        }

        LLMProvider llmProvider = ProviderFactory.create(provider, resolvedApiKey, model);

        ProviderConfig preset = ProviderFactory.getPresets().get(provider.toLowerCase());
        if (preset != null) {
            ProviderConfig config = new ProviderConfig(
                    preset.getName(), resolvedApiKey, preset.getBaseUrl(),
                    llmProvider.getModelName()
            );
            config.setTemperature(temperature);
            config.setMaxTokens(maxTokens);
            return ProviderFactory.create(config);
        }

        return llmProvider;
    }

    private void runRepl(AgentCore agent, TerminalUI ui) {
        Terminal terminal;
        LineReader reader;

        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .encoding(Charset.forName("UTF-8"))
                    .build();

            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
        } catch (IOException e) {
            ui.showError("Failed to initialize terminal: " + e.getMessage());
            return;
        }

        while (true) {
            String input;
            try {
                input = reader.readLine(ui.getPrompt());
            } catch (UserInterruptException e) {
                continue;
            } catch (EndOfFileException e) {
                ui.showGoodbye();
                break;
            }

            if (input == null || input.trim().isEmpty()) {
                continue;
            }

            String trimmed = input.trim();

            if (trimmed.equals("/exit") || trimmed.equals("/quit")) {
                ui.showGoodbye();
                break;
            }

            try {
                agent.processInput(trimmed);
            } catch (Exception e) {
                ui.showError("Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AgentCLI()).execute(args);
        System.exit(exitCode);
    }
}
