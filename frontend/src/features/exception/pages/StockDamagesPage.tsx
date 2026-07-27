import { useState } from 'react';
import { App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Switch } from 'antd';
import { SearchOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import type { Agreement } from '../../agreement/types';

type Page<T> = { content: T[]; totalElements: number };

export interface StockDamage {
  id: number;
  damageNumber: string;
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
  quantity: number;
  weight: number;
  repairable: boolean;
  status: string;
  actualRepairCost?: number;
  notes?: string;
  reversalReason?: string;
  scrapReason?: string;
  damageDate: string;
  damageType: string;
  conditionNotes?: string;
  chargeMethod: string;
  damageRate: number;
  calculatedDamageAmount: number;
  estimatedRepairCost: number;
  attachmentId?: number;
  recordedBy?: string;
  recordedAt?: string;
  reversedBy?: string;
  reversedAt?: string;
  createdAt: string;
  createdBy: string;
  version: number;
}

type DamageFormValue = {
  agreementId: number;
  itemId: number;
  quantity: number;
  weight: number;
  repairable: boolean;
  notes?: string;
  damageDate: string;
  damageType: string;
  conditionNotes?: string;
  chargeMethod: string;
  damageRate: number;
  estimatedRepairCost: number;
  attachmentId?: number;
};

export function StockDamagesPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { message, modal } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<StockDamage>();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<StockDamage | null>(null);

  const [form] = Form.useForm<DamageFormValue>();
  const selectedAgreementId = Form.useWatch('agreementId', form);

  // Queries
  const damages = useQuery({
    queryKey: ['stock-damages', search],
    queryFn: async () =>
      (
        await apiClient.get<Page<StockDamage>>('/stock-damages', {
          params: { search, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const activeAgreements = useQuery({
    queryKey: ['agreements-active-damages'],
    queryFn: async () =>
      (
        await apiClient.get<Page<Agreement>>('/agreements', {
          params: { status: 'ACTIVE', size: 100 },
        })
      ).data.content,
  });

  const currentAgreement = activeAgreements.data?.find((a) => a.id === selectedAgreementId);

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['stock-damages'] });
  };

  const handleAgreementChange = (_agreementId: number) => {
    form.setFieldsValue({ itemId: undefined as any });
  };

  const handleItemChange = (itemId: number) => {
    const agItem = currentAgreement?.items.find((i) => i.itemId === itemId);
    if (agItem) {
      form.setFieldsValue({
        damageRate: agItem.damageRate,
        chargeMethod: agItem.damageRate > 0 ? 'PER_PIECE' : 'NONE',
      });
    }
  };

  // Mutations
  const saveDamage = useMutation({
    mutationFn: async (v: DamageFormValue) => {
      const payload = {
        ...v,
        partyId: currentAgreement?.partyId,
        siteId: currentAgreement?.siteId,
      };
      if (editing) {
        return (await apiClient.put<StockDamage>(`/stock-damages/${editing.id}`, payload)).data;
      } else {
        return (await apiClient.post<StockDamage>('/stock-damages', payload)).data;
      }
    },
    onSuccess: (res) => {
      message.success(`Damage record ${res.damageNumber} saved`);
      setCreateOpen(false);
      setEditing(null);
      form.resetFields();
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to save record');
    },
  });

  const recordDamage = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<StockDamage>(`/stock-damages/${id}/record`)).data;
    },
    onSuccess: (res) => {
      message.success(`Damage record ${res.damageNumber} posted to stock ledger`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Recording failed');
    },
  });

  const startRepair = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<StockDamage>(`/stock-damages/${id}/start-repair`)).data;
    },
    onSuccess: (res) => {
      message.success(`Repair started for damage record ${res.damageNumber}`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Repair start failed');
    },
  });

  const markRepaired = useMutation({
    mutationFn: async ({ id, cost }: { id: number; cost: number }) => {
      return (await apiClient.post<StockDamage>(`/stock-damages/${id}/mark-repaired`, { actualRepairCost: cost })).data;
    },
    onSuccess: (res) => {
      message.success(`Damage record ${res.damageNumber} marked as REPAIRED`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Repair completion failed');
    },
  });

  const scrapDamage = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<StockDamage>(`/stock-damages/${id}/scrap`)).data;
    },
    onSuccess: (res) => {
      message.success(`Damage record ${res.damageNumber} marked as SCRAPPED`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Scrap failed');
    },
  });

  const reverseDamage = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) => {
      return (await apiClient.post<StockDamage>(`/stock-damages/${id}/reverse`, { reversalReason: reason })).data;
    },
    onSuccess: (res) => {
      message.success(`Damage record ${res.damageNumber} reversed`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Reversal failed');
    },
  });

  const handleRecord = (id: number) => {
    modal.confirm({
      title: 'Record Stock Damage',
      content: 'This will post the damage record, deduct site pending stock, and move it to DAMAGED global stock. Continue?',
      okText: 'Record',
      onOk: () => recordDamage.mutate(id),
    });
  };

  const handleStartRepair = (id: number) => {
    modal.confirm({
      title: 'Start Repair Lifecycle',
      content: 'Move this damage record to UNDER_REPAIR status. Proceed?',
      okText: 'Start Repair',
      onOk: () => startRepair.mutate(id),
    });
  };

  const handleMarkRepaired = (id: number) => {
    let cost = 0;
    modal.confirm({
      title: 'Complete Repair',
      content: (
        <div style={{ marginTop: 8 }}>
          <p>Please enter the actual repair cost (INR) incurred:</p>
          <InputNumber
            min={0}
            style={{ width: '100%' }}
            onChange={(v) => {
              cost = v ?? 0;
            }}
            placeholder="Actual repair cost"
          />
        </div>
      ),
      okText: 'Complete',
      onOk: () => markRepaired.mutate({ id, cost }),
    });
  };

  const handleScrap = (id: number) => {
    modal.confirm({
      title: 'Scrap Damaged Material',
      content: 'This will permanently write off the material and move it to global SCRAPPED bucket. This cannot be undone. Proceed?',
      okText: 'Scrap Material',
      okType: 'danger',
      onOk: () => scrapDamage.mutate(id),
    });
  };

  const handleReverse = (id: number) => {
    let reason = '';
    modal.confirm({
      title: 'Reverse Damage Record',
      content: (
        <div style={{ marginTop: 8 }}>
          <p>Please enter the reversal reason:</p>
          <Input
            onChange={(e) => {
              reason = e.target.value;
            }}
            placeholder="Reversal reason"
          />
        </div>
      ),
      okText: 'Reverse',
      onOk: () => {
        if (!reason.trim()) {
          message.error('Reversal reason is required');
          return Promise.reject();
        }
        reverseDamage.mutate({ id, reason });
      },
    });
  };

  const handleEdit = (record: StockDamage) => {
    setEditing(record);
    form.setFieldsValue({
      agreementId: record.agreementId,
      itemId: record.itemId,
      damageDate: record.damageDate,
      quantity: record.quantity,
      weight: record.weight,
      repairable: record.repairable,
      damageType: record.damageType,
      conditionNotes: record.conditionNotes,
      chargeMethod: record.chargeMethod,
      damageRate: record.damageRate,
      estimatedRepairCost: record.estimatedRepairCost,
      attachmentId: record.attachmentId,
    });
    setCreateOpen(true);
  };

  const columns = [
    { title: 'Damage Number', dataIndex: 'damageNumber', key: 'damageNumber' },
    { title: 'Date', dataIndex: 'damageDate', key: 'damageDate' },
    { title: 'Party', dataIndex: 'partyName', key: 'partyName' },
    { title: 'Site', dataIndex: 'siteName', key: 'siteName' },
    { title: 'Item', render: (_: any, r: StockDamage) => `${r.itemCode} - ${r.itemName}` },
    { title: 'Qty', dataIndex: 'quantity', key: 'quantity', render: (v: number) => v.toFixed(2) },
    { title: 'Type', dataIndex: 'damageType', key: 'damageType' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => {
        let color = 'gold';
        if (s === 'RECORDED') color = 'blue';
        if (s === 'UNDER_REPAIR') color = 'purple';
        if (s === 'REPAIRED') color = 'green';
        if (s === 'SCRAPPED') color = 'volcano';
        if (s === 'REVERSED') color = 'red';
        return <Tag color={color}>{s}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_: any, r: StockDamage) => (
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
    <div className="page-stack" data-testid="stock-damages-page">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Stock Damages</h1>
          <p className="page-description">
            Monitor, repair, and lifecycle-track damaged inventory components from sites.
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
                damageDate: new Date().toISOString().slice(0, 10),
                quantity: 0,
                weight: 0,
                repairable: true,
                damageType: 'REPAIRABLE',
                chargeMethod: 'PER_PIECE',
                damageRate: 0,
                estimatedRepairCost: 0,
              });
              setCreateOpen(true);
            }}
          >
            Record Damage
          </Button>
        )}
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search damages"
            allowClear
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 250 }}
          />
        </Space>

        <Table
          rowKey="id"
          loading={damages.isLoading}
          dataSource={damages.data?.content}
          columns={columns}
          pagination={{ hideOnSinglePage: true }}
        />
      </Card>

      {/* Form Drawer */}
      <FormDrawer
        title={editing ? `Edit Damage Draft: ${editing.damageNumber}` : 'Record New Stock Damage'}
        open={createOpen}
        width={600}
        onClose={() => {
          setCreateOpen(false);
          form.resetFields();
          setEditing(null);
        }}
        onSubmit={() => form.submit()}
        loading={saveDamage.isPending}
        okText="Save Draft"
      >
        <Form form={form} layout="vertical" onFinish={(v) => saveDamage.mutate(v)}>
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
            name="damageDate"
            label="Damage Declaration Date"
            rules={[{ required: true, message: 'Date is required' }]}
          >
            <Input type="date" />
          </Form.Item>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item name="quantity" label="Damaged Quantity" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
            <Form.Item name="weight" label="Damaged Weight (Kg)" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
          </Space>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item name="repairable" label="Repairable?" valuePropName="checked">
              <Switch checkedChildren="Yes" unCheckedChildren="No" />
            </Form.Item>

            <Form.Item
              name="damageType"
              label="Damage Type Category"
              rules={[{ required: true }]}
            >
              <Select style={{ width: 180 }}>
                <Select.Option value="MINOR">MINOR</Select.Option>
                <Select.Option value="BENT">BENT</Select.Option>
                <Select.Option value="BROKEN">BROKEN</Select.Option>
                <Select.Option value="RUSTED">RUSTED</Select.Option>
                <Select.Option value="REPAIRABLE">REPAIRABLE</Select.Option>
                <Select.Option value="NON_REPAIRABLE">NON_REPAIRABLE</Select.Option>
                <Select.Option value="OTHER">OTHER</Select.Option>
              </Select>
            </Form.Item>
          </Space>

          <Form.Item name="conditionNotes" label="Condition / Repair Notes">
            <Input.TextArea rows={2} placeholder="Explain specific defects..." />
          </Form.Item>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item
              name="chargeMethod"
              label="Damage Charge Method"
              rules={[{ required: true }]}
            >
              <Select style={{ width: 180 }}>
                <Select.Option value="NONE">NONE</Select.Option>
                <Select.Option value="PER_PIECE">PER_PIECE</Select.Option>
                <Select.Option value="PER_WEIGHT">PER_WEIGHT</Select.Option>
                <Select.Option value="FIXED">FIXED</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="damageRate" label="Damage Rate (INR)" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 140 }} />
            </Form.Item>
          </Space>

          <Form.Item name="estimatedRepairCost" label="Estimated Repair Cost (INR)">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="attachmentId" label="Attachment File ID">
            <InputNumber placeholder="File ID (Optional)" style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </FormDrawer>

      {/* Detail Modal */}
      <Modal
        title={`Damage Record Details: ${selected?.damageNumber ?? ''}`}
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
              <Descriptions.Item label="Date">{selected.damageDate}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selected.status === 'REPAIRED' ? 'green' : selected.status === 'REVERSED' ? 'red' : 'blue'}>
                  {selected.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Damaged Qty">{selected.quantity}</Descriptions.Item>
              <Descriptions.Item label="Damaged Weight">{selected.weight} Kg</Descriptions.Item>
              <Descriptions.Item label="Repairable">{selected.repairable ? 'Yes' : 'No'}</Descriptions.Item>
              <Descriptions.Item label="Damage Category">{selected.damageType}</Descriptions.Item>
              <Descriptions.Item label="Charge Method">{selected.chargeMethod}</Descriptions.Item>
              <Descriptions.Item label="Damage Rate">INR {selected.damageRate}</Descriptions.Item>
              <Descriptions.Item label="Calculated Charge" span={2}>
                <strong>INR {selected.calculatedDamageAmount}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Est. Repair Cost">INR {selected.estimatedRepairCost}</Descriptions.Item>
              <Descriptions.Item label="Actual Repair Cost">INR {selected.actualRepairCost}</Descriptions.Item>
              <Descriptions.Item label="Recorded By">{selected.recordedBy ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Recorded At">
                {selected.recordedAt ? new Date(selected.recordedAt).toLocaleString() : '-'}
              </Descriptions.Item>
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

            {selected.conditionNotes && (
              <div style={{ marginBottom: 16 }}>
                <strong>Condition/Remarks:</strong>
                <p>{selected.conditionNotes}</p>
              </div>
            )}

            <Space>
              {selected.status === 'DRAFT' && canWrite && (
                <Button type="primary" onClick={() => handleRecord(selected.id)}>
                  Post / Record Stock
                </Button>
              )}
              {selected.status === 'RECORDED' && admin && selected.repairable && (
                <Button type="primary" onClick={() => handleStartRepair(selected.id)}>
                  Start Repair
                </Button>
              )}
              {selected.status === 'UNDER_REPAIR' && admin && (
                <Button type="primary" onClick={() => handleMarkRepaired(selected.id)}>
                  Mark Repaired
                </Button>
              )}
              {(selected.status === 'RECORDED' || selected.status === 'UNDER_REPAIR') && admin && (
                <Button danger onClick={() => handleScrap(selected.id)}>
                  Scrap Material
                </Button>
              )}
              {(selected.status === 'RECORDED' || selected.status === 'UNDER_REPAIR') && admin && selected.sourceType === 'MANUAL_SITE_DECLARATION' && (
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
