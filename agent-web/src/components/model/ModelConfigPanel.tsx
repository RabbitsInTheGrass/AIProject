import { useState, useEffect } from 'react';
import { Table, Button, Form, Input, InputNumber, Select, Space, Modal, Switch, message, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { modelApi } from '../../api';
import { useChatStore } from '../../stores/chatStore';
import type { ModelConfig, ModelPreset } from '../../types';

export default function ModelConfigPanel() {
  const [configs, setConfigs] = useState<ModelConfig[]>([]);
  const [presets, setPresets] = useState<ModelPreset[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<ModelConfig | null>(null);
  const [form] = Form.useForm();
  const { loadModelConfigs } = useChatStore();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    const [configsData, presetsData] = await Promise.all([modelApi.list(), modelApi.presets()]);
    setConfigs(configsData);
    setPresets(presetsData);
  };

  const handleCreate = () => {
    setEditingConfig(null);
    form.resetFields();
    form.setFieldsValue({ temperature: 0.7, maxTokens: 4096, isDefault: false });
    setModalOpen(true);
  };

  const handleEdit = (config: ModelConfig) => {
    setEditingConfig(config);
    form.setFieldsValue(config);
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await modelApi.delete(id);
    message.success('已删除');
    loadData();
    loadModelConfigs();
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    if (editingConfig?.id) {
      await modelApi.update(editingConfig.id, values);
      message.success('已更新');
    } else {
      await modelApi.create(values);
      message.success('已创建');
    }
    setModalOpen(false);
    loadData();
    loadModelConfigs();
  };

  const handlePreset = (preset: ModelPreset) => {
    form.setFieldsValue({
      name: preset.name,
      provider: preset.provider,
      baseUrl: preset.baseUrl,
      modelName: preset.model,
    });
  };

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '模型', dataIndex: 'modelName', key: 'modelName' },
    {
      title: '状态',
      key: 'status',
      render: (_: any, r: ModelConfig) => r.isDefault ? <Tag color="blue">默认</Tag> : null,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: ModelConfig) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id!)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          {presets.map(p => (
            <Button key={p.provider} size="small" onClick={() => { handleCreate(); setTimeout(() => handlePreset(p), 100); }}>
              + {p.name}
            </Button>
          ))}
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>自定义</Button>
      </div>

      <Table dataSource={configs} columns={columns} rowKey="id" size="small" pagination={false} />

      <Modal title={editingConfig ? '编辑模型' : '添加模型'} open={modalOpen} onOk={handleSave} onCancel={() => setModalOpen(false)} width={600}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="显示名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如: DeepSeek-V3" />
          </Form.Item>
          <Form.Item name="provider" label="提供商标识" rules={[{ required: true }]}>
            <Input placeholder="如: deepseek, openai, custom" />
          </Form.Item>
          <Form.Item name="baseUrl" label="API 地址" rules={[{ required: true }]}>
            <Input placeholder="如: https://api.deepseek.com" />
          </Form.Item>
          <Form.Item name="apiKey" label="API 密钥" rules={[{ required: true }]}>
            <Input.Password placeholder="sk-..." />
          </Form.Item>
          <Form.Item name="modelName" label="模型名称" rules={[{ required: true }]}>
            <Input placeholder="如: deepseek-chat" />
          </Form.Item>
          <Space style={{ width: '100%' }}>
            <Form.Item name="temperature" label="Temperature">
              <InputNumber min={0} max={2} step={0.1} />
            </Form.Item>
            <Form.Item name="maxTokens" label="Max Tokens">
              <InputNumber min={100} max={128000} step={100} />
            </Form.Item>
            <Form.Item name="isDefault" label="默认模型" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
