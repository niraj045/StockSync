import { useState } from 'react';
import { App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Select, Space, Table, Tag } from 'antd';
import { SearchOutlined, PlusOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import type { Agreement } from '../../agreement/types';

type Page<T> = { content: T[]; totalElements: number };

export interface StockLoss {
  id: number;
  lossNumber: string;
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
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  lossDate: string;
  quantity: number;
  weight: number;
  chargeMethod: string;
  recoveryRate: number;
  calculatedRecoveryAmount: number;
  reason?: string;
  attachmentId?: number;
  status: string;
  approvedAt?: string;
  approvedBy?: string;
  reversedAt?: string;
  reversedBy?: string;
  reversalReason?: string;
  createdAt: string;
  createdBy: string;
  version: number;
}

type LossFormValue = {
  agreementId: number;
  itemId: number;
  lossDate: string;
  quantity: number;
  weight: number;
  chargeMethod: string;
  recoveryRate: number;
  reason?: string;
  attachmentId?: number;
};

export function StockLossesPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { message, modal } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<StockLoss>();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<StockLoss | null>(null);

  const [form] = Form.useForm<LossFormValue>();
  const selectedAgreementId = Form.useWatch('agreementId', form);

  // Queries
  const losses = useQuery({
    queryKey: ['stock-losses', search],
    queryFn: async () =>
      (
        await apiClient.get<Page<StockLoss>>('/stock-losses', {
          params: { search, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const activeAgreements = useQuery({
    queryKey: ['agreements-active-losses'],
    queryFn: async () =>
      (
        await apiClient.get<Page<Agreement>>('/agreements', {
          params: { status: 'ACTIVE', size: 100 },
        })
      ).data.content,
  });

  const currentAgreement = activeAgreements.data?.find((a) => a.id === selectedAgreementId);

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['stock-losses'] });
  };

  const handleAgreementChange = (_agreementId: number) => {
    form.setFieldsValue({ itemId: undefined as any });
  };

  const handleItemChange = (itemId: number) => {
    const agItem = currentAgreement?.items.find((i) => i.itemId === itemId);
    if (agItem) {
      const defaultRate = agItem.lossRatePerPiece > 0 ? agItem.lossRatePerPiece : agItem.lossRatePerWeight;
      const defaultMethod = agItem.lossRatePerWeight > 0 ? 'PER_WEIGHT' : 'PER_PIECE';
      form.setFieldsValue({
        recoveryRate: defaultRate,
        chargeMethod: defaultMethod,
      });
    }
  };

  // Mutations
  const saveLoss = useMutation({
    mutationFn: async (v: LossFormValue) => {
      const payload = {
        ...v,
        partyId: currentAgreement?.partyId,
        siteId: currentAgreement?.siteId,
      };
      if (editing) {
        return (await apiClient.put<StockLoss>(`/stock-losses/${editing.id}`, payload)).data;
      } else {
        return (await apiClient.post<StockLoss>('/stock-losses', payload)).data;
      }
    },
    onSuccess: (res) => {
      message.success(`Loss record ${res.lossNumber} saved successfully`);
      setCreateOpen(false);
      setEditing(null);
      form.resetFields();
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to save record');
    },
  });

  const approveLoss = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<StockLoss>(`/stock-losses/${id}/approve`)).data;
    },
    onSuccess: (res) => {
      message.success(`Loss record ${res.lossNumber} approved`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Approval failed');
    },
  });

  const reverseLoss = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) => {
      return (await apiClient.post<StockLoss>(`/stock-losses/${id}/reverse`, { reversalReason: reason })).data;
    },
    onSuccess: (res) => {
      message.success(`Loss record ${res.lossNumber} reversed successfully`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Reversal failed');
    },
  });

  const handleApprove = (id: number) => {
    modal.confirm({
      title: 'Approve Stock Loss',
      icon: <ExclamationCircleOutlined />,
      content: 'This will post the loss, deduct site stock pending, and adjust global loss metrics. Proceed?',
      okText: 'Approve',
      cancelText: 'Cancel',
      onOk: () => {
        approveLoss.mutate(id);
      },
    });
  };

  const handleReverse = (id: number) => {
    let reason = '';
    modal.confirm({
      title: 'Reverse Stock Loss',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div style={{ marginTop: 8 }}>
          <p>Please enter the reason for reversing this stock loss:</p>
          <Input
            onChange={(e) => {
              reason = e.target.value;
            }}
            placeholder="Reversal reason"
          />
        </div>
      ),
      okText: 'Reverse',
      cancelText: 'Cancel',
      onOk: () => {
        if (!reason.trim()) {
          message.error('Reversal reason is required');
          return Promise.reject();
        }
        reverseLoss.mutate({ id, reason });
      },
    });
  };

  const handleEdit = (record: StockLoss) => {
    setEditing(record);
    form.setFieldsValue({
      agreementId: record.agreementId,
      itemId: record.itemId,
      lossDate: record.lossDate,
      quantity: record.quantity,
      weight: record.weight,
      chargeMethod: record.chargeMethod,
      recoveryRate: record.recoveryRate,
      reason: record.reason,
      attachmentId: record.attachmentId,
    });
    setCreateOpen(true);
  };

  const columns = [
    { title: 'Loss Number', dataIndex: 'lossNumber', key: 'lossNumber' },
    { title: 'Date', dataIndex: 'lossDate', key: 'lossDate' },
    { title: 'Party', dataIndex: 'partyName', key: 'partyName' },
    { title: 'Site', dataIndex: 'siteName', key: 'siteName' },
    { title: 'Item', dataIndex: 'itemName', key: 'itemName', render: (_: any, r: StockLoss) => `${r.itemCode} - ${r.itemName}` },
    { title: 'Qty', dataIndex: 'quantity', key: 'quantity', render: (v: number) => v.toFixed(2) },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => {
        let color = 'gold';
        if (s === 'APPROVED') color = 'green';
        if (s === 'REVERSED') color = 'red';
        return <Tag color={color}>{s}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_: any, r: StockLoss) => (
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
    <div className="page-stack" data-testid="stock-losses-page">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Stock Losses</h1>
          <p className="page-description">
            Record and reconcile missing or lost shuttering materials from sites.
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
                lossDate: new Date().toISOString().slice(0, 10),
                quantity: 0,
                weight: 0,
                recoveryRate: 0,
                chargeMethod: 'PER_PIECE',
              });
              setCreateOpen(true);
            }}
          >
            Record Stock Loss
          </Button>
        )}
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search stock losses"
            allowClear
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 250 }}
          />
        </Space>

        <Table
          rowKey="id"
          loading={losses.isLoading}
          dataSource={losses.data?.content}
          columns={columns}
          pagination={{ hideOnSinglePage: true }}
        />
      </Card>

      {/* Form Drawer */}
      <FormDrawer
        title={editing ? `Edit Stock Loss Draft: ${editing.lossNumber}` : 'Record New Stock Loss'}
        open={createOpen}
        width={600}
        onClose={() => {
          setCreateOpen(false);
          form.resetFields();
          setEditing(null);
        }}
        onSubmit={() => form.submit()}
        loading={saveLoss.isPending}
        okText="Save Draft"
      >
        <Form form={form} layout="vertical" onFinish={(v) => saveLoss.mutate(v)}>
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
              name="itemId"
              label="Select Agreement Item"
              rules={[{ required: true, message: 'Select item' }]}
            >
              <Select
                placeholder="Select item"
                onChange={handleItemChange}
                options={currentAgreement.items.map((i) => ({
                  value: i.itemId,
                  label: `${i.itemCode} — ${i.itemName} (${i.size ?? '-'})`,
                }))}
              />
            </Form.Item>
          )}

          <Form.Item
            name="lossDate"
            label="Loss Declaration Date"
            rules={[{ required: true, message: 'Date is required' }]}
          >
            <Input type="date" />
          </Form.Item>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item name="quantity" label="Lost Quantity" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
            <Form.Item name="weight" label="Lost Weight (Kg)" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
          </Space>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item
              name="chargeMethod"
              label="Recovery Charge Method"
              rules={[{ required: true }]}
            >
              <Select style={{ width: 180 }}>
                <Select.Option value="NONE">NONE</Select.Option>
                <Select.Option value="PER_PIECE">PER_PIECE</Select.Option>
                <Select.Option value="PER_WEIGHT">PER_WEIGHT</Select.Option>
                <Select.Option value="FIXED">FIXED</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="recoveryRate" label="Recovery Rate (INR)" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
          </Space>

          <Form.Item name="reason" label="Reason/Remarks">
            <Input.TextArea rows={3} placeholder="Damage description or investigation notes" />
          </Form.Item>

          <Form.Item name="attachmentId" label="Attachment File ID">
            <InputNumber placeholder="File ID (Optional)" style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </FormDrawer>

      {/* Detail Modal */}
      <Modal
        title={`Loss Record Details: ${selected?.lossNumber ?? ''}`}
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
              <Descriptions.Item label="Item">
                {selected.itemCode} - {selected.itemName}
              </Descriptions.Item>
              <Descriptions.Item label="Date">{selected.lossDate}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selected.status === 'APPROVED' ? 'green' : selected.status === 'REVERSED' ? 'red' : 'gold'}>
                  {selected.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Lost Qty">{selected.quantity}</Descriptions.Item>
              <Descriptions.Item label="Lost Weight (Kg)">{selected.weight}</Descriptions.Item>
              <Descriptions.Item label="Charge Method">{selected.chargeMethod}</Descriptions.Item>
              <Descriptions.Item label="Recovery Rate">INR {selected.recoveryRate}</Descriptions.Item>
              <Descriptions.Item label="Calculated Charge" span={2}>
                <strong>INR {selected.calculatedRecoveryAmount}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Declared By">{selected.createdBy}</Descriptions.Item>
              <Descriptions.Item label="Declared At">
                {new Date(selected.createdAt).toLocaleString()}
              </Descriptions.Item>
              {selected.approvedAt && (
                <>
                  <Descriptions.Item label="Approved By">{selected.approvedBy}</Descriptions.Item>
                  <Descriptions.Item label="Approved At">
                    {new Date(selected.approvedAt).toLocaleString()}
                  </Descriptions.Item>
                </>
              )}
              {selected.reversedAt && (
                <>
                  <Descriptions.Item label="Reversed By">{selected.reversedBy}</Descriptions.Item>
                  <Descriptions.Item label="Reversed At">
                    {new Date(selected.reversedAt).toLocaleString()}
                  </Descriptions.Item>
                  <Descriptions.Item label="Reversal Reason" span={2}>
                    {selected.reversalReason}
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
              {selected.status === 'DRAFT' && canWrite && (
                <Button type="primary" onClick={() => handleApprove(selected.id)}>
                  Approve / Post Stock
                </Button>
              )}
              {selected.status === 'APPROVED' && admin && selected.sourceType === 'MANUAL_SITE_DECLARATION' && (
                <Button danger type="primary" onClick={() => handleReverse(selected.id)}>
                  Reverse Posting
                </Button>
              )}
            </Space>
          </>
        )}
      </Modal>
    </div>
  );
}
