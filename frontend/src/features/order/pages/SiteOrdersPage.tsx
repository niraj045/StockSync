import { useState } from 'react';
import { App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Select, Space, Table, Tag } from 'antd';
import { DownloadOutlined, SearchOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import { ReportExcelButton } from '../../../components/ReportExcelButton';
import type { Agreement } from '../../agreement/types';
import type { SiteOrder } from '../types';

type Page<T> = { content: T[]; totalElements: number };

type OrderFormValue = {
  agreementId: number;
  orderDate: string;
  notes?: string;
  items: {
    itemId: number;
    orderedQuantity: number;
  }[];
};

export function SiteOrdersPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { modal, message } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<string>();
  const [siteId, setSiteId] = useState<number>();
  const [selected, setSelected] = useState<SiteOrder>();
  const [editing, setEditing] = useState<SiteOrder | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  const [form] = Form.useForm<OrderFormValue>();
  const selectedAgreementId = Form.useWatch('agreementId', form);

  // Queries
  const orders = useQuery({
    queryKey: ['orders', search, status, siteId],
    queryFn: async () =>
      (
        await apiClient.get<Page<SiteOrder>>('/orders', {
          params: { search, status, siteId, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const activeAgreements = useQuery({
    queryKey: ['agreements-active-options'],
    queryFn: async () =>
      (
        await apiClient.get<Page<Agreement>>('/agreements', {
          params: { status: 'ACTIVE', size: 100 },
        })
      ).data.content,
  });

  const sites = useQuery({
    queryKey: ['sites-options'],
    queryFn: async () =>
      (await apiClient.get<{ content: any[] }>('/sites', { params: { size: 300 } })).data.content,
  });

  // Find selected agreement to display details & filter items
  const currentAgreement = activeAgreements.data?.find(
    (a) => a.id === selectedAgreementId
  );

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['orders'] });
  };

  // Mutations
  const createOrder = useMutation({
    mutationFn: async (v: OrderFormValue) =>
      (await apiClient.post<SiteOrder>('/orders', v)).data,
    onSuccess: (o) => {
      message.success(`Order ${o.orderNumber} created`);
      setCreateOpen(false);
      form.resetFields();
      refresh();
    },
    onError: () => message.error('Unable to create order'),
  });

  const updateOrder = useMutation({
    mutationFn: async (v: OrderFormValue) =>
      (
        await apiClient.put<SiteOrder>(`/orders/${editing?.id}`, {
          ...v,
          version: editing?.version,
        })
      ).data,
    onSuccess: (o) => {
      message.success(`Order ${o.orderNumber} updated`);
      setEditing(null);
      form.resetFields();
      refresh();
    },
    onError: () => message.error('Unable to update order'),
  });

  const confirmOrder = useMutation({
    mutationFn: async (id: number) =>
      (await apiClient.post<SiteOrder>(`/orders/${id}/confirm`)).data,
    onSuccess: (o) => {
      message.success(`Order ${o.orderNumber} confirmed`);
      if (selected?.id === o.id) setSelected(o);
      refresh();
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message ?? 'Unable to confirm order';
      message.error(msg);
    },
  });

  const cancelOrder = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason?: string }) =>
      (await apiClient.post<SiteOrder>(`/orders/${id}/cancel`, { reason })).data,
    onSuccess: (o) => {
      message.success(`Order ${o.orderNumber} cancelled`);
      if (selected?.id === o.id) setSelected(o);
      refresh();
    },
    onError: () => message.error('Unable to cancel order'),
  });

  // Action dialogs
  const handleConfirm = (o: SiteOrder) => {
    modal.confirm({
      title: `Confirm Order ${o.orderNumber}?`,
      content: 'This will lock the ordered quantities. This action is irreversible.',
      onOk: () => confirmOrder.mutateAsync(o.id),
    });
  };

  const handleCancel = (o: SiteOrder) => {
    let reason = '';
    modal.confirm({
      title: `Cancel Order ${o.orderNumber}?`,
      content: (
        <Input.TextArea
          data-testid="cancel-reason"
          placeholder="Reason is required"
          style={{ marginTop: 12 }}
          onChange={(e) => (reason = e.target.value)}
        />
      ),
      onOk: () => {
        if (!reason.trim()) {
          message.error('Reason is required');
          return Promise.reject();
        }
        return cancelOrder.mutateAsync({ id: o.id, reason });
      },
    });
  };

  const handleDownloadPdf = async (o: SiteOrder) => {
    try {
      const res = await apiClient.get(`/orders/${o.id}/pdf`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `order-${o.orderNumber.replaceAll('/', '-')}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Failed to download order PDF');
    }
  };

  const handleEdit = (o: SiteOrder) => {
    setEditing(o);
    form.setFieldsValue({
      agreementId: o.agreementId,
      orderDate: o.orderDate,
      notes: o.notes,
      items: o.items.map((i) => ({
        itemId: i.itemId,
        orderedQuantity: i.orderedQuantity,
      })),
    });
  };

  // Columns for main table
  const columns = [
    { title: 'Order Number', dataIndex: 'orderNumber', key: 'orderNumber' },
    { title: 'Agreement', dataIndex: 'agreementNumber', key: 'agreementNumber' },
    { title: 'Party', dataIndex: 'partyName', key: 'partyName' },
    { title: 'Site', dataIndex: 'siteName', key: 'siteName' },
    { title: 'Order Date', dataIndex: 'orderDate', key: 'orderDate' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => {
        let color = 'default';
        if (s === 'CONFIRMED') color = 'blue';
        if (s === 'PARTIALLY_FULFILLED') color = 'orange';
        if (s === 'FULFILLED') color = 'green';
        if (s === 'CANCELLED') color = 'red';
        return <Tag color={color}>{s.replaceAll('_', ' ')}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 180,
      render: (_: any, o: SiteOrder) => (
        <OrderActions
          order={o}
          canWrite={canWrite}
          onView={() => setSelected(o)}
          onEdit={() => handleEdit(o)}
          onConfirm={() => handleConfirm(o)}
          onCancel={() => handleCancel(o)}
          onPdf={() => handleDownloadPdf(o)}
        />
      ),
    },
  ];

  return (
    <div className="page-stack" data-testid="orders-page">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Site Orders</h1>
          <p className="page-description">
            Track material orders placed under active site rental agreements.
          </p>
        </div>
        <Space wrap>
          <ReportExcelButton reportType="SITE_ORDERS_REGISTER" filters={{ status, siteId, documentNumber: search || undefined }} />
          {canWrite && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              form.setFieldsValue({
                orderDate: new Date().toISOString().slice(0, 10),
                items: [{} as any],
              });
              setCreateOpen(true);
            }}
          >
            Create order
          </Button>
          )}
        </Space>
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search orders"
            allowClear
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 250 }}
          />
          <Select
            placeholder="All statuses"
            allowClear
            onChange={setStatus}
            options={['DRAFT', 'CONFIRMED', 'PARTIALLY_FULFILLED', 'FULFILLED', 'CANCELLED'].map((s) => ({
              value: s,
              label: s.replaceAll('_', ' '),
            }))}
            style={{ width: 180 }}
          />
          <Select
            placeholder="All sites"
            allowClear
            showSearch
            optionFilterProp="label"
            value={siteId}
            onChange={setSiteId}
            options={sites.data?.map((s) => ({ value: s.id, label: s.siteName }))}
            style={{ width: 220 }}
          />
        </Space>

        <Table
          rowKey="id"
          loading={orders.isLoading}
          dataSource={orders.data?.content}
          columns={columns}
          pagination={{ hideOnSinglePage: true }}
        />
      </Card>

      {/* Create Order Drawer */}
      <FormDrawer
        title="Create Site Order"
        open={createOpen}
        width={600}
        onClose={() => {
          setCreateOpen(false);
          form.resetFields();
        }}
        onSubmit={() => form.submit()}
        loading={createOrder.isPending}
        okText="Create order"
      >
        <OrderForm
          form={form}
          activeAgreements={activeAgreements.data ?? []}
          currentAgreement={currentAgreement}
          onSubmit={(v) => createOrder.mutate(v)}
        />
      </FormDrawer>

      {/* Edit Order Drawer */}
      <FormDrawer
        title={`Edit Order ${editing?.orderNumber ?? ''}`}
        open={!!editing}
        width={600}
        onClose={() => {
          setEditing(null);
          form.resetFields();
        }}
        onSubmit={() => form.submit()}
        loading={updateOrder.isPending}
        okText="Save changes"
      >
        <OrderForm
          form={form}
          activeAgreements={activeAgreements.data ?? []}
          currentAgreement={currentAgreement}
          onSubmit={(v) => updateOrder.mutate(v)}
        />
      </FormDrawer>

      {/* View Detail Modal */}
      <Modal
        title={selected?.orderNumber}
        open={!!selected}
        width={800}
        footer={null}
        onCancel={() => setSelected(undefined)}
      >
        {selected && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Agreement">
                {selected.agreementNumber}
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selected.status === 'CONFIRMED' ? 'blue' : 'default'}>
                  {selected.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Party">{selected.partyName}</Descriptions.Item>
              <Descriptions.Item label="Site">{selected.siteName}</Descriptions.Item>
              <Descriptions.Item label="Order Date">{selected.orderDate}</Descriptions.Item>
              <Descriptions.Item label="Created At">
                {new Date(selected.createdAt).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>

            <Table
              rowKey="id"
              dataSource={selected.items}
              pagination={false}
              size="small"
              columns={[
                { title: 'Code', dataIndex: 'itemCode' },
                { title: 'Item', dataIndex: 'itemName' },
                { title: 'Unit', dataIndex: 'unit' },
                { title: 'Ordered', dataIndex: 'orderedQuantity' },
                { title: 'Issued', dataIndex: 'issuedQuantity' },
                { title: 'Remaining', dataIndex: 'remainingQuantity' },
              ]}
            />
            {selected.notes && (
              <div style={{ marginTop: 16 }}>
                <strong>Notes:</strong>
                <p>{selected.notes}</p>
              </div>
            )}
            <Space style={{ marginTop: 16 }}>
              <Button icon={<DownloadOutlined />} onClick={() => handleDownloadPdf(selected)}>
                Download PDF
              </Button>
            </Space>
          </>
        )}
      </Modal>
    </div>
  );
}

function OrderForm({
  form,
  activeAgreements,
  currentAgreement,
  onSubmit,
}: {
  form: any;
  activeAgreements: Agreement[];
  currentAgreement?: Agreement;
  onSubmit: (v: OrderFormValue) => void;
}) {
  return (
    <Form form={form} layout="vertical" onFinish={onSubmit}>
      <Form.Item
        name="agreementId"
        label="Select Agreement"
        rules={[{ required: true, message: 'Please select an agreement' }]}
      >
        <Select
          placeholder="Select an active agreement"
          options={activeAgreements.map((a) => ({
            value: a.id,
            label: `${a.agreementNumber} — ${a.partyName} — ${a.siteCode}`,
          }))}
        />
      </Form.Item>

      {currentAgreement && (
        <Card size="small" style={{ marginBottom: 16, background: '#fafafa' }}>
          <div>
            <strong>Party: </strong> {currentAgreement.partyName}
          </div>
          <div style={{ marginTop: 4 }}>
            <strong>Site: </strong> {currentAgreement.siteName} ({currentAgreement.siteCode})
          </div>
        </Card>
      )}

      <Form.Item
        name="orderDate"
        label="Order Date"
        rules={[{ required: true, message: 'Please select order date' }]}
      >
        <Input type="date" />
      </Form.Item>

      <Form.Item name="notes" label="Notes">
        <Input.TextArea rows={2} placeholder="Dispatch instructions or notes" />
      </Form.Item>

      <div style={{ marginBottom: 8, fontWeight: 'bold' }}>Order Items</div>
      <Form.List name="items">
        {(fields, { add, remove }) => (
          <>
            {fields.map(({ key, name, ...restField }) => (
              <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                <Form.Item
                  {...restField}
                  name={[name, 'itemId']}
                  rules={[{ required: true, message: 'Select item' }]}
                  style={{ width: 280 }}
                >
                  <Select
                    placeholder="Select item"
                    disabled={!currentAgreement}
                    options={(currentAgreement?.items ?? []).map((i) => ({
                      value: i.itemId,
                      label: `${i.itemCode} — ${i.itemName} (Limit: ${i.contractedQuantity} ${i.unit})`,
                    }))}
                  />
                </Form.Item>
                <Form.Item
                  {...restField}
                  name={[name, 'orderedQuantity']}
                  rules={[
                    { required: true, message: 'Qty required' },
                    { type: 'number', min: 0.0001, message: 'Must be > 0' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!currentAgreement || value === undefined) return Promise.resolve();
                        const selectedItemId = getFieldValue(['items', name, 'itemId']);
                        const agreementItem = currentAgreement.items.find(
                          (i) => i.itemId === selectedItemId
                        );
                        if (agreementItem && value > agreementItem.contractedQuantity) {
                          return Promise.reject(
                            new Error(`Exceeds limit of ${agreementItem.contractedQuantity}`)
                          );
                        }
                        return Promise.resolve();
                      },
                    }),
                  ]}
                >
                  <InputNumber placeholder="Qty" style={{ width: 120 }} min={0.0001} />
                </Form.Item>
                {fields.length > 1 && <Button danger onClick={() => remove(name)}>Remove</Button>}
              </Space>
            ))}
            <Form.Item>
              <Button type="dashed" onClick={() => add()} block disabled={!currentAgreement}>
                Add Item
              </Button>
            </Form.Item>
          </>
        )}
      </Form.List>
    </Form>
  );
}

export function OrderActions({
  order: o,
  canWrite,
  onView,
  onEdit,
  onConfirm,
  onCancel,
  onPdf,
}: {
  order: SiteOrder;
  canWrite: boolean;
  onView: () => void;
  onEdit: () => void;
  onConfirm: () => void;
  onCancel: () => void;
  onPdf: () => void;
}) {
  return (
    <Space wrap>
      <Button size="small" onClick={onView}>
        View
      </Button>
      {canWrite && o.status === 'DRAFT' && (
        <Button size="small" onClick={onEdit}>
          Edit
        </Button>
      )}
      {canWrite && o.status === 'DRAFT' && (
        <Button size="small" type="primary" onClick={onConfirm}>
          Confirm
        </Button>
      )}
      {canWrite && (o.status === 'DRAFT' || o.status === 'CONFIRMED') && (
        <Button size="small" danger onClick={onCancel}>
          Cancel
        </Button>
      )}
      <Button size="small" icon={<DownloadOutlined />} onClick={onPdf}>
        PDF
      </Button>
      <ReportExcelButton size="small" reportType="SITE_ORDERS_REGISTER" filters={{ documentNumber: o.orderNumber }} />
    </Space>
  );
}
