import type { AuthResponse, LoginRequest, RegisterRequest, HttpToolConfig, PluginTool } from '../types';

const BASE_URL = '/api';

function getAuthHeaders(): Record<string, string> {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function fetchJSON<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE_URL + url, {
    headers: getAuthHeaders(),
    ...options,
  });
  if (res.status === 401) {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);
  return res.json();
}

// ===== Auth API =====
export const authApi = {
  login: (data: LoginRequest): Promise<AuthResponse> =>
    fetch(BASE_URL + '/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(r => {
      if (!r.ok) throw new Error('登录失败');
      return r.json();
    }),

  register: (data: RegisterRequest): Promise<AuthResponse> =>
    fetch(BASE_URL + '/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(r => {
      if (!r.ok) throw new Error('注册失败');
      return r.json();
    }),

  me: (): Promise<any> => fetchJSON('/auth/me'),

  refresh: (refreshToken: string): Promise<AuthResponse> =>
    fetch(BASE_URL + '/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    }).then(r => r.json()),
};

// ===== 模型配置 API =====
export const modelApi = {
  list: () => fetchJSON<any[]>('/models'),
  presets: () => fetchJSON<any[]>('/models/presets'),
  create: (data: any) => fetchJSON<any>('/models', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: any) => fetchJSON<any>(`/models/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => fetchJSON<void>(`/models/${id}`, { method: 'DELETE' }),
};

// ===== 会话 API =====
export const conversationApi = {
  list: () => fetchJSON<any[]>('/conversations'),
  get: (id: string) => fetchJSON<any>(`/conversations/${id}`),
  messages: (id: string) => fetchJSON<any[]>(`/conversations/${id}/messages`),
  updateTitle: (id: string, title: string) =>
    fetchJSON<any>(`/conversations/${id}`, { method: 'PUT', body: JSON.stringify({ title }) }),
  delete: (id: string) => fetchJSON<void>(`/conversations/${id}`, { method: 'DELETE' }),
};

// ===== Skill API =====
export const skillApi = {
  list: () => fetchJSON<any[]>('/skills'),
  toggle: (id: number) => fetchJSON<any>(`/skills/${id}/toggle`, { method: 'PUT' }),
};

// ===== HTTP Tool API =====
export const httpToolApi = {
  list: () => fetchJSON<HttpToolConfig[]>('/http-tools'),
  get: (id: number) => fetchJSON<HttpToolConfig>(`/http-tools/${id}`),
  create: (data: HttpToolConfig) => fetchJSON<HttpToolConfig>('/http-tools', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: HttpToolConfig) => fetchJSON<HttpToolConfig>(`/http-tools/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  toggle: (id: number) => fetchJSON<HttpToolConfig>(`/http-tools/${id}/toggle`, { method: 'PUT' }),
  delete: (id: number) => fetchJSON<void>(`/http-tools/${id}`, { method: 'DELETE' }),
};

// ===== Plugin Tool API =====
export const pluginApi = {
  list: () => fetchJSON<PluginTool[]>('/plugins'),
  get: (id: number) => fetchJSON<PluginTool>(`/plugins/${id}`),
  create: (config: PluginTool, jarFile: File) => {
    const formData = new FormData();
    formData.append('config', new Blob([JSON.stringify(config)], { type: 'application/json' }));
    formData.append('jar', jarFile);
    const token = localStorage.getItem('accessToken');
    return fetch(BASE_URL + '/plugins', {
      method: 'POST',
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: formData,
    }).then(r => r.json());
  },
  update: (id: number, data: PluginTool) => fetchJSON<PluginTool>(`/plugins/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  toggle: (id: number) => fetchJSON<PluginTool>(`/plugins/${id}/toggle`, { method: 'PUT' }),
  delete: (id: number) => fetchJSON<void>(`/plugins/${id}`, { method: 'DELETE' }),
};

// ===== 知识库 API =====
export const knowledgeApi = {
  list: () => fetchJSON<any[]>('/knowledge-bases'),
  create: (data: { name: string; description?: string }) =>
    fetchJSON<any>('/knowledge-bases', { method: 'POST', body: JSON.stringify(data) }),
  delete: (id: number) => fetchJSON<void>(`/knowledge-bases/${id}`, { method: 'DELETE' }),
  documents: (id: number) => fetchJSON<any[]>(`/knowledge-bases/${id}/documents`),
  uploadDocument: (id: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const token = localStorage.getItem('accessToken');
    return fetch(BASE_URL + `/knowledge-bases/${id}/documents`, {
      method: 'POST',
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: formData,
    }).then(r => r.json());
  },
  deleteDocument: (kbId: number, docId: number) =>
    fetchJSON<void>(`/knowledge-bases/${kbId}/documents/${docId}`, { method: 'DELETE' }),
};

// ===== 对话流式 API =====
export function streamChat(
  request: {
    conversationId?: string;
    content: string;
    modelConfigId?: number;
    enabledSkillIds?: number[];
    knowledgeBaseId?: number;
  },
  onEvent: (event: any) => void,
  onError?: (error: Error) => void,
  signal?: AbortSignal
) {
  const token = localStorage.getItem('accessToken');
  return fetch(BASE_URL + '/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(request),
    signal,
  }).then(async (response) => {
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim();
          if (data === '[DONE]') continue;
          try {
            const event = JSON.parse(data);
            onEvent(event);
          } catch {}
        }
      }
    }
  }).catch((err) => {
    if (err.name !== 'AbortError' && onError) {
      onError(err);
    }
  });
}
