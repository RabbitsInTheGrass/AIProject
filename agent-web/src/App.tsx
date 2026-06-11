import React from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useAuthStore } from './stores/authStore';
import LoginPage from './components/auth/LoginPage';
import AppLayout from './components/layout/AppLayout';
import './index.css';

const App: React.FC = () => {
  const { isAuthenticated } = useAuthStore();

  return (
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#6366f1' } }}>
      {isAuthenticated ? <AppLayout /> : <LoginPage />}
    </ConfigProvider>
  );
};

export default App;
