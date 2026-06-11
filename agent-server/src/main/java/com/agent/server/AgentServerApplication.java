package com.agent.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AgentServerApplication {
    public static void main(String[] args) {
        // 开发阶段排除 Milvus 自动配置，启用 Milvus 时移除此行
        System.setProperty("spring.autoconfigure.exclude",
                "org.springframework.ai.autoconfigure.vectorstore.milvus.MilvusVectorStoreAutoConfiguration");
        SpringApplication.run(AgentServerApplication.class, args);
    }
}
