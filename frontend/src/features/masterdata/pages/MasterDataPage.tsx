import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, InputNumber, Select, Switch, Table, Tag, message } from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import { LedgerImportModal } from '../components/LedgerImportModal';
import { UploadOutlined } from '@ant-design/icons';

type Kind = 'categories' | 'items' | 'parties' | 'sites' | 'vendors';
type Row = Record<string, unknown> & { id: number; version: number };
type FieldType = 'text' | 'textarea' | 'number' | 'boolean' | 'select' | 'date';
interface Field {
  name: string;
  label: string;
  type?: FieldType;
  required?: boolean;
  options?: { label: string; value: string | number }[];
  editDisabled?: boolean;
}
interface Config {
  title: string;
  description: string;
  singular: string;
  fields: Field[];
  columns: { key: string; title: string; render?: (value: unknown, row: Row) => React.ReactNode }[];
}
interface PageResponse { content: Row[]; totalElements: number }

const statusTag = (value: unknown) => (
  <Tag className="status-tag" color={value ? 'success' : 'default'}>{value ? 'Active' : 'Inactive'}</Tag>
);

const baseConfigs: Record<Kind, Config> = {
  categories: {
    title: 'Item categories', description: 'Organise inventory items into controlled material groups.', singular: 'category',
    fields: [
      { name: 'name', label: 'Category name', required: true },
      { name: 'description', label: 'Description', type: 'textarea' },
      { name: 'active', label: 'Active', type: 'boolean' },
    ],
    columns: [
      { key: 'name', title: 'Category' }, { key: 'description', title: 'Description' },
      { key: 'active', title: 'Status', render: statusTag },
    ],
  },
  items: {
    title: 'Item master', description: 'Maintain material specifications, values and stock thresholds.', singular: 'item',
    fields: [
      { name: 'itemCode', label: 'Item code', required: true, editDisabled: true },
      { name: 'itemName', label: 'Item name', required: true },
      { name: 'categoryId', label: 'Category', type: 'select', required: true },
      { name: 'size', label: 'Size' }, { name: 'unit', label: 'Unit', required: true },
      { name: 'weightPerPiece', label: 'Weight per piece', type: 'number' },
      { name: 'purchaseValue', label: 'Purchase value', type: 'number' },
      { name: 'rentalConfiguration', label: 'Rental configuration', type: 'textarea' },
      { name: 'lossRate', label: 'Loss rate', type: 'number' },
      { name: 'scrapValue', label: 'Scrap value', type: 'number' },
      { name: 'minimumStock', label: 'Minimum stock', type: 'number', required: true },
      { name: 'active', label: 'Active', type: 'boolean' },
    ],
    columns: [
      { key: 'itemCode', title: 'Code' }, { key: 'itemName', title: 'Item' },
      { key: 'categoryName', title: 'Category' }, { key: 'unit', title: 'Unit' },
      { key: 'minimumStock', title: 'Minimum stock' }, { key: 'active', title: 'Status', render: statusTag },
    ],
  },
  parties: {
    title: 'Parties', description: 'Maintain customer businesses and their statutory details.', singular: 'party',
    fields: [
      { name: 'legalName', label: 'Legal name', required: true }, { name: 'tradeName', label: 'Trade name' },
      { name: 'gstin', label: 'GSTIN' }, { name: 'pan', label: 'PAN' },
      { name: 'contactPerson', label: 'Contact person' }, { name: 'phone', label: 'Phone' },
      { name: 'email', label: 'Email' }, { name: 'address', label: 'Address', type: 'textarea' },
      { name: 'state', label: 'State' }, { name: 'notes', label: 'Notes', type: 'textarea' },
      { name: 'active', label: 'Active', type: 'boolean' },
    ],
    columns: [
      { key: 'legalName', title: 'Legal name' }, { key: 'tradeName', title: 'Trade name' },
      { key: 'gstin', title: 'GSTIN' }, { key: 'contactPerson', title: 'Contact' },
      { key: 'phone', title: 'Phone' }, { key: 'active', title: 'Status', render: statusTag },
    ],
  },
  sites: {
    title: 'Sites', description: 'Track party sites, operational state and schedule information.', singular: 'site',
    fields: [
      { name: 'partyId', label: 'Party', type: 'select', required: true },
      { name: 'siteName', label: 'Site name', required: true }, { name: 'siteCode', label: 'Site code', required: true, editDisabled: true },
      { name: 'address', label: 'Address', type: 'textarea' }, { name: 'contactPerson', label: 'Contact person' },
      { name: 'startDate', label: 'Start date', type: 'date' }, { name: 'expectedEndDate', label: 'Expected end date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', required: true, options: [
        { label: 'Active', value: 'ACTIVE' }, { label: 'On hold', value: 'ON_HOLD' },
        { label: 'Defaulter', value: 'DEFAULTER' }, { label: 'Closed', value: 'CLOSED' },
      ] },
      { name: 'defaulter', label: 'Defaulter', type: 'boolean' },
      { name: 'closedDate', label: 'Closed date', type: 'date' }, { name: 'notes', label: 'Notes', type: 'textarea' },
      { name: 'excelTemplateCode', label: 'Site Excel format', type: 'select', options: [{ label: 'Standard export', value: 'STANDARD' }, { label: 'SBUT D&R', value: 'SBUT_DR_V1' }] },
    ],
    columns: [
      { key: 'siteCode', title: 'Code' }, { key: 'siteName', title: 'Site' },
      { key: 'partyName', title: 'Party' }, { key: 'status', title: 'Status', render: (v) => <Tag>{String(v).replace('_', ' ')}</Tag> },
      { key: 'defaulter', title: 'Risk', render: (v) => v ? <Tag color="error">Defaulter</Tag> : <Tag>Clear</Tag> },
    ],
  },
  vendors: {
    title: 'Vendors', description: 'Maintain suppliers used for purchasing inventory.', singular: 'vendor',
    fields: [
      { name: 'name', label: 'Vendor name', required: true }, { name: 'gstin', label: 'GSTIN' },
      { name: 'contactPerson', label: 'Contact person' }, { name: 'phone', label: 'Phone' },
      { name: 'email', label: 'Email' }, { name: 'address', label: 'Address', type: 'textarea' },
      { name: 'notes', label: 'Notes', type: 'textarea' }, { name: 'active', label: 'Active', type: 'boolean' },
    ],
    columns: [
      { key: 'name', title: 'Vendor' }, { key: 'gstin', title: 'GSTIN' },
      { key: 'contactPerson', title: 'Contact' }, { key: 'phone', title: 'Phone' },
      { key: 'active', title: 'Status', render: statusTag },
    ],
  },
};

