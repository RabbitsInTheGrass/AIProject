import React, { useEffect, useState } from 'react';
import { Tabs, List, Switch, Button, Modal, Form, Input, Select, message, Upload, Tag, Card } from 'antd';
import { PlusOutlined, UploadOutlined, DeleteOutlined, ApiOutlined } from '@ant-design/icons';
import { skillApi, httpToolApi, pluginApi } from '../../api';
import type { SkillConfig, HttpToolConfig, PluginTool } from '../../types';

const SkillPanel: React.FC = () => {
  const [builtinSkills, setBuiltinSkills] = useState<SkillConfig[]>([]);
  const [httpTools, setHttpTools] = useState<HttpToolConfig[]>([]);
  const [plugins, setPlugins] = useState<PluginTool[]>([]);
  const [httpModalOpen, setHttpModalOpen] = useState(false);
  const [pluginModalOpen, setPluginModalOpen] = useState(false);
  const [editingHttpTool, setEditingHttpTool] = useState<HttpToolConfig | null>(null);
  const [form] = Form.useForm();

  const loadData = async () => {
    try {
      const [skills, tools, plugs] = await Promise.all([
        skillApi.list(),
        httpToolApi.list().catch(() => []),
        pluginApi.list().catch(() => []),
      ]);
      setBuiltinSkills(skills);
      setHttpTools(tools);
      setPlugins(plugs);
    } catch (e) {
      console.error('Failed to load skills', e);
    }
  };

  useEffect(() => { loadData(); }, []);

  const toggleSkill = async (id: number) => {
    await skillApi.toggle(id);
    loadData();
  };

  const toggleHttpTool = async (id: number) => {
    await httpToolApi.toggle(id);
    loadData();
  };

  const togglePlugin = async (id: number) => {
    await pluginApi.toggle(id);
    loadData();
  };

  const saveHttpTool = async (values: any) => {
    try {
      if (editingHttpTool?.id) {
        await httpToolApi.update(editingHttpTool.id, values);
        message.success('更新成功');
      } else {
        await httpToolApi.create(values);
        message.success('创建成功');
      }
      setHttpModalOpen(false);
      setEditingHttpTool(null);
      form.resetFields();
      loadData();
    } catch (e: any) {
      message.error(e.message || '保存失败');
    }
  };

  const deleteHttpTool = async (id: number) => {
    await httpToolApi.delete(id);
    message.success('已删除');
    loadData();
  };

  const savePlugin = async (values: any) => {
    try {
      await pluginApi.create(values, undefined as any);
      message.success('创建成功');
      setPluginModalOpen(false);
      form.resetFields();
      loadData();
    } catch (e: any) {
      message.error(e.message || '保存失败');
    }
  };

  const deletePlugin = async (id: number) => {
    await pluginApi.delete(id);
    message.success('已删除');
    loadData();
  };

  return (
    <>
      <Tabs items={[
      {
        key: 'builtin',
        label: `内置工具 (${builtinSkills.length})`,
        children: (
          <List
            dataSource={builtinSkills}
            renderItem={(skill) => (
              <List.Item actions={[
                <Switch key="toggle" checked={skill.isEnabled} onChange={() => toggleSkill(skill.id)} size="small" />,
              ]}>
                <List.Item.Meta
                  title={<span>{skill.displayName} <Tag color="blue">{skill.category}</Tag></span>}
                  description={skill.description}
                />
              </List.Item>
            )}
          />
        ),
      },
      {
        key: 'http',
        label: `HTTP API (${httpTools.length})`,
        children: (
          <>
            <Button type="dashed" icon={<PlusOutlined />} onClick={() => {
              setEditingHttpTool(null);
              form.resetFields();
              setHttpModalOpen(true);
            }} style={{ marginBottom: 16 }} block>
              添加 HTTP 工具
            </Button>
            <List
              dataSource={httpTools}
              renderItem={(tool) => (
                <List.Item actions={[
                  <Switch key="toggle" checked={tool.isEnabled} onChange={() => toggleHttpTool(tool.id!)} size="small" />,
                  <Button key="edit" type="text" size="small" onClick={() => {
                    setEditingHttpTool(tool);
                    form.setFieldsValue(tool);
                    setHttpModalOpen(true);
                  }}>编辑</Button>,
                  <Button key="del" type="text" danger size="small" icon={<DeleteOutlined />}
                    onClick={() => deleteHttpTool(tool.id!)} />,
                ]}>
                  <List.Item.Meta
                    title={<span><ApiOutlined /> {tool.displayName} <Tag>{tool.requestMethod}</Tag></span>}
                    description={<span>{tool.description || ''}<br /><code>{tool.requestUrl}</code></span>}
                  />
                </List.Item>
              )}
            />
          </>
        ),
      },
      {
        key: 'plugin',
        label: `jar 插件 (${plugins.length})`,
        children: (
          <>
            <Button type="dashed" icon={<UploadOutlined />} onClick={() => setPluginModalOpen(true)} style={{ marginBottom: 16 }} block>
              上传插件 jar
            </Button>
            <List
              dataSource={plugins}
              renderItem={(plugin) => (
                <List.Item actions={[
                  <Switch key="toggle" checked={plugin.isEnabled} onChange={() => togglePlugin(plugin.id!)} size="small" />,
                  <Button key="del" type="text" danger size="small" icon={<DeleteOutlined />}
                    onClick={() => deletePlugin(plugin.id!)} />,
                ]}>
                  <List.Item.Meta
                    title={plugin.displayName}
                    description={<span>{plugin.description || ''}<br /><code>{plugin.mainClass}</code></span>}
                  />
                </List.Item>
              )}
            />
          </>
        ),
      },
    ]} />

      <HttpToolModal
        open={httpModalOpen}
        onClose={() => { setHttpModalOpen(false); setEditingHttpTool(null); form.resetFields(); }}
        onSubmit={saveHttpTool}
        form={form}
        editing={editingHttpTool}
      />
    </>
  );
};

