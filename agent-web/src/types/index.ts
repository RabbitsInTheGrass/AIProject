// 类型定义
export interface ModelConfig {
  id?: number;
  name: string;
  provider: string;
  baseUrl: string;
  apiKey: string;
  modelName: string;
  temperature?: number;
  maxTokens?: number;
  isDefault?: boolean;
  extraHeaders?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ModelPreset {
  name: string;
  provider: string;
  baseUrl: string;
  model: string;
}

export interface Conversation {
  id: string;
  title: string;
  modelConfigId?: number;
  userId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id?: number;
  conversationId: string;
  role: 'system' | 'user' | 'assistant' | 'tool';
  content: string;
  toolCalls?: string;
  toolCallId?: string;
  tokenUsage?: string;
  sortOrder: number;
  createdAt?: string;
}

export interface ChatStreamEvent {
  type: 'content' | 'tool_call' | 'tool_result' | 'done' | 'error';
  content?: string;
  toolName?: string;
  arguments?: string;
  result?: string;
  conversationId?: string;
  usage?: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  message?: string;
}

export interface SkillConfig {
  id: number;
  name: string;
  displayName: string;
  description: string;
  category: string;
  isEnabled: boolean;
  configJson?: string;
}

export interface KnowledgeBase {
  id: number;
  name: string;
  description?: string;
  collectionName: string;
  embeddingModel: string;
  documentCount: number;
  chunkCount: number;
  status: string;
}

export interface KnowledgeDocument {
  id: number;
  knowledgeBaseId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  chunkCount: number;
  status: string;
  errorMessage?: string;
}

// Auth types
export interface UserInfo {
  id: number;
  username: string;
  email?: string;
  phone?: string;
  nickname?: string;
  avatarUrl?: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email?: string;
  phone?: string;
  nickname?: string;
}

// HTTP Tool types
export interface HttpToolConfig {
  id?: number;
  name: string;
  displayName: string;
  description?: string;
  requestUrl: string;
  requestMethod: string;
  requestHeaders?: string;
  requestBodyTemplate?: string;
  requestParams?: string;
  responseExtractPath?: string;
  parameterSchema?: string;
  isEnabled?: boolean;
  timeoutMs?: number;
}

export interface PluginTool {
  id?: number;
  name: string;
  displayName: string;
  description?: string;
  mainClass: string;
  isEnabled?: boolean;
  configJson?: string;
}

// 前端展示用的消息类型（包含流式状态）
export interface DisplayMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  isStreaming?: boolean;
  toolCalls?: Array<{
    name: string;
    arguments: string;
    result?: string;
  }>;
}
