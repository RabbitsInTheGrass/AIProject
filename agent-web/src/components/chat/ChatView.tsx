import { useRef, useEffect } from 'react';
import { Typography, Select, Empty } from 'antd';
import { RobotOutlined } from '@ant-design/icons';
import { useChatStore } from '../../stores/chatStore';
import MessageBubble from './MessageBubble';
import ChatInput from './ChatInput';

export default function ChatView() {
  const { messages, isStreaming, modelConfigs, currentModelId, setCurrentModel, currentConversationId } = useChatStore();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const currentModel = modelConfigs.find(m => m.id === currentModelId);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', maxWidth: 900, margin: '0 auto', width: '100%' }}>
      {/* Header */}
      <div style={{
        padding: '12px 24px',
        background: '#fff',
        borderBottom: '1px solid #f0f0f0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <RobotOutlined style={{ fontSize: 20, color: '#1677ff' }} />
          <Typography.Text strong>Agent AI 助手</Typography.Text>
        </div>
        <Select
          value={currentModelId}
          onChange={setCurrentModel}
          style={{ width: 200 }}
          placeholder="选择模型"
          options={modelConfigs.map(m => ({
            value: m.id,
            label: `${m.name} (${m.modelName})`,
          }))}
        />
      </div>

      {/* Messages */}
      <div style={{ flex: 1, overflow: 'auto', padding: '24px 24px 0' }}>
        {messages.length === 0 ? (
          <div style={{ textAlign: 'center', paddingTop: 120 }}>
            <Empty
              image={<RobotOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
              imageStyle={{ height: 80 }}
              description={
                <Typography.Text type="secondary">
                  开始一段新对话吧！
                </Typography.Text>
              }
            />
          </div>
        ) : (
          messages.map((msg) => (
            <MessageBubble key={msg.id} message={msg} />
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <ChatInput />
    </div>
  );
}