// HTTP Tool Form Modal - rendered inline
export const HttpToolModal: React.FC<{
  open: boolean;
  onClose: () => void;
  onSubmit: (values: any) => void;
  form: any;
  editing: HttpToolConfig | null;
}> = ({ open, onClose, onSubmit, form, editing }) => (
  <Modal
    title={editing ? '编辑 HTTP 工具' : '添加 HTTP 工具'}
    open={open}
    onCancel={onClose}
    onOk={() => form.submit()}
    width={600}
  >
    <Form form={form} onFinish={onSubmit} layout="vertical">
      <Form.Item name="name" label="工具标识" rules={[{ required: true }]}>
        <Input placeholder="e.g., weather_api" />
      </Form.Item>
      <Form.Item name="displayName" label="显示名称" rules={[{ required: true }]}>
        <Input placeholder="e.g., 天气查询" />
      </Form.Item>
      <Form.Item name="description" label="描述">
        <Input.TextArea placeholder="工具功能描述，LLM 会根据此描述决定是否调用" rows={2} />
      </Form.Item>
      <Form.Item name="requestUrl" label="请求 URL" rules={[{ required: true }]}>
        <Input placeholder="https://api.example.com/weather?city={{city}}" />
      </Form.Item>
      <Form.Item name="requestMethod" label="请求方法" initialValue="GET">
        <Select options={[
          { value: 'GET' }, { value: 'POST' }, { value: 'PUT' }, { value: 'DELETE' },
        ]} />
      </Form.Item>
      <Form.Item name="requestHeaders" label="请求 Headers (JSON)">
        <Input.TextArea placeholder='{"Authorization": "Bearer xxx"}' rows={2} />
      </Form.Item>
      <Form.Item name="requestBodyTemplate" label="请求 Body 模板 (JSON)">
        <Input.TextArea placeholder='{"query": "{{input}}"}' rows={2} />
      </Form.Item>
      <Form.Item name="parameterSchema" label="参数 Schema (JSON)">
        <Input.TextArea placeholder='{"type":"object","properties":{"city":{"type":"string"}}}' rows={2} />
      </Form.Item>
      <Form.Item name="responseExtractPath" label="响应提取路径">
        <Input placeholder="e.g., data.result" />
      </Form.Item>
      <Form.Item name="timeoutMs" label="超时(ms)" initialValue={30000}>
        <Input type="number" />
      </Form.Item>
    </Form>
  </Modal>
);

export default SkillPanel;
