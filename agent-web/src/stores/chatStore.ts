import { create } from 'zustand';
import type { DisplayMessage, Conversation, ModelConfig, SkillConfig } from '../types';
import { conversationApi, modelApi, skillApi, streamChat } from '../api';

interface ChatStore {
  // 会话
  conversations: Conversation[];
  currentConversationId: string | null;
  messages: DisplayMessage[];
  isStreaming: boolean;
  abortController: AbortController | null;

  // 模型
  modelConfigs: ModelConfig[];
  currentModelId: number | null;

  // Skill
  skills: SkillConfig[];

  // Actions
  loadConversations: () => Promise<void>;
  loadMessages: (conversationId: string) => Promise<void>;
  loadModelConfigs: () => Promise<void>;
  loadSkills: () => Promise<void>;
  setCurrentConversation: (id: string | null) => void;
  setCurrentModel: (id: number | null) => void;
  sendMessage: (content: string) => Promise<void>;
  stopStreaming: () => void;
  newConversation: () => void;
  deleteConversation: (id: string) => Promise<void>;
}

export const useChatStore = create<ChatStore>((set, get) => ({
  conversations: [],
  currentConversationId: null,
  messages: [],
  isStreaming: false,
  abortController: null,
  modelConfigs: [],
  currentModelId: null,
  skills: [],

  loadConversations: async () => {
    try {
      const data = await conversationApi.list();
      set({ conversations: data });
    } catch (e) {
      console.error('Failed to load conversations', e);
    }
  },

  loadMessages: async (conversationId: string) => {
    try {
      const data = await conversationApi.messages(conversationId);
      const msgs: DisplayMessage[] = data
        .filter((m: any) => m.role === 'user' || m.role === 'assistant')
        .map((m: any) => ({
          id: String(m.id),
          role: m.role,
          content: m.content || '',
        }));
      set({ messages: msgs, currentConversationId: conversationId });
    } catch (e) {
      console.error('Failed to load messages', e);
    }
  },

  loadModelConfigs: async () => {
    try {
      const data = await modelApi.list();
      set({ modelConfigs: data });
      const defaultModel = data.find((m: ModelConfig) => m.isDefault);
      if (defaultModel && defaultModel.id) {
        set({ currentModelId: defaultModel.id });
      } else if (data.length > 0 && data[0].id) {
        set({ currentModelId: data[0].id });
      }
    } catch (e) {
      console.error('Failed to load model configs', e);
    }
  },

  loadSkills: async () => {
    try {
      const data = await skillApi.list();
      set({ skills: data });
    } catch (e) {
      console.error('Failed to load skills', e);
    }
  },

  setCurrentConversation: (id) => {
    set({ currentConversationId: id });
    if (id) get().loadMessages(id);
    else set({ messages: [] });
  },

  setCurrentModel: (id) => set({ currentModelId: id }),

  newConversation: () => {
    set({ currentConversationId: null, messages: [] });
  },

  deleteConversation: async (id) => {
    try {
      await conversationApi.delete(id);
      const { currentConversationId } = get();
      if (currentConversationId === id) {
        set({ currentConversationId: null, messages: [] });
      }
      await get().loadConversations();
    } catch (e) {
      console.error('Failed to delete conversation', e);
    }
  },

  sendMessage: async (content: string) => {
    const { currentConversationId, currentModelId, messages, skills } = get();

    // 添加用户消息
    const userMsg: DisplayMessage = {
      id: Date.now().toString(),
      role: 'user',
      content,
    };
    const assistantMsg: DisplayMessage = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: '',
      isStreaming: true,
    };

    set({
      messages: [...messages, userMsg, assistantMsg],
      isStreaming: true,
    });

    const abortController = new AbortController();
    set({ abortController });

    const enabledSkillIds = skills.filter(s => s.isEnabled).map(s => s.id);

    await streamChat(
      {
        conversationId: currentConversationId || undefined,
        content,
        modelConfigId: currentModelId || undefined,
        enabledSkillIds,
      },
      (event) => {
        const state = get();
        const msgs = [...state.messages];
        const lastMsg = msgs[msgs.length - 1];

        if (event.type === 'content' && event.content && lastMsg?.role === 'assistant') {
          lastMsg.content += event.content;
          set({ messages: msgs });
        } else if (event.type === 'done') {
          if (lastMsg?.role === 'assistant') {
            lastMsg.isStreaming = false;
          }
          if (event.conversationId) {
            set({
              messages: msgs,
              currentConversationId: event.conversationId,
              isStreaming: false,
              abortController: null,
            });
            get().loadConversations();
          } else {
            set({ messages: msgs, isStreaming: false, abortController: null });
          }
        } else if (event.type === 'error') {
          if (lastMsg?.role === 'assistant') {
            lastMsg.content += `\n\n❌ 错误: ${event.message || '未知错误'}`;
            lastMsg.isStreaming = false;
          }
          set({ messages: msgs, isStreaming: false, abortController: null });
        }
      },
      (error) => {
        const state = get();
        const msgs = [...state.messages];
        const lastMsg = msgs[msgs.length - 1];
        if (lastMsg?.role === 'assistant') {
          lastMsg.content += `\n\n❌ 连接错误: ${error.message}`;
          lastMsg.isStreaming = false;
        }
        set({ messages: msgs, isStreaming: false, abortController: null });
      },
      abortController.signal
    );
  },

  stopStreaming: () => {
    const { abortController } = get();
    if (abortController) {
      abortController.abort();
      set((state) => {
        const msgs = [...state.messages];
        const lastMsg = msgs[msgs.length - 1];
        if (lastMsg?.role === 'assistant') {
          lastMsg.isStreaming = false;
        }
        return { messages: msgs, isStreaming: false, abortController: null };
      });
    }
  },
}));
