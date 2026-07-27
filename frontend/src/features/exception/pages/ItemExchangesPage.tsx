import { useState } from 'react';
import { App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Select, Space, Table, Tag } from 'antd';
import { SearchOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import type { Agreement } from '../../agreement/types';

type Page<T> = { content: T[]; totalElements: number };

export interface ItemExchange {
  id: number;
  exchangeNumber: string;
  sourceType: string;
  sourceReceivingChallanId?: number;
  sourceReceivingChallanNumber?: string;
  sourceReceivingChallanItemId?: number;
  agreementId: number;
  agreementNumber: string;
  partyId: number;
  partyName: string;
  siteId: number;
  siteName: string;
  expectedItemId: number;
  expectedItemCode: string;
  expectedItemName: string;
  actualItemId: number;
  actualItemCode: string;
  actualItemName: string;
  exchangeDate: string;
  expectedQuantity: number;
  actualQuantity: number;
  expectedWeight: number;
  actualWeight: number;
  destinationStockStatus: string;
  reason?: string;
  status: string;
  postedAt?: string;
  postedBy?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancellationReason?: string;
  createdAt: string;
  createdBy: string;
  version: number;
}

type ExchangeFormValue = {
  agreementId: number;
  expectedItemId: number;
  actualItemId: number;
  exchangeDate: string;
  expectedQuantity: number;
  actualQuantity: number;
  expectedWeight: number;
  actualWeight: number;
  destinationStockStatus: string;
  reason?: string;
};

export function ItemExchangesPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { message, modal } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<ItemExchange>();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<ItemExchange | null>(null);

  const [form] = Form.useForm<ExchangeFormValue>();
  const selectedAgreementId = Form.useWatch('agreementId', form);
  const selectedExpectedItemId = Form.useWatch('expectedItemId', form);
  const selectedActualItemId = Form.useWatch('actualItemId', form);

  // Queries
  const exchanges = useQuery({
    queryKey: ['item-exchanges', search],
    queryFn: async () =>
      (
        await apiClient.get<Page<ItemExchange>>('/item-exchanges', {
          params: { search, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const activeAgreements = useQuery({
    queryKey: ['agreements-active-exchanges'],
    queryFn: async () =>
      (
        await apiClient.get<Page<Agreement>>('/agreements', {
          params: { status: 'ACTIVE', size: 100 },
        })
      ).data.content,
  });

  const currentAgreement = activeAgreements.data?.find((a) => a.id === selectedAgreementId);

  const itemsQuery = useQuery({
    queryKey: ['all-items-lookup'],
    queryFn: async () =>
      (
        await apiClient.get<Page<any>>('/items', {
          params: { size: 500 },
        })
      ).data.content,
  });

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['item-exchanges'] });
  };

  const handleAgreementChange = (_agreementId: number) => {
    form.setFieldsValue({ expectedItemId: undefined as any, actualItemId: undefined as any });
  };

  const handleExpectedItemChange = (itemId: number) => {
    const agItem = currentAgreement?.items.find((i) => i.itemId === itemId);
    if (agItem) {
      form.setFieldsValue({
        expectedWeight: (form.getFieldValue('expectedQuantity') || 0) * (agItem.weight || 0),
      });
    }
  };

  const handleActualItemChange = (itemId: number) => {
    const matchedItem = itemsQuery.data?.find((i) => i.id === itemId);
    if (matchedItem) {
      form.setFieldsValue({
        actualWeight: (form.getFieldValue('actualQuantity') || 0) * (matchedItem.weight || 0),
      });
    }
  };

  // Mutations
  const saveExchange = useMutation({
    mutationFn: async (v: ExchangeFormValue) => {
      const payload = {
        ...v,
        partyId: currentAgreement?.partyId,
        siteId: currentAgreement?.siteId,
      };
      if (editing) {
        return (await apiClient.put<ItemExchange>(`/item-exchanges/${editing.id}`, payload)).data;
      } else {
        return (await apiClient.post<ItemExchange>('/item-exchanges', payload)).data;
      }
    },
    onSuccess: (res) => {
      message.success(`Exchange record ${res.exchangeNumber} saved`);
      setCreateOpen(false);
      setEditing(null);
      form.resetFields();
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to save record');
    },
  });

  const postExchange = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<ItemExchange>(`/item-exchanges/${id}/post`)).data;
    },
    onSuccess: (res) => {
      message.success(`Exchange record ${res.exchangeNumber} posted to stock ledger`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Posting failed');
    },
  });

  const cancelExchange = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) => {
      return (await apiClient.post<ItemExchange>(`/item-exchanges/${id}/cancel`, { cancellationReason: reason })).data;
    },
    onSuccess: (res) => {
      message.success(`Exchange record ${res.exchangeNumber} cancelled`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Cancellation failed');
    },
  });

  const handlePost = (id: number) => {
    modal.confirm({
      title: 'Post Item Exchange',
      content: 'This will post the exchange: deduct expected item pending from site, and add actual returned item to godown bucket. Continue?',
      okText: 'Post Exchange',
      onOk: () => postExchange.mutate(id),
    });
  };

  const handleCancel = (id: number) => {
    let reason = '';
    modal.confirm({
      title: 'Cancel / Reverse Exchange',
      content: (
        <div style={{ marginTop: 8 }}>
          <p>Please enter the cancellation reason:</p>
          <Input
            onChange={(e) => {
              reason = e.target.value;
            }}
            placeholder="Cancellation reason"
          />
        </div>
      ),
      okText: 'Cancel Exchange',
      okType: 'danger',
      onOk: () => {
        if (!reason.trim()) {
          message.error('Cancellation reason is required');
          return Promise.reject();
        }
        cancelExchange.mutate({ id, reason });
      },
    });
  };

  const handleEdit = (record: ItemExchange) => {
    setEditing(record);
    form.setFieldsValue({
      agreementId: record.agreementId,
      expectedItemId: record.expectedItemId,
      actualItemId: record.actualItemId,
      exchangeDate: record.exchangeDate,
      expectedQuantity: record.expectedQuantity,
      actualQuantity: record.actualQuantity,
      expectedWeight: record.expectedWeight,
      actualWeight: record.actualWeight,
      destinationStockStatus: record.destinationStockStatus,
      reason: record.reason,
    });
    setCreateOpen(true);
  };

  const columns = [
    { title: 'Exchange Number', dataIndex: 'exchangeNumber', key: 'exchangeNumber' },
    { title: 'Date', dataIndex: 'exchangeDate', key: 'exchangeDate' },
    { title: 'Party', dataIndex: 'partyName', key: 'partyName' },
    { title: 'Site', dataIndex: 'siteName', key: 'siteName' },
    { title: 'Expected Item', render: (_: any, r: ItemExchange) => `${r.expectedItemCode} - ${r.expectedItemName}` },
    { title: 'Actual Item', render: (_: any, r: ItemExchange) => `${r.actualItemCode} - ${r.actualItemName}` },
    { title: 'Expected Qty', dataIndex: 'expectedQuantity', key: 'expectedQuantity', render: (v: number) => v.toFixed(2) },
    { title: 'Actual Qty', dataIndex: 'actualQuantity', key: 'actualQuantity', render: (v: number) => v.toFixed(2) },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => {
        let color = 'gold';
        if (s === 'POSTED') color = 'green';
        if (s === 'CANCELLED') color = 'red';
        return <Tag color={color}>{s}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_: any, r: ItemExchange) => (
        <Space>
          <Button size="small" onClick={() => setSelected(r)}>
            View
          </Button>
          {r.status === 'DRAFT' && canWrite && (
            <Button size="small" type="primary" ghost onClick={() => handleEdit(r)}>
              Edit
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="page-stack" data-testid="item-exchanges-page">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Item Exchanges</h1>
          <p className="page-description">
            Process returns of alternative sizes or materials than originally dispatched to sites.
          </p>
        </div>
        {canWrite && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing(null);
              form.resetFields();
              form.setFieldsValue({
                exchangeDate: new Date().toISOString().slice(0, 10),
                expectedQuantity: 0,
                actualQuantity: 0,
                expectedWeight: 0,
                actualWeight: 0,
                destinationStockStatus: 'AVAILABLE',
              });
              setCreateOpen(true);
            }}
          >
            Create Item Exchange
          </Button>
        )}
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search exchanges"
            allowClear
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 250 }}
          />
        </Space>

        <Table
          rowKey="id"
          loading={exchanges.isLoading}
          dataSource={exchanges.data?.content}
          columns={columns}
          pagination={{ hideOnSinglePage: true }}
        />
      </Card>

      {/* Form Drawer */}
      <FormDrawer
        title={editing ? `Edit Exchange Draft: ${editing.exchangeNumber}` : 'Record New Item Exchange'}
        open={createOpen}
        width={600}
        onClose={() => {
          setCreateOpen(false);
          form.resetFields();
          setEditing(null);
        }}
        onSubmit={() => form.submit()}
        loading={saveExchange.isPending}
        okText="Save Draft"
      >
        <Form form={form} layout="vertical" onFinish={(v) => saveExchange.mutate(v)}>
          <Form.Item
            name="agreementId"
            label="Active Agreement"
            rules={[{ required: true, message: 'Select agreement' }]}
          >
            <Select
              placeholder="Select active agreement"
              onChange={handleAgreementChange}
              options={activeAgreements.data?.map((a) => ({
                value: a.id,
                label: `${a.agreementNumber} — ${a.partyName} (${a.siteName})`,
              }))}
            />
          </Form.Item>

          {currentAgreement && (
            <Form.Item
              name="expectedItemId"
              label="Expected Item (on Site Pending)"
              rules={[{ required: true, message: 'Select expected item' }]}
            >
              <Select
                placeholder="Select expected item"
                onChange={handleExpectedItemChange}
                options={currentAgreement.items.map((i) => ({
                  value: i.itemId,
                  label: `${i.itemCode} — ${i.itemName} (${i.size ?? '-'})`,
                }))}
              />
            </Form.Item>
          )}

          <Form.Item
            name="actualItemId"
            label="Actual Returned Item (exchanged)"
            rules={[{ required: true, message: 'Select actual item' }]}
          >
            <Select
              placeholder="Select actual item"
              onChange={handleActualItemChange}
              showSearch
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={itemsQuery.data?.map((i) => ({
                value: i.id,
                label: `${i.itemCode} — ${i.itemName} (${i.size ?? '-'})`,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="exchangeDate"
            label="Exchange Date"
            rules={[{ required: true, message: 'Date is required' }]}
          >
            <Input type="date" />
          </Form.Item>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item name="expectedQuantity" label="Expected Qty" rules={[{ required: true }]}>
              <InputNumber
                min={0}
                style={{ width: 140 }}
                onChange={(v) => {
                  const agItem = currentAgreement?.items.find((i) => i.itemId === selectedExpectedItemId);
                  form.setFieldsValue({ expectedWeight: (v ?? 0) * (agItem?.weight ?? 0) });
                }}
              />
            </Form.Item>
            <Form.Item name="expectedWeight" label="Expected Weight (Kg)" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
          </Space>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item name="actualQuantity" label="Actual Qty" rules={[{ required: true }]}>
              <InputNumber
                min={0}
                style={{ width: 140 }}
                onChange={(v) => {
                  const matchedItem = itemsQuery.data?.find((i) => i.id === selectedActualItemId);
                  form.setFieldsValue({ actualWeight: (v ?? 0) * (matchedItem?.weight ?? 0) });
                }}
              />
            </Form.Item>
            <Form.Item name="actualWeight" label="Actual Weight (Kg)" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
          </Space>

          <Form.Item
            name="destinationStockStatus"
            label="Destination Stock Status"
            rules={[{ required: true }]}
          >
            <Select style={{ width: 220 }}>
              <Select.Option value="AVAILABLE">AVAILABLE (Good returned)</Select.Option>
              <Select.Option value="DAMAGED">DAMAGED (Needs repair)</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="reason" label="Reason/Remarks">
            <Input.TextArea rows={2} placeholder="Explain why size or type exchange happened..." />
          </Form.Item>
        </Form>
      </FormDrawer>

      {/* Detail Modal */}
      <Modal
        title={`Exchange Record Details: ${selected?.exchangeNumber ?? ''}`}
        open={!!selected}
        width={700}
        footer={null}
        onCancel={() => setSelected(undefined)}
      >
        {selected && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Source Type">{selected.sourceType}</Descriptions.Item>
              <Descriptions.Item label="Source Document">
                {selected.sourceReceivingChallanNumber ?? 'Manual'}
              </Descriptions.Item>
              <Descriptions.Item label="Agreement">{selected.agreementNumber}</Descriptions.Item>
              <Descriptions.Item label="Party">{selected.partyName}</Descriptions.Item>
              <Descriptions.Item label="Site">{selected.siteName}</Descriptions.Item>
              <Descriptions.Item label="Date">{selected.exchangeDate}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selected.status === 'POSTED' ? 'green' : selected.status === 'CANCELLED' ? 'red' : 'gold'}>
                  {selected.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Destination Bucket">{selected.destinationStockStatus}</Descriptions.Item>
              
              <Descriptions.Item label="Expected Item" span={2}>
                <strong>{selected.expectedItemCode}</strong> - {selected.expectedItemName}
              </Descriptions.Item>
              <Descriptions.Item label="Expected Qty">{selected.expectedQuantity}</Descriptions.Item>
              <Descriptions.Item label="Expected Weight">{selected.expectedWeight} Kg</Descriptions.Item>

              <Descriptions.Item label="Actual Item" span={2}>
                <strong>{selected.actualItemCode}</strong> - {selected.actualItemName}
              </Descriptions.Item>
              <Descriptions.Item label="Actual Qty">{selected.actualQuantity}</Descriptions.Item>
              <Descriptions.Item label="Actual Weight">{selected.actualWeight} Kg</Descriptions.Item>

              <Descriptions.Item label="Declared By">{selected.createdBy}</Descriptions.Item>
              <Descriptions.Item label="Declared At">
                {new Date(selected.createdAt).toLocaleString()}
              </Descriptions.Item>
              {selected.postedAt && (
                <>
                  <Descriptions.Item label="Posted By">{selected.postedBy}</Descriptions.Item>
                  <Descriptions.Item label="Posted At">
                    {new Date(selected.postedAt).toLocaleString()}
                  </Descriptions.Item>
                </>
              )}
              {selected.cancelledAt && (
                <>
                  <Descriptions.Item label="Cancelled By">{selected.cancelledBy}</Descriptions.Item>
                  <Descriptions.Item label="Cancelled At">
                    {new Date(selected.cancelledAt).toLocaleString()}
                  </Descriptions.Item>
                  <Descriptions.Item label="Cancellation Reason" span={2}>
                    {selected.cancellationReason}
                  </Descriptions.Item>
                </>
              )}
            </Descriptions>

            {selected.reason && (
              <div style={{ marginBottom: 16 }}>
                <strong>Remarks:</strong>
                <p>{selected.reason}</p>
              </div>
            )}

            <Space>
              {selected.status === 'DRAFT' && admin && (
                <Button type="primary" onClick={() => handlePost(selected.id)}>
                  Post / Record Stock
                </Button>
              )}
              {selected.status === 'POSTED' && admin && selected.sourceType === 'MANUAL_SITE_DECLARATION' && (
                <Button danger type="primary" onClick={() => handleCancel(selected.id)}>
                  Cancel / Revert Exchange
                </Button>
              )}
            </Space>
          </>
        )}
      </Modal>
    </div>
  );
}
