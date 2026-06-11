import { useState, useEffect } from 'react';
import { Button, List, Upload, Modal, Form, Input, Typography, Tag, Space, message, Empty, Progress } from 'antd';
import { PlusOutlined, UploadOutlined, DeleteOutlined, DatabaseOutlined } from '@ant-design/icons';
import { knowledgeApi } from '../../api';
import type { KnowledgeBase, KnowledgeDocument } from '../../types';

export default function KnowledgePanel() {
  const [bases, setBases] = useState<KnowledgeBase[]>([]);
  const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null);
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => { loadBases(); }, []);

  const loadBases = async () => {
    const data = await knowledgeApi.list();
    setBases(data);
  };

  const loadDocuments = async (kbId: number) => {
    const data = await knowledgeApi.documents(kbId);
    setDocuments(data);
  };

  const handleCreate = async () => {
    const values = await form.validateFields();
    await knowledgeApi.create(values);
    message.success('知识库已创建');
    setCreateModalOpen(false);
    form.resetFields();
    loadBases();
  };

  const handleDelete = async (id: number) => {
    await knowledgeApi.delete(id);
    message.success('已删除');
    if (selectedKb?.id === id) { setSelectedKb(null); setDocuments([]); }
    loadBases();
  };

  const handleUpload = async (file: File) => {
    if (!selectedKb) return;
    await knowledgeApi.uploadDocument(selectedKb.id, file);
    message.success(`${file.name} 已上传`);
    loadDocuments(selectedKb.id);
    loadBases();
  };

  const handleDeleteDoc = async (docId: number) => {
    if (!selectedKb) return;
    await knowledgeApi.deleteDocument(selectedKb.id, docId);
    message.success('文档已删除');
    loadDocuments(selectedKb.id);
    loadBases();
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Text strong>知识库列表</Typography.Text>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>新建知识库</Button>
      </div>

      <List
        dataSource={bases}
        locale={{ emptyText: <Empty description="暂无知识库" /> }}
        renderItem={(kb) => (
          <List.Item
            style={{ cursor: 'pointer', background: selectedKb?.id === kb.id ? '#e6f4ff' : undefined, padding: '8px 12px', borderRadius: 8 }}
            onClick={() => { setSelectedKb(kb); loadDocuments(kb.id); }}
            actions={[
              <Button key="del" type="text" danger size="small" icon={<DeleteOutlined />}
                onClick={(e) => { e.stopPropagation(); handleDelete(kb.id); }} />
            ]}
          >
            <List.Item.Meta
              avatar={<DatabaseOutlined style={{ fontSize: 20, color: '#1677ff' }} />}
              title={kb.name}
              description={`${kb.documentCount} 文档 · ${kb.chunkCount} 分块 · ${kb.status}`}
            />
          </List.Item>
        )}
      />

      {selectedKb && (
        <div style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Typography.Text strong>{selectedKb.name} - 文档列表</Typography.Text>
            <Upload
              beforeUpload={(file) => { handleUpload(file); return false; }}
              showUploadList={false}
            >
              <Button icon={<UploadOutlined />} size="small">上传文档</Button>
            </Upload>
          </div>
          <List
            size="small"
            dataSource={documents}
            locale={{ emptyText: <Empty description="暂无文档" /> }}
            renderItem={(doc) => (
              <List.Item
                actions={[
                  <Button key="del" type="text" danger size="small" icon={<DeleteOutlined />}
                    onClick={() => handleDeleteDoc(doc.id)} />
                ]}
              >
                <List.Item.Meta
                  title={doc.fileName}
                  description={
                    <Space>
                      <Tag>{doc.fileType}</Tag>
                      <Tag color={doc.status === 'DONE' ? 'success' : doc.status === 'ERROR' ? 'error' : 'processing'}>
                        {doc.status}
                      </Tag>
                      <span>{doc.chunkCount} 分块</span>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        </div>
      )}

      <Modal title="新建知识库" open={createModalOpen} onOk={handleCreate} onCancel={() => setCreateModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder="如: 项目文档" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="可选描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
