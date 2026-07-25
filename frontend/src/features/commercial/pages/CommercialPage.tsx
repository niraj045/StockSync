import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  App as AntApp, Button, Card, Descriptions, Empty, Form, Input, InputNumber, Modal, Select, Space,
  Table, Tabs, Tag, Upload, message,
} from 'antd';
import {
  CheckOutlined, CopyOutlined, DownloadOutlined, FileAddOutlined, PlusOutlined,
  SearchOutlined, SendOutlined, StopOutlined, UploadOutlined,
} from '@ant-design/icons';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';

type Kind = 'quotations' | 'agreements' | 'orders';
type Row = Record<string, any> & { id: number; version: number; status: string; items: any[] };
interface PageResponse { content: Row[]; totalElements: number }
interface OptionRow { id: number; [key: string]: unknown }

const statusColor: Record<string, string> = {
  DRAFT: 'default', SENT: 'processing', APPROVED: 'success', REJECTED: 'error',
  EXPIRED: 'warning', CONVERTED: 'purple', ACTIVE: 'success', TERMINATED: 'error',
  CONFIRMED: 'processing', COMPLETED: 'success', CANCELLED: 'error',
};
const money = (value: unknown) => `₹${Number(value ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
const errorMessage = (error: unknown) =>
  (error as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'The request could not be completed';

export function CommercialPage({ kind }: { kind: Kind }) {
  const { modal } = AntApp.useApp();
  const { user } = useAuth();
  const canManage = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS') ?? false;
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const [form] = Form.useForm();
  const [convertForm] = Form.useForm();
  const [templateForm] = Form.useForm();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [selected, setSelected] = useState<Row | null>(null);
  const [convertQuotation, setConvertQuotation] = useState<Row | null>(null);
  const [templateOpen, setTemplateOpen] = useState(false);
  const [agreementId, setAgreementId] = useState<number>();

  const records = useQuery({
    queryKey: ['commercial', kind, page, search],
    queryFn: async () => (await apiClient.get<PageResponse>(`/${kind}`, {
      params: { page: page - 1, size: 10, search, sort: 'id,desc' },
    })).data,
  });
  const parties = useQuery({
    queryKey: ['commercial-options', 'parties'],
    queryFn: async () => (await apiClient.get<PageResponse>('/parties', { params: { size: 200, active: true } })).data.content,
    enabled: kind !== 'orders',
  });
  const sites = useQuery({
    queryKey: ['commercial-options', 'sites'],
    queryFn: async () => (await apiClient.get<PageResponse>('/sites', { params: { size: 200 } })).data.content,
    enabled: kind !== 'orders',
  });
  const items = useQuery({
    queryKey: ['commercial-options', 'items'],
    queryFn: async () => (await apiClient.get<PageResponse>('/items', { params: { size: 500, active: true } })).data.content,
  });
  const agreements = useQuery({
    queryKey: ['commercial-options', 'agreements'],
    queryFn: async () => (await apiClient.get<PageResponse>('/agreements', { params: { size: 200, status: 'ACTIVE' } })).data.content,
    enabled: kind === 'orders',
  });
  const templates = useQuery({
    queryKey: ['agreement-templates'],
    queryFn: async () => (await apiClient.get<Row[]>('/agreement-templates')).data,
    enabled: kind !== 'orders',
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['commercial'] });
    queryClient.invalidateQueries({ queryKey: ['commercial-options', 'agreements'] });
  };
  const save = useMutation({
    mutationFn: async (values: Record<string, any>) => {
      const body = normalize(values);
      return editing
        ? (await apiClient.put(`/${kind}/${editing.id}`, { ...body, version: editing.version })).data
        : (await apiClient.post(`/${kind}`, body)).data;
    },
    onSuccess: () => { message.success(`${singular(kind)} saved`); setOpen(false); setEditing(null); form.resetFields(); refresh(); },
    onError: (error) => message.error(errorMessage(error)),
  });
  const action = useMutation({
    mutationFn: async ({ row, name }: { row: Row; name: string }) =>
      (await apiClient.post(`/${kind}/${row.id}/${name}`)).data,
    onSuccess: (_, variables) => { message.success(`${singular(kind)} ${variables.name} successful`); refresh(); },
    onError: (error) => message.error(errorMessage(error)),
  });
  const clone = useMutation({
    mutationFn: async (row: Row) => (await apiClient.post(`/quotations/${row.id}/clone`)).data,
    onSuccess: () => { message.success('Quotation cloned as a new draft'); refresh(); },
    onError: (error) => message.error(errorMessage(error)),
  });
  const convert = useMutation({
    mutationFn: async (values: Record<string, unknown>) =>
      (await apiClient.post(`/quotations/${convertQuotation?.id}/convert`, values)).data,
    onSuccess: () => { message.success('Agreement draft created'); setConvertQuotation(null); convertForm.resetFields(); refresh(); },
    onError: (error) => message.error(errorMessage(error)),
  });
  const uploadTemplate = useMutation({
    mutationFn: async (values: Record<string, any>) => {
      const data = new FormData();
      data.append('name', values.name);
      if (values.description) data.append('description', values.description);
      data.append('file', values.file[0].originFileObj);
      return (await apiClient.post('/agreement-templates', data, { headers: { 'Content-Type': 'multipart/form-data' } })).data;
    },
    onSuccess: () => {
      message.success('Agreement template uploaded'); setTemplateOpen(false); templateForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['agreement-templates'] });
    },
    onError: (error) => message.error(errorMessage(error)),
  });

  const config = CONFIG[kind];
  const columns = useMemo(() => [
    { title: config.numberLabel, dataIndex: config.numberKey, key: config.numberKey },
    ...(kind === 'orders' ? [{ title: 'Agreement', dataIndex: 'agreementNumber' }] : []),
    { title: 'Party', dataIndex: 'partyName' },
    { title: 'Site', dataIndex: 'siteName' },
    { title: config.dateLabel, dataIndex: config.dateKey },
    ...(kind === 'quotations' ? [{ title: 'Total', dataIndex: 'grandTotal', render: money }] : []),
    { title: 'Status', dataIndex: 'status', render: (value: string) => <Tag color={statusColor[value]}>{value}</Tag> },
    {
      title: 'Actions', key: 'actions', width: 310, render: (_: unknown, row: Row) => (
        <Space wrap>
          <Button size="small" onClick={() => setSelected(row)}>View</Button>
          {canManage && row.status === 'DRAFT' && <Button size="small" onClick={() => edit(row)}>Edit</Button>}
          {kind === 'quotations' && canManage && <>
            <Button size="small" icon={<CopyOutlined />} onClick={() => clone.mutate(row)}>Clone</Button>
            {row.status === 'DRAFT' && <Button size="small" icon={<SendOutlined />} onClick={() => run(row, 'send')}>Send</Button>}
            {row.status === 'SENT' && <Button size="small" icon={<CheckOutlined />} onClick={() => run(row, 'approve')}>Approve</Button>}
            {row.status === 'SENT' && <Button size="small" danger onClick={() => run(row, 'reject')}>Reject</Button>}
            {row.status === 'APPROVED' && <Button size="small" type="primary" onClick={() => openConvert(row)}>Convert</Button>}
          </>}
          {kind === 'agreements' && canManage && <>
            {row.status === 'DRAFT' && <Button size="small" icon={<FileAddOutlined />} onClick={() => run(row, 'generate')}>Generate</Button>}
            {row.status === 'GENERATED' && row.generated && <Button size="small" type="primary" onClick={() => run(row, 'activate')}>Activate</Button>}
            {row.generated && <Button size="small" icon={<DownloadOutlined />} href={`/api/v1/agreements/${row.id}/document`}>DOCX</Button>}
            {row.status === 'ACTIVE' && isAdmin && <Button size="small" danger icon={<StopOutlined />} onClick={() => run(row, 'terminate')}>Terminate</Button>}
          </>}
          {kind === 'orders' && canManage && <>
            {row.status === 'DRAFT' && <Button size="small" type="primary" onClick={() => run(row, 'confirm')}>Confirm</Button>}
            {['DRAFT', 'CONFIRMED'].includes(row.status) && <Button size="small" danger onClick={() => run(row, 'cancel')}>Cancel</Button>}
          </>}
        </Space>
      ),
    },
  ], [kind, canManage, isAdmin]);

  function normalize(values: Record<string, any>) {
    const body = { ...values };
    if (kind === 'orders') body.items = values.items.map((line: any) => ({ itemId: line.itemId, orderedQuantity: line.quantity }));
    if (kind === 'agreements') body.items = values.items.map((line: any) => ({
      itemId: line.itemId, agreedQuantity: line.quantity, unitRate: line.unitRate ?? 0, rentalRate: line.rentalRate ?? 0, notes: line.notes,
    }));
    if (kind === 'quotations') body.items = values.items.map((line: any) => ({
      itemId: line.itemId, quantity: line.quantity, unitRate: line.unitRate ?? 0, rentalRate: line.rentalRate ?? 0, notes: line.notes,
    }));
    return body;
  }
  function defaults() {
    const today = new Date().toISOString().slice(0, 10);
    return kind === 'quotations'
      ? { quotationDate: today, validUntil: today, rentalType: 'PER_PIECE_PER_DAY', transportCharge: 0, loadingCharge: 0, unloadingCharge: 0, taxRate: 18, items: [{}] }
      : kind === 'agreements'
        ? { effectiveDate: today, rentalType: 'PER_PIECE_PER_DAY', securityDeposit: 0, transportCharge: 0, loadingCharge: 0, unloadingCharge: 0, items: [{}] }
        : { orderDate: today, items: [{}] };
  }
  function edit(row: Row) {
    setEditing(row);
    form.setFieldsValue({
      ...row,
      items: row.items.map((line: any) => ({ ...line, quantity: line.quantity ?? line.agreedQuantity ?? line.orderedQuantity })),
    });
    if (kind === 'orders') setAgreementId(row.agreementId);
    setOpen(true);
  }
  function run(row: Row, name: string) {
    modal.confirm({
      title: `${name[0].toUpperCase()}${name.slice(1)} ${singular(kind)}?`,
      content: 'This changes the record status and will be recorded in the audit log.',
      okText: 'Confirm',
      onOk: () => action.mutateAsync({ row, name }),
    });
  }
  function openConvert(row: Row) {
    setConvertQuotation(row);
    convertForm.setFieldsValue({ effectiveDate: new Date().toISOString().slice(0, 10), securityDeposit: 0 });
  }
  const selectedAgreement = (agreements.data ?? []).find((row) => row.id === agreementId);
  const selectableItems = kind === 'orders' && selectedAgreement ? selectedAgreement.items : items.data ?? [];

  const main = (
    <div className="page-stack">
      <div className="page-header-container">
        <div><h1 className="page-heading">{config.title}</h1><p className="page-description">{config.description}</p></div>
        <Space>
          {kind === 'agreements' && isAdmin && <Button icon={<UploadOutlined />} onClick={() => setTemplateOpen(true)}>Upload template</Button>}
          {canManage && <Button type="primary" icon={<PlusOutlined />} onClick={() => {
            setEditing(null); setAgreementId(undefined); form.resetFields(); form.setFieldsValue(defaults()); setOpen(true);
          }}>Add {singular(kind).toLowerCase()}</Button>}
        </Space>
      </div>
      <Card className="premium-card">
        <div className="filters-bar">
          <Input allowClear prefix={<SearchOutlined />} placeholder={`Search ${kind}`} value={search}
            onChange={(event) => { setSearch(event.target.value); setPage(1); }} style={{ width: 320 }} />
        </div>
        <Table rowKey="id" dataSource={records.data?.content} columns={columns} loading={records.isLoading}
          locale={{ emptyText: <Empty description={`No ${kind} found`} /> }} scroll={{ x: 1050 }}
          pagination={{ current: page, pageSize: 10, total: records.data?.totalElements, onChange: setPage, showSizeChanger: false }} />
      </Card>
    </div>
  );

  return <>
    {kind === 'agreements' ? <Tabs items={[
      { key: 'agreements', label: 'Agreements', children: main },
      { key: 'templates', label: 'Document templates', children: <TemplateTable data={templates.data ?? []} loading={templates.isLoading} /> },
    ]} /> : main}
    <Modal open={open} title={`${editing ? 'Edit' : 'Add'} ${singular(kind)}`} width={900}
      onCancel={() => setOpen(false)} onOk={() => form.submit()} confirmLoading={save.isPending} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} style={{ marginTop: 18 }}>
        <div className="master-form-grid">
          {kind !== 'orders' ? <>
            <Form.Item name="partyId" label="Party" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={(parties.data ?? []).map((p: OptionRow) => ({ value: p.id, label: String(p.legalName) }))} />
            </Form.Item>
            <Form.Item name="siteId" label="Site" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={(sites.data ?? []).map((s: OptionRow) => ({ value: s.id, label: `${s.siteCode} — ${s.siteName}` }))} />
            </Form.Item>
          </> : <Form.Item name="agreementId" label="Active agreement" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" disabled={Boolean(editing)}
              onChange={(value) => { setAgreementId(value); form.setFieldValue('items', [{}]); }}
              options={(agreements.data ?? []).map((a) => ({ value: a.id, label: `${a.agreementNumber} — ${a.siteName}` }))} />
          </Form.Item>}
          <Form.Item name={config.dateKey} label={config.dateLabel} rules={[{ required: true }]}><Input type="date" /></Form.Item>
          {kind === 'quotations' && <Form.Item name="validUntil" label="Valid until" rules={[{ required: true }]}><Input type="date" /></Form.Item>}
          {kind === 'agreements' && <>
            <Form.Item name="expiryDate" label="Expiry date"><Input type="date" /></Form.Item>
            <Form.Item name="templateId" label="Document template"><Select allowClear options={(templates.data ?? []).map((t) => ({ value: t.id, label: t.name }))} /></Form.Item>
          </>}
          {kind !== 'orders' && <Form.Item name="rentalType" label="Rental type" rules={[{ required: true }]}>
            <Select options={[
              { value: 'PER_PIECE_PER_DAY', label: 'Per piece per day' },
              { value: 'PLATE_AREA_PER_DAY', label: 'Plate area per day' },
              { value: 'SCAFFOLD_AREA_PER_DAY', label: 'Scaffold area per day' },
              { value: 'PLOT_AREA_PER_DAY', label: 'Plot area per day' },
              { value: 'SLAB_BASED', label: 'Slab based' },
              { value: 'FIXED_RATE', label: 'Fixed rate' },
            ]} />
          </Form.Item>}
          {kind === 'agreements' && <Form.Item name="securityDeposit" label="Security deposit" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>}
          {kind !== 'orders' && <>
            <MoneyField name="transportCharge" label="Transport charge" /><MoneyField name="loadingCharge" label="Loading charge" />
            <MoneyField name="unloadingCharge" label="Unloading charge" />
          </>}
          {kind === 'quotations' && <MoneyField name="taxRate" label="Tax rate (%)" max={100} />}
          {kind !== 'orders' && <Form.Item name="terms" label="Terms" className="master-form-wide"><Input.TextArea rows={3} /></Form.Item>}
          <Form.Item name="notes" label="Notes" className="master-form-wide"><Input.TextArea rows={2} /></Form.Item>
        </div>
        <Form.List name="items">
          {(fields, { add, remove }) => <div className="commercial-lines">
            <div className="line-heading"><strong>Items</strong><Button size="small" onClick={() => add()}>Add line</Button></div>
            {fields.map(({ key, name }) => <Space key={key} align="start" wrap className="commercial-line">
              <Form.Item name={[name, 'itemId']} label="Item" rules={[{ required: true }]} style={{ width: 260 }}>
                <Select showSearch optionFilterProp="label" options={selectableItems.map((item: any) => ({
                  value: item.itemId ?? item.id, label: `${item.itemCode} — ${item.itemName}`,
                }))} />
              </Form.Item>
              <Form.Item name={[name, 'quantity']} label="Quantity" rules={[{ required: true }]}><InputNumber min={0.0001} /></Form.Item>
              {kind !== 'orders' && <>
                <Form.Item name={[name, 'unitRate']} label="Unit rate" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
                <Form.Item name={[name, 'rentalRate']} label="Rental rate" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
              </>}
              <Button danger type="text" onClick={() => remove(name)} disabled={fields.length === 1}>Remove</Button>
            </Space>)}
          </div>}
        </Form.List>
      </Form>
    </Modal>
    <Details row={selected} kind={kind} onClose={() => setSelected(null)} />
    <Modal open={Boolean(convertQuotation)} title="Convert quotation to agreement" onCancel={() => setConvertQuotation(null)}
      onOk={() => convertForm.submit()} confirmLoading={convert.isPending}>
      <Form form={convertForm} layout="vertical" onFinish={(values) => convert.mutate(values)}>
        <Form.Item name="templateId" label="Template"><Select allowClear options={(templates.data ?? []).map((t) => ({ value: t.id, label: t.name }))} /></Form.Item>
        <Form.Item name="effectiveDate" label="Effective date" rules={[{ required: true }]}><Input type="date" /></Form.Item>
        <Form.Item name="expiryDate" label="Expiry date"><Input type="date" /></Form.Item>
        <MoneyField name="securityDeposit" label="Security deposit" />
        <Form.Item name="notes" label="Notes"><Input.TextArea /></Form.Item>
      </Form>
    </Modal>
    <Modal open={templateOpen} title="Upload agreement template" onCancel={() => setTemplateOpen(false)}
      onOk={() => templateForm.submit()} confirmLoading={uploadTemplate.isPending}>
      <Form form={templateForm} layout="vertical" onFinish={(values) => uploadTemplate.mutate(values)}>
        <Form.Item name="name" label="Template name" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="description" label="Description"><Input.TextArea /></Form.Item>
        <Form.Item name="file" label="DOCX or PDF" valuePropName="fileList" getValueFromEvent={(event) => event?.fileList}
          rules={[{ required: true }]}><Upload beforeUpload={() => false} maxCount={1} accept=".docx,.pdf"><Button icon={<UploadOutlined />}>Choose file</Button></Upload></Form.Item>
      </Form>
    </Modal>
  </>;
}

const CONFIG = {
  quotations: { title: 'Quotations', description: 'Prepare, approve and convert commercial offers.', numberKey: 'quotationNumber', numberLabel: 'Quotation', dateKey: 'quotationDate', dateLabel: 'Quotation date' },
  agreements: { title: 'Agreements', description: 'Create contractual drafts, generate documents and activate terms.', numberKey: 'agreementNumber', numberLabel: 'Agreement', dateKey: 'effectiveDate', dateLabel: 'Effective date' },
  orders: { title: 'Site orders', description: 'Authorise site quantities against active agreements.', numberKey: 'orderNumber', numberLabel: 'Order', dateKey: 'orderDate', dateLabel: 'Order date' },
};
const singular = (kind: Kind) => kind === 'quotations' ? 'Quotation' : kind === 'agreements' ? 'Agreement' : 'Order';
function MoneyField({ name, label, max }: { name: string; label: string; max?: number }) {
  return <Form.Item name={name} label={label} rules={[{ required: true }]}><InputNumber min={0} max={max} style={{ width: '100%' }} /></Form.Item>;
}
function TemplateTable({ data, loading }: { data: Row[]; loading: boolean }) {
  return <Card className="premium-card"><Table rowKey="id" dataSource={data} loading={loading} columns={[
    { title: 'Template', dataIndex: 'name' }, { title: 'File', dataIndex: 'originalFilename' },
    { title: 'Size', dataIndex: 'fileSize', render: (v: number) => `${Math.ceil(v / 1024)} KB` },
    { title: 'Status', dataIndex: 'active', render: (v: boolean) => <Tag color={v ? 'success' : 'default'}>{v ? 'Active' : 'Inactive'}</Tag> },
    { title: '', render: (_: unknown, row: Row) => <Button icon={<DownloadOutlined />} href={`/api/v1/agreement-templates/${row.id}/download`}>Download</Button> },
  ]} /></Card>;
}
function Details({ row, kind, onClose }: { row: Row | null; kind: Kind; onClose: () => void }) {
  return <Modal open={Boolean(row)} title={row?.[CONFIG[kind].numberKey]} footer={null} onCancel={onClose} width={800}>
    {row && <>
      <Descriptions bordered size="small" column={2} items={[
        { key: 'party', label: 'Party', children: row.partyName },
        { key: 'site', label: 'Site', children: row.siteName },
        { key: 'status', label: 'Status', children: <Tag color={statusColor[row.status]}>{row.status}</Tag> },
        { key: 'date', label: CONFIG[kind].dateLabel, children: row[CONFIG[kind].dateKey] },
        ...(kind === 'quotations' ? [{ key: 'total', label: 'Grand total', children: money(row.grandTotal) }] : []),
        { key: 'notes', label: 'Notes', children: row.notes || '—', span: 2 },
      ]} />
      <Table style={{ marginTop: 20 }} size="small" pagination={false} rowKey={(line) => line.id ?? line.itemId} dataSource={row.items} columns={[
        { title: 'Item', dataIndex: 'itemName' }, { title: 'Unit', dataIndex: 'unit' },
        { title: 'Quantity', render: (_: unknown, line: any) => line.quantity ?? line.agreedQuantity ?? line.orderedQuantity },
        ...(kind === 'orders' ? [
          { title: 'Issued', dataIndex: 'issuedQuantity' }, { title: 'Remaining', dataIndex: 'remainingQuantity' },
        ] : [{ title: 'Rental rate', dataIndex: 'rentalRate', render: money }]),
      ]} />
    </>}
  </Modal>;
}
