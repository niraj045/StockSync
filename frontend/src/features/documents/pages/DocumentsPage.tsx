import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Empty, Form, Input, Select, Table, Tag, Upload, message } from 'antd';
import { DownloadOutlined, FileAddOutlined, InboxOutlined } from '@ant-design/icons';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';

interface OptionRow { id: number; [key: string]: unknown }
interface PageResponse { content: OptionRow[] }
interface Attachment {
  id: number; documentType: string; originalFilename: string; contentType: string;
  fileSize: number; description?: string; uploadedBy: string; createdAt: string;
}

const entityConfig = {
  PARTY: { endpoint: '/parties', label: 'Party', name: (row: OptionRow) => String(row.legalName) },
  SITE: { endpoint: '/sites', label: 'Site', name: (row: OptionRow) => `${row.siteCode} · ${row.siteName}` },
  VENDOR: { endpoint: '/vendors', label: 'Vendor', name: (row: OptionRow) => String(row.name) },
  ITEM: { endpoint: '/items', label: 'Item', name: (row: OptionRow) => `${row.itemCode} · ${row.itemName}` },
} as const;
type EntityType = keyof typeof entityConfig;

export function DocumentsPage() {
  const { user } = useAuth();
  const canUpload = user?.roles.includes('ROLE_ADMIN') ?? false;
  const [form] = Form.useForm();
  const [entityType, setEntityType] = useState<EntityType>('PARTY');
  const [entityId, setEntityId] = useState<number>();
  const queryClient = useQueryClient();
  const config = entityConfig[entityType];

  const entities = useQuery({
    queryKey: ['document-entities', entityType],
    queryFn: async () => (await apiClient.get<PageResponse>(config.endpoint, { params: { size: 200, sort: 'id,desc' } })).data.content,
  });
  const options = useMemo(() => (entities.data ?? []).map((row) => ({ value: row.id, label: config.name(row) })), [entities.data, config]);
  const attachments = useQuery({
    queryKey: ['documents', entityType, entityId],
    queryFn: async () => (await apiClient.get<Attachment[]>('/files', { params: { entityType, entityId } })).data,
    enabled: Boolean(entityId),
  });
  const upload = useMutation({
    mutationFn: async (values: { documentType: string; description?: string; file: { fileList?: { originFileObj?: File }[] } }) => {
      const selected = values.file?.fileList?.[0]?.originFileObj;
      if (!selected || !entityId) throw new Error('Select a business record and file');
      const body = new FormData();
      body.append('entityType', entityType); body.append('entityId', String(entityId));
      body.append('documentType', values.documentType); body.append('description', values.description ?? '');
      body.append('file', selected);
      return apiClient.post('/files', body, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
    },
    onSuccess: () => {
      message.success('Document uploaded'); form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['documents', entityType, entityId] });
    },
    onError: (error: unknown) => {
      const response = error as { response?: { data?: { message?: string } } };
      message.error(response.response?.data?.message ?? (error as Error).message);
    },
  });

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div><h1 className="page-heading">Documents</h1><p className="page-description">Store and retrieve files against parties, sites, vendors and items.</p></div>
      </div>
      <div className="document-layout">
        <Card className="premium-card" title="Choose business record">
          <Form layout="vertical">
            <Form.Item label="Record type">
              <Select value={entityType} options={Object.entries(entityConfig).map(([value, entry]) => ({ value, label: entry.label }))}
                onChange={(value: EntityType) => { setEntityType(value); setEntityId(undefined); }} />
            </Form.Item>
            <Form.Item label={config.label}>
              <Select showSearch optionFilterProp="label" value={entityId} options={options} loading={entities.isLoading}
                placeholder={`Select ${config.label.toLowerCase()}`} onChange={setEntityId} />
            </Form.Item>
          </Form>
          {canUpload && entityId && (
            <Form form={form} layout="vertical" onFinish={(values) => upload.mutate(values)}>
              <Form.Item name="documentType" label="Document type" rules={[{ required: true }]}>
                <Input placeholder="e.g. GST certificate" />
              </Form.Item>
              <Form.Item name="description" label="Description"><Input /></Form.Item>
              <Form.Item name="file" valuePropName="file" rules={[{ required: true }]}>
                <Upload.Dragger beforeUpload={() => false} maxCount={1} accept=".pdf,.docx,.xlsx,.jpg,.jpeg,.png">
                  <InboxOutlined style={{ fontSize: 28, color: '#0f766e' }} />
                  <p>Drop a file here or click to select</p>
                </Upload.Dragger>
              </Form.Item>
              <Button type="primary" htmlType="submit" icon={<FileAddOutlined />} loading={upload.isPending}>Upload document</Button>
            </Form>
          )}
        </Card>
        <Card className="premium-card" title="Attached documents">
          {!entityId ? <Empty description="Select a record to view its documents" /> :
            <Table rowKey="id" dataSource={attachments.data} loading={attachments.isLoading} pagination={false}
              columns={[
                { title: 'File', dataIndex: 'originalFilename' },
                { title: 'Type', dataIndex: 'documentType', render: (value) => <Tag className="role-tag">{value}</Tag> },
                { title: 'Size', dataIndex: 'fileSize', render: (value) => `${(Number(value) / 1024).toFixed(1)} KB` },
                { title: '', key: 'download', render: (_, row: Attachment) =>
                  <Button type="link" icon={<DownloadOutlined />} href={`/api/v1/files/${row.id}/download`}>Download</Button> },
              ]} />}
        </Card>
      </div>
    </div>
  );
}
