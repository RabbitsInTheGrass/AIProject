import { create } from 'zustand';
import type { AuthResponse, UserInfo } from '../types';
import { authApi } from '../api';

interface AuthStore {
  user: UserInfo | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  login: (username: string, password: string) => Promise<void>;
  register: (data: { username: string; password: string; email?: string; nickname?: string }) => Promise<void>;
  logout: () => void;
  checkAuth: () => Promise<boolean>;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,
  isAuthenticated: !!localStorage.getItem('accessToken'),
  isLoading: false,

  login: async (username, password) => {
    set({ isLoading: true });
    try {
      const response: AuthResponse = await authApi.login({ username, password });
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      set({ user: response.user, isAuthenticated: true, isLoading: false });
    } catch (e) {
      set({ isLoading: false });
      throw e;
    }
  },

  register: async (data) => {
    set({ isLoading: true });
    try {
      const response: AuthResponse = await authApi.register(data);
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      set({ user: response.user, isAuthenticated: true, isLoading: false });
    } catch (e) {
      set({ isLoading: false });
      throw e;
    }
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({ user: null, isAuthenticated: false });
  },

  checkAuth: async () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      set({ isAuthenticated: false, user: null });
      return false;
    }
    try {
      const user = await authApi.me();
      set({ user, isAuthenticated: true });
      return true;
    } catch {
      set({ isAuthenticated: false, user: null });
      return false;
    }
  },
}));
