import { useEffect } from 'react';
import { Layout } from 'antd';
import Sidebar from './Sidebar';
import ChatView from '../chat/ChatView';
import { useChatStore } from '../../stores/chatStore';

const { Content } = Layout;

export default function AppLayout() {
  const { loadConversations, loadModelConfigs, loadSkills } = useChatStore();

  useEffect(() => {
    loadConversations();
    loadModelConfigs();
    loadSkills();
  }, []);

  return (
    <Layout style={{ height: '100vh' }}>
      <Sidebar />
      <Content style={{ background: '#f5f5f5', display: 'flex', flexDirection: 'column' }}>
        <ChatView />
      </Content>
    </Layout>
  );
}
