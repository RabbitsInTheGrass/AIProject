import { useState, useRef, useEffect } from 'react';
import { Input, Button, Space } from 'antd';
import { SendOutlined, StopOutlined } from '@ant-design/icons';
import { useChatStore } from '../../stores/chatStore';

const { TextArea } = Input;

export default function ChatInput() {
  const [input, setInput] = useState('');
  const { sendMessage, isStreaming, stopStreaming } = useChatStore();
  const textAreaRef = useRef<any>(null);

  useEffect(() => {
    if (!isStreaming) {
      textAreaRef.current?.focus();
    }
  }, [isStreaming]);

  const handleSend = () => {
    const content = input.trim();
    if (!content || isStreaming) return;
    setInput('');
    sendMessage(content);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div style={{
      padding: '16px 24px 24px',
      background: '#fff',
      borderTop: '1px solid #f0f0f0',
    }}>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
        <TextArea
          ref={textAreaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
          autoSize={{ minRows: 1, maxRows: 6 }}
          disabled={isStreaming}
          style={{
            borderRadius: 12,
            resize: 'none',
            fontSize: 14,
          }}
        />
        {isStreaming ? (
          <Button
            icon={<StopOutlined />}
            onClick={stopStreaming}
            danger
            style={{ borderRadius: 12, height: 40 }}
          >
            停止
          </Button>
        ) : (
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={!input.trim()}
            style={{ borderRadius: 12, height: 40 }}
          >
            发送
          </Button>
        )}
      </div>
    </div>
  );
}
