import React, { useState } from 'react';
import { Form, Input, Button, Tabs, message, Card } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../stores/authStore';

const LoginPage: React.FC = () => {
  const { login, register, isLoading } = useAuthStore();
  const [activeTab, setActiveTab] = useState('login');

  const handleLogin = async (values: { username: string; password: string }) => {
    try {
      await login(values.username, values.password);
      message.success('登录成功');
    } catch (e: any) {
      message.error(e.message || '登录失败');
    }
  };

  const handleRegister = async (values: any) => {
    try {
      await register({
        username: values.username,
        password: values.password,
        email: values.email,
        nickname: values.nickname,
      });
      message.success('注册成功，已自动登录');
    } catch (e: any) {
      message.error(e.message || '注册失败');
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    }}>
      <Card style={{ width: 420, borderRadius: 12, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <h1 style={{ fontSize: 28, margin: 0, color: '#1a1a2e' }}>Agent AI</h1>
          <p style={{ color: '#666', marginTop: 4 }}>智能对话平台</p>
        </div>

        <Tabs activeKey={activeTab} onChange={setActiveTab} centered items={[
          {
            key: 'login',
            label: '登录',
            children: (
              <Form onFinish={handleLogin} layout="vertical" size="large">
                <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                  <Input prefix={<UserOutlined />} placeholder="用户名" />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={isLoading} block>
                    登录
                  </Button>
                </Form.Item>
              </Form>
            ),
          },
          {
            key: 'register',
            label: '注册',
            children: (
              <Form onFinish={handleRegister} layout="vertical" size="large">
                <Form.Item name="username" rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, message: '用户名至少3个字符' },
                ]}>
                  <Input prefix={<UserOutlined />} placeholder="用户名" />
                </Form.Item>
                <Form.Item name="password" rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' },
                ]}>
                  <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>
                <Form.Item name="nickname">
                  <Input prefix={<UserOutlined />} placeholder="昵称（可选）" />
                </Form.Item>
                <Form.Item name="email" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
                  <Input prefix={<MailOutlined />} placeholder="邮箱（可选）" />
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={isLoading} block>
                    注册
                  </Button>
                </Form.Item>
              </Form>
            ),
          },
        ]} />
      </Card>
    </div>
  );
};

export default LoginPage;
