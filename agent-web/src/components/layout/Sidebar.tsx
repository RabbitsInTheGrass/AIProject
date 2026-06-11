import { useState } from 'react';
import { Layout, Button, List, Typography, Modal, Space, Dropdown, Avatar } from 'antd';
import {
  PlusOutlined, MessageOutlined, DeleteOutlined,
  SettingOutlined, DatabaseOutlined, ToolOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, UserOutlined,
  LogoutOutlined
} from '@ant-design/icons';
import { useChatStore } from '../../stores/chatStore';
import { useAuthStore } from '../../stores/authStore';
import ModelConfigPanel from '../model/ModelConfigPanel';
import SkillPanel from '../skill/SkillPanel';
import KnowledgePanel from '../knowledge/KnowledgePanel';

const { Sider } = Layout;

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const [settingModal, setSettingModal] = useState<string | null>(null);
  const { conversations, currentConversationId, setCurrentConversation, newConversation, deleteConversation } = useChatStore();
  const { user, logout } = useAuthStore();

  return (
    <>
      <Sider
        width={280}
        collapsible
        collapsed={collapsed}
        collapsedWidth={60}
        onCollapse={setCollapsed}
        trigger={null}
        style={{ background: '#fff', borderRight: '1px solid #f0f0f0' }}
      >
        <div style={{ padding: collapsed ? '12px 8px' : '12px 16px', display: 'flex', flexDirection: 'column', height: '100%' }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            {!collapsed && <Typography.Title level={5} style={{ margin: 0 }}>Agent AI</Typography.Title>}
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              size="small"
            />
          </div>

          {/* New Chat Button */}
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={newConversation}
            block={!collapsed}
            size="middle"
            style={{ marginBottom: 16 }}
          >
            {collapsed ? '' : '新对话'}
          </Button>

          {/* Conversation List */}
          <div style={{ flex: 1, overflow: 'auto' }}>
            <List
              size="small"
              dataSource={conversations}
              renderItem={(conv) => (
                <List.Item
                  onClick={() => setCurrentConversation(conv.id)}
                  style={{
                    cursor: 'pointer',
                    background: currentConversationId === conv.id ? '#e6f4ff' : 'transparent',
                    borderRadius: 8,
                    padding: collapsed ? '8px 4px' : '8px 12px',
                    marginBottom: 4,
                    display: 'flex',
                    justifyContent: 'space-between',
                  }}
                  actions={collapsed ? [] : [
                    <Button
                      key="del"
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={(e) => { e.stopPropagation(); deleteConversation(conv.id); }}
                    />
                  ]}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, overflow: 'hidden', flex: 1 }}>
                    <MessageOutlined style={{ color: '#6366f1' }} />
                    {!collapsed && (
                      <Typography.Text ellipsis style={{ fontSize: 13 }}>
                        {conv.title}
                      </Typography.Text>
                    )}
                  </div>
                </List.Item>
              )}
            />
          </div>

          {/* Bottom Actions */}
          {!collapsed && (
            <Space direction="vertical" style={{ width: '100%', marginTop: 12 }} size={4}>
              <Button icon={<SettingOutlined />} block type="text" onClick={() => setSettingModal('model')}>
                模型配置
              </Button>
              <Button icon={<ToolOutlined />} block type="text" onClick={() => setSettingModal('skill')}>
                技能管理
              </Button>
              <Button icon={<DatabaseOutlined />} block type="text" onClick={() => setSettingModal('knowledge')}>
                知识库
              </Button>
            </Space>
          )}
          {collapsed && (
            <Space direction="vertical" size={4} style={{ marginTop: 12 }}>
              <Button icon={<SettingOutlined />} type="text" size="small" onClick={() => setSettingModal('model')} />
              <Button icon={<ToolOutlined />} type="text" size="small" onClick={() => setSettingModal('skill')} />
              <Button icon={<DatabaseOutlined />} type="text" size="small" onClick={() => setSettingModal('knowledge')} />
            </Space>
          )}

          {/* User Menu */}
          <div style={{ borderTop: '1px solid #f0f0f0', marginTop: 12, paddingTop: 8 }}>
            <Dropdown menu={{
              items: [
                { key: 'user', label: user?.nickname || user?.username || '用户', icon: <UserOutlined />, disabled: true },
                { type: 'divider' },
                { key: 'logout', label: '退出登录', icon: <LogoutOutlined />, danger: true, onClick: logout },
              ],
            }}>
              <Button type="text" block style={{ textAlign: 'left' }}>
                <Avatar size="small" icon={<UserOutlined />} style={{ marginRight: 8 }} />
                {!collapsed && (user?.nickname || user?.username || '用户')}
              </Button>
            </Dropdown>
          </div>
        </div>
      </Sider>

      {/* Settings Modals */}
      <Modal
        title="模型配置"
        open={settingModal === 'model'}
        onCancel={() => setSettingModal(null)}
        footer={null}
        width={720}
      >
        <ModelConfigPanel />
      </Modal>
      <Modal
        title="技能管理"
        open={settingModal === 'skill'}
        onCancel={() => setSettingModal(null)}
        footer={null}
        width={700}
      >
        <SkillPanel />
      </Modal>
      <Modal
        title="知识库"
        open={settingModal === 'knowledge'}
        onCancel={() => setSettingModal(null)}
        footer={null}
        width={700}
      >
        <KnowledgePanel />
      </Modal>
    </>
  );
}
  );
}
