import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, DatePicker, Form, Input, InputNumber, Select, Space, Table, Tabs, Tag, message } from 'antd';
import { ArrowDownOutlined, DeleteOutlined, HistoryOutlined, PlusOutlined, RetweetOutlined, ToolOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import { FormDrawer } from '../../../components/FormDrawer';
import { ReportExcelButton } from '../../../components/ReportExcelButton';
import { useAuth } from '../../auth/context/AuthContext';

interface PageResponse<T> { content: T[]; totalElements: number }
interface ItemOption { id: number; itemCode: string; itemName: string; unit: string }
interface VendorOption { id: number; name: string }
interface Balance {
  itemId: number; itemCode: string; itemName: string; categoryName: string; unit: string;
  availableQuantity: number; issuedQuantity: number; hiredQuantity: number; lostQuantity: number;
  scrappedQuantity: number; availableWeight: number; minimumStock: number; belowMinimum: boolean;
}
interface Transaction {
  id: number; itemCode: string; itemName: string; transactionType: string; transactionDate: string;
  quantity: number; direction: string; sourceType: string; sourceId: number; createdBy: string;
}
type PostingType = 'purchase' | 'scrap' | 'adjustment';

export function InventoryPage() {
  const { user } = useAuth();
  const canPost = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS') ?? false;
  const [form] = Form.useForm();
  const [posting, setPosting] = useState<PostingType>();
  const [balancePage, setBalancePage] = useState(1);
  const [historyPage, setHistoryPage] = useState(1);
  const [search, setSearch] = useState('');
  const queryClient = useQueryClient();

  const items = useQuery({
    queryKey: ['inventory-item-options'],
    queryFn: async () => (await apiClient.get<PageResponse<ItemOption>>('/items', { params: { size: 500, active: true } })).data.content,
  });
  const vendors = useQuery({
    queryKey: ['inventory-vendor-options'],
    queryFn: async () => (await apiClient.get<PageResponse<VendorOption>>('/vendors', { params: { size: 500, active: true } })).data.content,
  });
  const balances = useQuery({
    queryKey: ['stock-balances', balancePage, search],
    queryFn: async () => (await apiClient.get<PageResponse<Balance>>('/stock/balances', {
      params: { page: balancePage - 1, size: 10, search, sort: 'item.itemCode,asc' },
    })).data,
  });
  const history = useQuery({
    queryKey: ['stock-history', historyPage],
    queryFn: async () => (await apiClient.get<PageResponse<Transaction>>('/stock/transactions', {
      params: { page: historyPage - 1, size: 10, sort: 'createdAt,desc' },
    })).data,
  });
  const submit = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      const lines = (values.items as { itemId: number; quantity: number; unitRate?: number }[]);
      const date = (values.date as dayjs.Dayjs).format('YYYY-MM-DD');
      const headers = { 'Idempotency-Key': crypto.randomUUID() };
      if (posting === 'purchase') {
        return apiClient.post('/stock/purchases', {
          vendorId: values.vendorId, purchaseDate: date, notes: values.notes, items: lines,
        }, { headers });
      }
      if (posting === 'scrap') {
        return apiClient.post('/stock/scrap', { date, reason: values.reason, notes: values.notes, items: lines }, { headers });
      }
      return apiClient.post('/stock/adjustments', {
        date, direction: values.direction, reason: values.reason, notes: values.notes, items: lines,
      }, { headers });
    },
    onSuccess: (response) => {
      message.success(`${response.data.number} posted successfully`); setPosting(undefined); form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['stock-balances'] });
      queryClient.invalidateQueries({ queryKey: ['stock-history'] });
      queryClient.invalidateQueries({ queryKey: ['stock-summary'] });
    },
    onError: (error: unknown) => {
      const response = error as { response?: { data?: { message?: string } } };
      message.error(response.response?.data?.message ?? 'Unable to post stock transaction');
    },
  });

  const openPosting = (type: PostingType) => {
    form.resetFields(); form.setFieldsValue({ date: dayjs(), direction: 'IN', items: [{}] }); setPosting(type);
  };
  const itemOptions = (items.data ?? []).map((item) => ({ value: item.id, label: `${item.itemCode} · ${item.itemName} (${item.unit})` }));

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div><h1 className="page-heading">Inventory</h1><p className="page-description">Live stock projection backed by an immutable movement ledger.</p></div>
        <Space wrap>
          <ReportExcelButton reportType="CURRENT_STOCK_SUMMARY" />
          {canPost && <>
            <Button icon={<ArrowDownOutlined />} onClick={() => openPosting('purchase')}>Purchase</Button>
            <Button icon={<ToolOutlined />} onClick={() => openPosting('scrap')}>Scrap</Button>
            <Button type="primary" icon={<RetweetOutlined />} onClick={() => openPosting('adjustment')}>Adjustment</Button>
          </>}
        </Space>
      </div>
      <Card className="premium-card">
        <Tabs items={[
          { key: 'balance', label: 'Stock balance', children: <>
            <div className="filters-bar"><Input.Search allowClear placeholder="Search item code or name" value={search}
              onChange={(event) => { setSearch(event.target.value); setBalancePage(1); }} style={{ width: 320 }} /></div>
            <Table rowKey="itemId" dataSource={balances.data?.content} loading={balances.isLoading} scroll={{ x: 1000 }}
              columns={[
                { title: 'Code', dataIndex: 'itemCode' }, { title: 'Item', dataIndex: 'itemName' },
                { title: 'Category', dataIndex: 'categoryName' }, { title: 'Available', dataIndex: 'availableQuantity',
                  render: (value, row: Balance) => <strong className={row.belowMinimum ? 'stock-low' : ''}>{value} {row.unit}</strong> },
                { title: 'Available weight', dataIndex: 'availableWeight' }, { title: 'Issued', dataIndex: 'issuedQuantity' },
                { title: 'Hired', dataIndex: 'hiredQuantity' }, { title: 'Lost', dataIndex: 'lostQuantity' },
                { title: 'Scrapped', dataIndex: 'scrappedQuantity' },
                { title: 'Threshold', key: 'threshold', render: (_, row: Balance) =>
                  row.belowMinimum ? <Tag color="warning">Below minimum</Tag> : <Tag color="success">Healthy</Tag> },
              ]} pagination={{ current: balancePage, pageSize: 10, total: balances.data?.totalElements,
                onChange: setBalancePage, showSizeChanger: false }} />
          </> },
          { key: 'history', label: <span><HistoryOutlined /> Movement history</span>, children:
            <Table rowKey="id" dataSource={history.data?.content} loading={history.isLoading} scroll={{ x: 900 }}
              columns={[
                { title: 'Date', dataIndex: 'transactionDate' }, { title: 'Item', key: 'item', render: (_, row: Transaction) => `${row.itemCode} · ${row.itemName}` },
                { title: 'Movement', dataIndex: 'transactionType', render: (value) => <Tag className="role-tag">{value.replaceAll('_', ' ')}</Tag> },
                { title: 'Direction', dataIndex: 'direction', render: (value) => <Tag color={value === 'IN' ? 'success' : 'error'}>{value}</Tag> },
                { title: 'Quantity', dataIndex: 'quantity' }, { title: 'Source', key: 'source', render: (_, row: Transaction) => `${row.sourceType} #${row.sourceId}` },
                { title: 'Posted by', dataIndex: 'createdBy' },
              ]} pagination={{ current: historyPage, pageSize: 10, total: history.data?.totalElements,
                onChange: setHistoryPage, showSizeChanger: false }} /> },
        ]} />
      </Card>
      <FormDrawer open={Boolean(posting)} title={posting ? `Post ${posting}` : ''} subtitle="Record a controlled inventory transaction in the immutable movement ledger." width={760} onClose={() => setPosting(undefined)}
        onSubmit={() => form.submit()} loading={submit.isPending} okText="Post transaction">
        <Form form={form} layout="vertical" onFinish={(values) => submit.mutate(values)} style={{ marginTop: 20 }}>
          <div className="master-form-grid">
            {posting === 'purchase' && <Form.Item name="vendorId" label="Vendor" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={(vendors.data ?? []).map((v) => ({ value: v.id, label: v.name }))} />
            </Form.Item>}
            <Form.Item name="date" label="Transaction date" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
            {posting === 'adjustment' && <Form.Item name="direction" label="Direction" rules={[{ required: true }]}>
              <Select options={[{ value: 'IN', label: 'Adjustment in' }, { value: 'OUT', label: 'Adjustment out' }]} />
            </Form.Item>}
            {posting !== 'purchase' && <Form.Item name="reason" label="Reason" rules={[{ required: true }]} className="master-form-wide"><Input /></Form.Item>}
            <Form.Item name="notes" label="Notes" className="master-form-wide"><Input.TextArea rows={2} /></Form.Item>
          </div>
          <Form.List name="items">
            {(fields, { add, remove }) => <>
              <div className="line-list-header"><strong>Items</strong><Button type="dashed" icon={<PlusOutlined />} onClick={() => add()}>Add line</Button></div>
              {fields.map((field) => <div className="stock-line" key={field.key}>
                <Form.Item {...field} name={[field.name, 'itemId']} rules={[{ required: true, message: 'Select an item' }]}>
                  <Select showSearch optionFilterProp="label" placeholder="Item" options={itemOptions} />
                </Form.Item>
                <Form.Item {...field} name={[field.name, 'quantity']} rules={[{ required: true, message: 'Enter quantity' }]}>
                  <InputNumber min={0.0001} placeholder="Quantity" style={{ width: '100%' }} />
                </Form.Item>
                {posting === 'purchase' && <Form.Item {...field} name={[field.name, 'unitRate']} rules={[{ required: true, message: 'Enter rate' }]}>
                  <InputNumber min={0} prefix="₹" placeholder="Unit rate" style={{ width: '100%' }} />
                </Form.Item>}
                <Button danger type="text" icon={<DeleteOutlined />} disabled={fields.length === 1} onClick={() => remove(field.name)} />
              </div>)}
            </>}
          </Form.List>
        </Form>
      </FormDrawer>
    </div>
  );
}
