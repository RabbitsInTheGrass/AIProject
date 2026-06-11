-- Agent Platform MySQL Schema
-- 使用前先创建数据库: CREATE DATABASE agent_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/EMAIL/WECHAT/QQ/GITHUB/GOOGLE',
    oauth_id VARCHAR(200) COMMENT '第三方OAuth ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_oauth (auth_provider, oauth_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL COMMENT 'ADMIN/USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS conversation (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) DEFAULT '新对话',
    model_config_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_updated (user_id, updated_at DESC),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_calls TEXT,
    tool_call_id VARCHAR(100),
    token_usage VARCHAR(500),
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_sort (conversation_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT 'NULL表示系统级共享配置',
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    temperature DOUBLE DEFAULT 0.7,
    max_tokens INT DEFAULT 4096,
    is_default TINYINT DEFAULT 0,
    extra_headers VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skill_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT 'NULL表示系统内置工具',
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) DEFAULT 'builtin',
    is_enabled TINYINT DEFAULT 1,
    config_json TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    collection_name VARCHAR(200) NOT NULL,
    embedding_model VARCHAR(100) DEFAULT 'text-embedding-3-small',
    document_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'READY',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_message VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS http_tool_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '工具唯一标识名',
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    request_url VARCHAR(500) NOT NULL,
    request_method VARCHAR(10) NOT NULL DEFAULT 'GET',
    request_headers TEXT COMMENT 'JSON格式请求头模板',
    request_body_template TEXT COMMENT '请求体模板,支持{{param}}占位符',
    request_params TEXT COMMENT 'JSON格式查询参数模板',
    response_extract_path VARCHAR(200) COMMENT 'JSONPath提取规则',
    parameter_schema TEXT COMMENT '工具参数JSON Schema',
    is_enabled TINYINT NOT NULL DEFAULT 1,
    timeout_ms INT NOT NULL DEFAULT 30000,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_enabled (user_id, is_enabled),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plugin_tool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '工具唯一标识名',
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    jar_file_path VARCHAR(500) NOT NULL COMMENT 'jar文件存储路径',
    main_class VARCHAR(300) NOT NULL COMMENT '实现AgentTool接口的全限定类名',
    parameter_schema TEXT COMMENT '工具参数JSON Schema',
    is_enabled TINYINT NOT NULL DEFAULT 1,
    class_loader_id VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_enabled (user_id, is_enabled),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_long_term_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    memory_type VARCHAR(30) NOT NULL COMMENT 'PREFERENCE/FACT/SUMMARY',
    content TEXT NOT NULL,
    source_conversation_id VARCHAR(36),
    relevance_score DECIMAL(3,2) DEFAULT 0.80,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_active (user_id, is_active),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS verification_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target VARCHAR(100) NOT NULL COMMENT '手机号或邮箱',
    target_type VARCHAR(10) NOT NULL COMMENT 'PHONE/EMAIL',
    code VARCHAR(10) NOT NULL,
    purpose VARCHAR(20) NOT NULL COMMENT 'REGISTER/LOGIN/RESET',
    used TINYINT NOT NULL DEFAULT 0,
    expire_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target_purpose (target, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
