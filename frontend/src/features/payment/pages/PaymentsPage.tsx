import { useEffect, useMemo, useState } from 'react';
import { App, Button, Card, DatePicker, Divider, Drawer, Form, Input, InputNumber, Modal, Select, Space, Statistic, Table, Tag, Typography } from 'antd';
import { DownloadOutlined, FileDoneOutlined, PlusOutlined, SafetyCertificateOutlined, StopOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { paymentsApi, type AllocationInput, type PaymentInput } from '../api';
import type { EligibleInvoice, Page, PaymentReceipt } from '../types';

type PartyOption = { id: number; legalName: string };
type PaymentFormValues = Omit<PaymentInput, 'paymentDate' | 'chequeDate' | 'allocations'> & {
  paymentDate: dayjs.Dayjs;
  chequeDate?: dayjs.Dayjs;
  allocations?: AllocationInput[];
};

const money = (value?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value ?? 0);
const canPost = (roles: string[]) => roles.includes('ROLE_ADMIN') || roles.includes('ROLE_ACCOUNTS');
const canReverse = (roles: string[]) => roles.includes('ROLE_ADMIN');

export function PaymentsPage() {
  const { message } = App.useApp();
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const queryClient = useQueryClient();
  const [form] = Form.useForm<PaymentFormValues>();
  const [tdsForm] = Form.useForm<{ deductionDate?: dayjs.Dayjs; section?: string; certificateNumber?: string; certificateDate?: dayjs.Dayjs }>();
  const [search, setSearch] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selected, setSelected] = useState<PaymentReceipt | null>(null);
  const [partyId, setPartyId] = useState<number>();

  useEffect(() => {
    const handler = (event: BeforeUnloadEvent) => {
      if (drawerOpen) {
        event.preventDefault();
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [drawerOpen]);

  const payments = useQuery({
    queryKey: ['payments', search],
    queryFn: () => paymentsApi.list({ search, size: 50, sort: 'id,desc' }),
  });
  const parties = useQuery({
    queryKey: ['party-options'],
    queryFn: async () => (await apiClient.get<Page<PartyOption>>('/parties', { params: { size: 100, sort: 'legalName,asc' } })).data.content,
  });
  const eligible = useQuery({
    queryKey: ['eligible-invoices', partyId],
    enabled: Boolean(partyId),
    queryFn: () => paymentsApi.eligibleInvoices(partyId as number),
  });
  const advance = useQuery({
    queryKey: ['payment-advance', partyId],
    enabled: Boolean(partyId),
    queryFn: () => paymentsApi.advance(partyId as number),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['payments'] });
    await queryClient.invalidateQueries({ queryKey: ['eligible-invoices'] });
    await queryClient.invalidateQueries({ queryKey: ['payment-advance'] });
  };

  const createPayment = useMutation({
    mutationFn: (input: PaymentInput) => paymentsApi.create(input),
    onSuccess: async () => {
      message.success('Payment draft saved');
      setDrawerOpen(false);
      form.resetFields();
      await invalidate();
    },
  });
  const postPayment = useMutation({ mutationFn: paymentsApi.post, onSuccess: invalidate });
  const reversePayment = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => paymentsApi.reverse(id, reason),
    onSuccess: invalidate,
  });
  const updateTds = useMutation({
    mutationFn: ({ id, values }: { id: number; values: { deductionDate?: string; section?: string; certificateNumber?: string; certificateDate?: string } }) => paymentsApi.updateTds(id, values),
    onSuccess: invalidate,
  });
  const verifyTds = useMutation({ mutationFn: paymentsApi.verifyTds, onSuccess: invalidate });

  const allocationRows = Form.useWatch('allocations', form) ?? [];
  const totals = useMemo(() => allocationRows.reduce((sum, row) => ({
    cash: sum.cash + Number(row?.cashAllocated ?? 0),
    tds: sum.tds + Number(row?.tdsAllocated ?? 0),
  }), { cash: 0, tds: 0 }), [allocationRows]);
  const cashAmount = Number(Form.useWatch('cashAmount', form) ?? 0);
  const tdsAmount = Number(Form.useWatch('tdsAmount', form) ?? 0);
  const remainingAdvance = cashAmount + tdsAmount - totals.cash - totals.tds;

  const submit = async () => {
    const values = await form.validateFields();
    await createPayment.mutateAsync({
      ...values,
      paymentDate: values.paymentDate.format('YYYY-MM-DD'),
      chequeDate: values.chequeDate?.format('YYYY-MM-DD'),
      allocations: values.allocations ?? [],
    });
  };

  const openReverse = (record: PaymentReceipt) => {
    Modal.confirm({
      title: `Reverse ${record.receiptNumber}`,
      content: <Input.TextArea id="payment-reversal-reason" placeholder="Required reason" rows={3} />,
      okButtonProps: { danger: true },
      onOk: async () => {
        const input = document.getElementById('payment-reversal-reason') as HTMLTextAreaElement | null;
        if (!input?.value.trim()) {
          message.error('Reversal reason is required');
          return Promise.reject();
        }
        await reversePayment.mutateAsync({ id: record.id, reason: input.value.trim() });
      },
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Payments</h1>
          <p className="page-description">Record receipts, TDS, invoice allocations and advances.</p>
        </div>
        <Button icon={<PlusOutlined />} type="primary" onClick={() => setDrawerOpen(true)}>Create payment</Button>
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input.Search allowClear placeholder="Search receipt or party" onSearch={setSearch} style={{ width: 280 }} />
        </Space>
        <Table<PaymentReceipt>
          rowKey="id"
          loading={payments.isLoading}
          dataSource={payments.data?.content ?? []}
          columns={[
            { title: 'Receipt', dataIndex: 'receiptNumber', width: 150 },
            { title: 'Party', dataIndex: 'partyName' },
            { title: 'Date', dataIndex: 'paymentDate', width: 120 },
            { title: 'Mode', dataIndex: 'paymentMode', width: 130 },
            { title: 'Cash', dataIndex: 'cashAmount', render: money, width: 130 },
            { title: 'TDS', dataIndex: 'tdsAmount', render: money, width: 120 },
            { title: 'Advance', dataIndex: 'unallocatedAmount', render: money, width: 130 },
            { title: 'Status', dataIndex: 'status', width: 110, render: (status: PaymentReceipt['status']) => <Tag color={status === 'POSTED' ? 'green' : status === 'REVERSED' ? 'red' : 'orange'}>{status}</Tag> },
            {
              title: 'Actions',
              width: 320,
              render: (_value: unknown, row) => (
                <Space>
                  <Button onClick={() => setSelected(row)}>Details</Button>
                  {row.status === 'DRAFT' && canPost(roles) && <Button icon={<FileDoneOutlined />} onClick={() => postPayment.mutate(row.id)}>Post</Button>}
                  {row.status === 'POSTED' && canReverse(roles) && <Button danger icon={<StopOutlined />} onClick={() => openReverse(row)}>Reverse</Button>}
                  {row.status === 'POSTED' && <Button icon={<DownloadOutlined />} href={paymentsApi.receiptUrl(row.id)}>PDF</Button>}
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Drawer title="Create payment" width={760} open={drawerOpen} onClose={() => setDrawerOpen(false)} extra={<Button type="primary" onClick={submit} loading={createPayment.isPending}>Save draft</Button>}>
        <Form form={form} layout="vertical" initialValues={{ paymentDate: dayjs(), paymentMode: 'CASH', cashAmount: 0, tdsAmount: 0, allocations: [] }}>
          <Space wrap align="start">
            <Form.Item name="partyId" label="Party" rules={[{ required: true }]} style={{ width: 260 }}>
              <Select
                showSearch
                optionFilterProp="label"
                options={(parties.data ?? []).map((party) => ({ value: party.id, label: party.legalName }))}
                onChange={setPartyId}
              />
            </Form.Item>
            <Form.Item name="paymentDate" label="Date" rules={[{ required: true }]}><DatePicker /></Form.Item>
            <Form.Item name="paymentMode" label="Mode" rules={[{ required: true }]}>
              <Select style={{ width: 170 }} options={['CASH', 'BANK_TRANSFER', 'CHEQUE', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'OTHER'].map((value) => ({ value, label: value }))} />
            </Form.Item>
            <Form.Item name="referenceNumber" label="Reference"><Input style={{ width: 180 }} /></Form.Item>
          </Space>
          <Space wrap align="start">
            <Form.Item name="cashAmount" label="Cash amount" rules={[{ required: true }]}><InputNumber min={0} precision={2} /></Form.Item>
            <Form.Item name="tdsAmount" label="TDS amount" rules={[{ required: true }]}><InputNumber min={0} precision={2} /></Form.Item>
            <Form.Item name="notes" label="Notes"><Input style={{ width: 300 }} /></Form.Item>
          </Space>
          <Divider />
          <Space wrap>
            <Statistic title="Allocated cash" value={totals.cash} prefix="Rs" precision={2} />
            <Statistic title="Allocated TDS" value={totals.tds} prefix="Rs" precision={2} />
            <Statistic title="Remaining advance" value={remainingAdvance} prefix="Rs" precision={2} />
            <Statistic title="Existing advance" value={advance.data?.availableAdvance ?? 0} prefix="Rs" precision={2} />
          </Space>
          <Form.List name="allocations">
            {(fields, { add, remove }) => (
              <div>
                <Divider orientation="left">Invoice allocations</Divider>
                {fields.map((field) => (
                  <Space key={field.key} wrap align="baseline">
                    <Form.Item name={[field.name, 'invoiceId']} rules={[{ required: true }]}>
                      <Select style={{ width: 260 }} placeholder="Invoice" options={(eligible.data ?? []).map((invoice: EligibleInvoice) => ({ value: invoice.id, label: `${invoice.invoiceNumber} (${money(invoice.outstandingAmount)})` }))} />
                    </Form.Item>
                    <Form.Item name={[field.name, 'cashAllocated']}><InputNumber min={0} precision={2} placeholder="Cash" /></Form.Item>
                    <Form.Item name={[field.name, 'tdsAllocated']}><InputNumber min={0} precision={2} placeholder="TDS" /></Form.Item>
                    <Button onClick={() => remove(field.name)}>Remove</Button>
                  </Space>
                ))}
                <Button onClick={() => add({ cashAllocated: 0, tdsAllocated: 0 })}>Add allocation</Button>
              </div>
            )}
          </Form.List>
        </Form>
      </Drawer>

      <Drawer title={selected?.receiptNumber} open={Boolean(selected)} width={620} onClose={() => setSelected(null)}>
        {selected && (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Typography.Text>{selected.partyName} · {selected.paymentMode} · {money(selected.totalSettlementAmount)}</Typography.Text>
            <Table rowKey="invoiceId" pagination={false} dataSource={selected.allocations} columns={[
              { title: 'Invoice', dataIndex: 'invoiceNumber' },
              { title: 'Cash', dataIndex: 'cashAllocated', render: money },
              { title: 'TDS', dataIndex: 'tdsAllocated', render: money },
            ]} />
            {selected.tdsAmount > 0 && (
              <Form form={tdsForm} layout="vertical">
                <Space wrap>
                  <Form.Item name="deductionDate" label="Deduction date"><DatePicker /></Form.Item>
                  <Form.Item name="section" label="Section"><Input /></Form.Item>
                  <Form.Item name="certificateNumber" label="Certificate"><Input /></Form.Item>
                  <Form.Item name="certificateDate" label="Certificate date"><DatePicker /></Form.Item>
                </Space>
                <Space>
                  <Button icon={<SafetyCertificateOutlined />} onClick={async () => {
                    const values = await tdsForm.validateFields();
                    await updateTds.mutateAsync({ id: selected.id, values: { ...values, deductionDate: values.deductionDate?.format('YYYY-MM-DD'), certificateDate: values.certificateDate?.format('YYYY-MM-DD') } });
                  }}>Save TDS</Button>
                  {canPost(roles) && <Button onClick={() => verifyTds.mutate(selected.id)}>Verify TDS</Button>}
                </Space>
              </Form>
            )}
          </Space>
        )}
      </Drawer>
    </div>
  );
}
