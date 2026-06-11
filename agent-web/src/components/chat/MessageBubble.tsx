import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { Typography, Avatar, Button, message } from 'antd';
import { UserOutlined, RobotOutlined, CopyOutlined, CheckOutlined } from '@ant-design/icons';
import { useState } from 'react';
import type { DisplayMessage } from '../../types';

interface Props {
  message: DisplayMessage;
}

export default function MessageBubble({ message: msg }: Props) {
  const isUser = msg.role === 'user';

  return (
    <div style={{
      display: 'flex',
      gap: 12,
      marginBottom: 24,
      flexDirection: isUser ? 'row-reverse' : 'row',
      alignItems: 'flex-start',
    }}>
      <Avatar
        size={36}
        icon={isUser ? <UserOutlined /> : <RobotOutlined />}
        style={{ background: isUser ? '#1677ff' : '#722ed1', flexShrink: 0 }}
      />
      <div style={{
        maxWidth: '75%',
        background: isUser ? '#1677ff' : '#fff',
        color: isUser ? '#fff' : '#333',
        borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
        padding: '12px 16px',
        boxShadow: '0 1px 2px rgba(0,0,0,0.06)',
        wordBreak: 'break-word',
      }}>
        {isUser ? (
          <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{msg.content}</div>
        ) : (
          <div className="markdown-body">
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              rehypePlugins={[rehypeHighlight]}
              components={{
                pre: ({ children, ...props }) => (
                  <div style={{ position: 'relative' }}>
                    <pre {...props} style={{
                      background: '#282c34',
                      borderRadius: 8,
                      padding: '16px',
                      overflow: 'auto',
                      fontSize: 13,
                    }}>{children}</pre>
                  </div>
                ),
                code: ({ children, className, ...props }: any) => {
                  const isInline = !className;
                  if (isInline) {
                    return (
                      <code style={{
                        background: '#f0f0f0',
                        padding: '2px 6px',
                        borderRadius: 4,
                        fontSize: '0.9em',
                        color: '#d63384',
                      }} {...props}>{children}</code>
                    );
                  }
                  return <code className={className} {...props}>{children}</code>;
                },
              }}
            >
              {msg.content || (msg.isStreaming ? '...' : '')}
            </ReactMarkdown>
            {msg.isStreaming && (
              <span style={{
                display: 'inline-block',
                width: 8,
                height: 16,
                background: '#1677ff',
                animation: 'blink 1s infinite',
                marginLeft: 2,
                verticalAlign: 'text-bottom',
              }} />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