export function MasterDataPage({ kind }: { kind: Kind }) {
  const [form] = Form.useForm();
  const { user } = useAuth();
  const canManage = user?.roles.includes('ROLE_ADMIN') ?? false;
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<Row | null>(null);
  const [open, setOpen] = useState(false);
  const [ledgerModalOpen, setLedgerModalOpen] = useState(false);
  const [selectedSite, setSelectedSite] = useState<Row | null>(null);
  const queryClient = useQueryClient();
  const config = baseConfigs[kind];

  const categories = useQuery({
    queryKey: ['category-options'],
    queryFn: async () => (await apiClient.get<PageResponse>('/categories', { params: { size: 200, active: true } })).data.content,
    enabled: kind === 'items',
  });
  const parties = useQuery({
    queryKey: ['party-options'],
    queryFn: async () => (await apiClient.get<PageResponse>('/parties', { params: { size: 200, active: true } })).data.content,
    enabled: kind === 'sites',
  });
  const fields = useMemo(() => config.fields.map((field) => {
    if (field.name === 'categoryId') return { ...field, options: (categories.data ?? []).map((c) => ({ label: String(c.name), value: Number(c.id) })) };
    if (field.name === 'partyId') return { ...field, options: (parties.data ?? []).map((p) => ({ label: String(p.legalName), value: Number(p.id) })) };
    return field;
  }), [config.fields, categories.data, parties.data]);

  const records = useQuery({
    queryKey: ['master-data', kind, search, page],
    queryFn: async () => (await apiClient.get<PageResponse>(`/${kind}`, { params: { page: page - 1, size: 10, search, sort: 'id,desc' } })).data,
  });
  const save = useMutation({
    mutationFn: async (values: Record<string, unknown>) => editing
      ? (await apiClient.put(`/${kind}/${editing.id}`, { ...values, version: editing.version })).data
      : (await apiClient.post(`/${kind}`, values)).data,
    onSuccess: () => {
      message.success(`${config.singular[0].toUpperCase()}${config.singular.slice(1)} saved`);
      setOpen(false); setEditing(null); form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['master-data', kind] });
      queryClient.invalidateQueries({ queryKey: [`${config.singular}-options`] });
    },
    onError: (error: unknown) => {
      const response = error as { response?: { data?: { message?: string } } };
      message.error(response.response?.data?.message ?? `Unable to save ${config.singular}`);
    },
  });

  const columns = [
    ...config.columns.map((column) => ({
      title: column.title, dataIndex: column.key, key: column.key,
      render: column.render ? (value: unknown, row: Row) => column.render?.(value, row) : undefined,
    })),
    ...(canManage ? [{ title: 'Actions', key: 'actions', width: kind === 'sites' ? 140 : 90, render: (_: unknown, row: Row) => (
      <div style={{ display: 'flex', gap: 8 }}>
        <Button type="text" className="action-button" icon={<EditOutlined />} onClick={() => {
          setEditing(row); form.setFieldsValue(row); setOpen(true);
        }}>Edit</Button>
        {kind === 'sites' && (
          <Button type="text" className="action-button" icon={<UploadOutlined />} onClick={() => {
            setSelectedSite(row); setLedgerModalOpen(true);
          }}>Ledger</Button>
        )}
      </div>
    ) }] : []),
  ];

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div><h1 className="page-heading">{config.title}</h1><p className="page-description">{config.description}</p></div>
        {canManage && <Button type="primary" icon={<PlusOutlined />} onClick={() => {
          setEditing(null); form.resetFields(); form.setFieldsValue({ active: true, defaulter: false, minimumStock: 0, status: 'ACTIVE' }); setOpen(true);
        }}>Add {config.singular}</Button>}
      </div>
      <Card className="premium-card">
        <div className="filters-bar">
          <Input allowClear prefix={<SearchOutlined />} placeholder={`Search ${config.title.toLowerCase()}`}
            value={search} onChange={(event) => { setSearch(event.target.value); setPage(1); }} style={{ width: 320 }} />
        </div>
        <Table rowKey="id" dataSource={records.data?.content} columns={columns} loading={records.isLoading}
          scroll={{ x: 850 }} pagination={{ current: page, pageSize: 10, total: records.data?.totalElements,
            onChange: setPage, showSizeChanger: false }} />
      </Card>
      <FormDrawer open={open} title={`${editing ? 'Edit' : 'Add'} ${config.singular}`} width={kind === 'categories' ? 520 : 720}
        subtitle={editing ? `Update this ${config.singular}'s controlled information.` : `Create a new ${config.singular} and configure its details.`}
        onClose={() => setOpen(false)} onSubmit={() => form.submit()} loading={save.isPending}
        okText={editing ? 'Save changes' : `Add ${config.singular}`}>
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} style={{ marginTop: 20 }}>
          {(kind === 'items' ? [
            { title: 'Basic information', names: ['itemCode','itemName','categoryId','size','unit','weightPerPiece'] },
            { title: 'Stock and valuation', names: ['purchaseValue','minimumStock'] },
            { title: 'Rental configuration', names: ['rentalConfiguration'] },
            { title: 'Loss and scrap', names: ['lossRate','scrapValue','active'] },
          ] : [{ title: `${config.singular.charAt(0).toUpperCase()}${config.singular.slice(1)} details`, names: fields.map((field) => field.name) }]).map((section) => <section className="form-section" key={section.title}>
          <h3 className="form-section-title">{section.title}</h3>
          <div className="master-form-grid">
            {fields.filter((field) => section.names.includes(field.name)).map((field) => (
              <Form.Item key={field.name} name={field.name} label={field.label} valuePropName={field.type === 'boolean' ? 'checked' : 'value'}
                rules={field.required ? [{ required: true, message: `${field.label} is required` }] : undefined}
                className={field.type === 'textarea' ? 'master-form-wide' : undefined}>
                {field.type === 'textarea' ? <Input.TextArea rows={3} /> :
                  field.type === 'number' ? <InputNumber min={0} style={{ width: '100%' }} /> :
                  field.type === 'boolean' ? <Switch /> :
                  field.type === 'select' ? <Select options={field.options} /> :
                  field.type === 'date' ? <Input type="date" /> :
                  <Input disabled={Boolean(editing && field.editDisabled)} />}
              </Form.Item>
            ))}
          </div>
          </section>)}
        </Form>
      </FormDrawer>
      {kind === 'sites' && (
        <LedgerImportModal 
          open={ledgerModalOpen} 
          siteId={selectedSite?.id as number} 
          siteName={String(selectedSite?.siteName || '')} 
          onClose={() => setLedgerModalOpen(false)} 
        />
      )}
    </div>
  );
}
