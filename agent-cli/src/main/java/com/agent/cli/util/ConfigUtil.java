package com.agent.cli.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置工具 - 加载和保存配置
 */
public class ConfigUtil {

    private static final String CONFIG_DIR = ".agent-cli";
    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getConfigDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, CONFIG_DIR);
    }

    public static Path getConfigFile() {
        return getConfigDir().resolve(CONFIG_FILE);
    }

    public static JsonObject loadConfig() {
        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            return new JsonObject();
        }
        try {
            String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static void saveConfig(JsonObject config) throws IOException {
        Path configDir = getConfigDir();
        Files.createDirectories(configDir);
        byte[] bytes = GSON.toJson(config).getBytes(StandardCharsets.UTF_8);
        Files.write(getConfigFile(), bytes);
    }

    public static String getApiKey(String provider) {
        JsonObject config = loadConfig();
        if (config.has("providers")) {
            JsonObject providers = config.getAsJsonObject("providers");
            if (providers.has(provider)) {
                JsonObject providerConfig = providers.getAsJsonObject(provider);
                if (providerConfig.has("apiKey")) {
                    return providerConfig.get("apiKey").getAsString();
                }
            }
        }

        String envKey;
        switch (provider.toLowerCase()) {
            case "deepseek": envKey = "DEEPSEEK_API_KEY"; break;
            case "glm": envKey = "GLM_API_KEY"; break;
            case "moonshot": envKey = "MOONSHOT_API_KEY"; break;
            case "stepfun": envKey = "STEPFUN_API_KEY"; break;
            case "spark": envKey = "SPARK_API_KEY"; break;
            case "openai": envKey = "OPENAI_API_KEY"; break;
            default: envKey = provider.toUpperCase() + "_API_KEY";
        }

        return System.getenv(envKey);
    }

    public static void saveApiKey(String provider, String apiKey) throws IOException {
        JsonObject config = loadConfig();
        if (!config.has("providers")) {
            config.add("providers", new JsonObject());
        }
        JsonObject providers = config.getAsJsonObject("providers");
        if (!providers.has(provider)) {
            providers.add(provider, new JsonObject());
        }
        providers.getAsJsonObject(provider).addProperty("apiKey", apiKey);
        saveConfig(config);
    }
}
