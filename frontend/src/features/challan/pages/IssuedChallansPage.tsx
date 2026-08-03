import { useState } from 'react';
import { App, Button, Card, Descriptions, Empty, Form, Input, InputNumber, Modal, Select, Space, Table } from 'antd';
import { DownloadOutlined, SearchOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import { ReportExcelButton } from '../../../components/ReportExcelButton';
import type { SiteOrder } from '../../order/types';
import type { IssuedChallan } from '../types';

type Page<T> = { content: T[]; totalElements: number };

type ChallanFormValue = {
  siteOrderId: number;
  dispatchDate: string;
  vehicleNumber?: string;
  driverName?: string;
  notes?: string;
  items: {
    itemId: number;
    itemCode: string;
    itemName: string;
    unit: string;
    remainingQuantity: number;
    quantity: number;
  }[];
};

export function IssuedChallansPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { message } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<IssuedChallan>();
  const [createOpen, setCreateOpen] = useState(false);

  const [form] = Form.useForm<ChallanFormValue>();
  const selectedOrderId = Form.useWatch('siteOrderId', form);

  // Queries
  const challans = useQuery({
    queryKey: ['issued-challans', search],
    queryFn: async () =>
      (
        await apiClient.get<Page<IssuedChallan>>('/challans/issued', {
          params: { search, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const pendingOrders = useQuery({
    queryKey: ['orders-pending-challan'],
    queryFn: async () =>
      (
        await apiClient.get<Page<SiteOrder>>('/orders', {
          params: { size: 100 },
        })
      ).data.content.filter(
        (o) => o.status === 'CONFIRMED' || o.status === 'PARTIALLY_FULFILLED'
      ),
  });

  const stockBalances = useQuery({
    queryKey: ['stock-balances-challan-lookup'],
    queryFn: async () =>
      (
        await apiClient.get<Page<any>>('/stock/balances', {
          params: { size: 500 },
        })
      ).data.content,
  });

  const currentOrder = pendingOrders.data?.find((o) => o.id === selectedOrderId);

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['issued-challans'] });
    void qc.invalidateQueries({ queryKey: ['orders-pending-challan'] });
    void qc.invalidateQueries({ queryKey: ['stock-balances-challan-lookup'] });
  };

  // Mutations
  const createChallan = useMutation({
    mutationFn: async (v: ChallanFormValue) => {
      // Filter out items with 0 quantity
      const payload = {
        siteOrderId: v.siteOrderId,
        dispatchDate: v.dispatchDate,
        vehicleNumber: v.vehicleNumber,
        driverName: v.driverName,
        notes: v.notes,
        items: v.items
          .filter((i) => i.quantity > 0)
          .map((i) => ({ itemId: i.itemId, quantity: i.quantity })),
      };
      return (await apiClient.post<IssuedChallan>('/challans/issued', payload)).data;
    },
    onSuccess: (c) => {
      message.success(`Challan ${c.challanNumber} generated successfully`);
      setCreateOpen(false);
      form.resetFields();
      refresh();
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message ?? 'Unable to generate challan';
      message.error(msg);
    },
  });

  const handleDownloadPdf = async (c: IssuedChallan) => {
    try {
      const res = await apiClient.get(`/challans/issued/${c.id}/pdf`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `challan-${c.challanNumber.replaceAll('/', '-')}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Failed to download challan PDF');
    }
  };

  const handleOrderChange = (orderId: number) => {
    const order = pendingOrders.data?.find((o) => o.id === orderId);
    if (order) {
      const items = order.items
        .filter((i) => i.remainingQuantity > 0)
        .map((i) => ({
          itemId: i.itemId,
          itemCode: i.itemCode,
          itemName: i.itemName,
          unit: i.unit,
          remainingQuantity: i.remainingQuantity,
          quantity: i.remainingQuantity,
        }));
      form.setFieldsValue({ items });
    }
  };

  // Columns for main list
  const columns = [
    { title: 'Challan Number', dataIndex: 'challanNumber', key: 'challanNumber' },
    { title: 'Dispatch Date', dataIndex: 'dispatchDate', key: 'dispatchDate' },
    { title: 'Order Number', dataIndex: 'siteOrderNumber', key: 'siteOrderNumber' },
    { title: 'Party', dataIndex: 'partyName', key: 'partyName' },
    { title: 'Site', dataIndex: 'siteName', key: 'siteName' },
    { title: 'Vehicle', dataIndex: 'vehicleNumber', key: 'vehicleNumber', render: (v?: string) => v ?? '-' },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_: any, c: IssuedChallan) => (
        <ChallanActions
          challan={c}
          onView={() => setSelected(c)}
          onPdf={() => handleDownloadPdf(c)}
        />
      ),
    },
  ];

  return (
    <div className="page-stack" data-testid="issued-challans-page">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Issued Challans</h1>
          <p className="page-description">
            Track dispatches and outgoing material transfers from godown to sites.
          </p>
        </div>
        <Space wrap>
          <ReportExcelButton reportType="ISSUED_CHALLANS_REGISTER" filters={{ documentNumber: search || undefined }} />
          {canWrite && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              form.setFieldsValue({
                dispatchDate: new Date().toISOString().slice(0, 10),
              });
              setCreateOpen(true);
            }}
          >
            Issue Challan
          </Button>
          )}
        </Space>
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search challans"
            allowClear
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 250 }}
          />
        </Space>

        <Table
          rowKey="id"
          loading={challans.isLoading}
          dataSource={challans.data?.content}
          columns={columns}
          pagination={{ hideOnSinglePage: true }}
        />
      </Card>

      {/* Issue Challan Form Drawer */}
      <FormDrawer
        title="New Outward Dispatch (Issued Challan)"
        open={createOpen}
        width={750}
        onClose={() => {
          setCreateOpen(false);
          form.resetFields();
        }}
        onSubmit={() => form.submit()}
        loading={createChallan.isPending}
        okText="Generate Challan"
      >
        <Form form={form} layout="vertical" onFinish={(v) => createChallan.mutate(v)}>
          <Form.Item
            name="siteOrderId"
            label="Select Confirmed Site Order"
            rules={[{ required: true, message: 'Select site order' }]}
          >
            <Select
              placeholder="Select order to dispatch items"
              onChange={handleOrderChange}
              options={pendingOrders.data?.map((o) => ({
                value: o.id,
                label: `${o.orderNumber} — ${o.partyName} (${o.siteName})`,
              }))}
            />
          </Form.Item>

          {currentOrder && (
            <Card size="small" style={{ marginBottom: 16, background: '#fafafa' }}>
              <div>
                <strong>Party: </strong> {currentOrder.partyName}
              </div>
              <div style={{ marginTop: 4 }}>
                <strong>Site: </strong> {currentOrder.siteName}
              </div>
            </Card>
          )}

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item
              name="dispatchDate"
              label="Dispatch Date"
              rules={[{ required: true, message: 'Required' }]}
              style={{ width: 200 }}
            >
              <Input type="date" />
            </Form.Item>
            <Form.Item name="vehicleNumber" label="Vehicle Number" style={{ width: 220 }}>
              <Input placeholder="e.g. MH-12-PQ-1234" />
            </Form.Item>
            <Form.Item name="driverName" label="Driver Name" style={{ width: 220 }}>
              <Input placeholder="Driver full name" />
            </Form.Item>
          </Space>

          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={2} placeholder="Loading remarks or delivery instructions" />
          </Form.Item>

          <div style={{ marginBottom: 8, fontWeight: 'bold' }}>Dispatch Items</div>
          <Form.List name="items">
            {(fields) => {
              if (!fields.length) return <Empty description="Select an order to load items" />;
              return (
                <Table
                  dataSource={fields}
                  pagination={false}
                  rowKey="key"
                  size="small"
                  columns={[
                    {
                      title: 'Item',
                      render: (_, field) => {
                        const item = form.getFieldValue(['items', field.name]);
                        return `${item?.itemCode} — ${item?.itemName}`;
                      },
                    },
                    {
                      title: 'Unit',
                      render: (_, field) => form.getFieldValue(['items', field.name])?.unit,
                    },
                    {
                      title: 'Order Remaining',
                      render: (_, field) => form.getFieldValue(['items', field.name])?.remainingQuantity,
                    },
                    {
                      title: 'Godown Stock',
                      render: (_, field) => {
                        const item = form.getFieldValue(['items', field.name]);
                        const balance = stockBalances.data?.find((b) => b.itemId === item?.itemId);
                        return balance?.availableQuantity ?? 0;
                      },
                    },
                    {
                      title: 'Dispatch Qty',
                      width: 140,
                      render: (_, field) => (
                        <Form.Item
                          name={[field.name, 'quantity']}
                          rules={[
                            { required: true, message: 'Required' },
                            { type: 'number', min: 0, message: 'Min 0' },
                            ({ getFieldValue }) => ({
                              validator(_, value) {
                                if (value === undefined) return Promise.resolve();
                                const item = getFieldValue(['items', field.name]);
                                if (value > item.remainingQuantity) {
                                  return Promise.reject(
                                    new Error(`Max: ${item.remainingQuantity}`)
                                  );
                                }
                                const balance = stockBalances.data?.find(
                                  (b) => b.itemId === item?.itemId
                                );
                                const available = balance?.availableQuantity ?? 0;
                                if (value > available) {
                                  return Promise.reject(new Error(`Stock: ${available}`));
                                }
                                return Promise.resolve();
                              },
                            }),
                          ]}
                          noStyle
                        >
                          <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                      ),
                    },
                  ]}
                />
              );
            }}
          </Form.List>
        </Form>
      </FormDrawer>

      {/* Details Modal */}
      <Modal
        title={`Challan Detail: ${selected?.challanNumber ?? ''}`}
        open={!!selected}
        width={800}
        footer={null}
        onCancel={() => setSelected(undefined)}
      >
        {selected && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Order Number">
                {selected.siteOrderNumber}
              </Descriptions.Item>
              <Descriptions.Item label="Dispatch Date">
                {selected.dispatchDate}
              </Descriptions.Item>
              <Descriptions.Item label="Party">{selected.partyName}</Descriptions.Item>
              <Descriptions.Item label="Site">{selected.siteName}</Descriptions.Item>
              <Descriptions.Item label="Vehicle">
                {selected.vehicleNumber ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Driver">
                {selected.driverName ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Created By">
                {selected.createdBy}
              </Descriptions.Item>
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
                { title: 'Dispatched Qty', dataIndex: 'quantity' },
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

export function ChallanActions({
  challan,
  onView,
  onPdf,
}: {
  challan: IssuedChallan;
  onView: () => void;
  onPdf: () => void;
}) {
  return (
    <Space wrap>
      <Button size="small" onClick={onView}>
        View
      </Button>
      <Button size="small" icon={<DownloadOutlined />} onClick={onPdf}>
        PDF
      </Button>
      <ReportExcelButton size="small" reportType="ISSUED_CHALLANS_REGISTER" filters={{ documentNumber: challan.challanNumber }} />
    </Space>
  );
}
