import { useState } from 'react';
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Select, Space, Statistic, Table, Tag } from 'antd';
import { PlusOutlined, RetweetOutlined, SwapOutlined, UndoOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { depositsApi } from '../api';
import type { DepositSummary, Page, SecurityDepositTransaction } from '../types';
import { ReportExcelButton } from '../../../components/ReportExcelButton';

type AgreementOption = { id: number; agreementNumber: string; partyNameSnapshot: string; siteNameSnapshot: string };
type DepositAction = 'RECEIPT' | 'REFUND' | 'ADJUSTMENT_TO_INVOICE';
type DepositFormValues = {
  agreementId: number;
  invoiceId?: number;
  transactionDate: dayjs.Dayjs;
  amount: number;
  paymentMode?: string;
  referenceNumber?: string;
  reason?: string;
  notes?: string;
};

const money = (value?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value ?? 0);
const canWrite = (roles: string[]) => roles.includes('ROLE_ADMIN') || roles.includes('ROLE_ACCOUNTS');
const canReverse = (roles: string[]) => roles.includes('ROLE_ADMIN');

export function SecurityDepositsPage() {
  const { message } = App.useApp();
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const queryClient = useQueryClient();
  const [form] = Form.useForm<DepositFormValues>();
  const [action, setAction] = useState<DepositAction>('RECEIPT');
  const [modalOpen, setModalOpen] = useState(false);
  const [agreementId, setAgreementId] = useState<number>();

  const transactions = useQuery({
    queryKey: ['security-deposits'],
    queryFn: () => depositsApi.list({ size: 50, sort: 'id,desc' }),
  });
  const agreements = useQuery({
    queryKey: ['agreement-options'],
    queryFn: async () => (await apiClient.get<Page<AgreementOption>>('/agreements', { params: { size: 100, sort: 'id,desc' } })).data.content,
  });
  const summary = useQuery<DepositSummary>({
    queryKey: ['deposit-summary', agreementId],
    enabled: Boolean(agreementId),
    queryFn: () => depositsApi.summary(agreementId as number),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['security-deposits'] });
    await queryClient.invalidateQueries({ queryKey: ['deposit-summary'] });
  };

  const receipt = useMutation({ mutationFn: depositsApi.receipt, onSuccess: invalidate });
  const refund = useMutation({ mutationFn: depositsApi.refund, onSuccess: invalidate });
  const adjust = useMutation({ mutationFn: depositsApi.adjust, onSuccess: invalidate });
  const reverse = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => depositsApi.reverse(id, reason),
    onSuccess: invalidate,
  });

  const submit = async () => {
    const values = await form.validateFields();
    const base = { agreementId: values.agreementId, transactionDate: values.transactionDate.format('YYYY-MM-DD'), amount: values.amount };
    if (action === 'RECEIPT') {
      await receipt.mutateAsync({ ...base, paymentMode: values.paymentMode ?? 'CASH', referenceNumber: values.referenceNumber, notes: values.notes });
    } else if (action === 'REFUND') {
      await refund.mutateAsync({ ...base, paymentMode: values.paymentMode ?? 'CASH', referenceNumber: values.referenceNumber, reason: values.reason ?? '' });
    } else {
      await adjust.mutateAsync({ ...base, invoiceId: values.invoiceId as number, notes: values.notes });
    }
    message.success('Security deposit transaction posted');
    setModalOpen(false);
    form.resetFields();
  };

  const openReverse = (row: SecurityDepositTransaction) => {
    Modal.confirm({
      title: `Reverse ${row.depositNumber}`,
      content: <Input.TextArea id="deposit-reversal-reason" placeholder="Required reason" rows={3} />,
      okButtonProps: { danger: true },
      onOk: async () => {
        const input = document.getElementById('deposit-reversal-reason') as HTMLTextAreaElement | null;
        if (!input?.value.trim()) {
          message.error('Reversal reason is required');
          return Promise.reject();
        }
        await reverse.mutateAsync({ id: row.id, reason: input.value.trim() });
      },
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Security Deposits</h1>
          <p className="page-description">Record receipts, refunds and explicit invoice adjustments.</p>
        </div>
        <Space wrap><ReportExcelButton reportType="SECURITY_DEPOSIT_REPORT" filters={{ agreementId }} />{canWrite(roles) && <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>New transaction</Button>}</Space>
      </div>

      <Card className="premium-card">
        <Space wrap className="filters-bar">
          <Select
            allowClear
            placeholder="Agreement summary"
            style={{ width: 260 }}
            options={(agreements.data ?? []).map((agreement) => ({ value: agreement.id, label: agreement.agreementNumber }))}
            onChange={setAgreementId}
          />
          <Statistic title="Required" value={summary.data?.requiredDeposit ?? 0} prefix="Rs" precision={2} />
          <Statistic title="Available" value={summary.data?.available ?? 0} prefix="Rs" precision={2} />
          <Statistic title="Shortfall/excess" value={summary.data?.shortfallOrExcess ?? 0} prefix="Rs" precision={2} />
        </Space>
        <Table<SecurityDepositTransaction>
          rowKey="id"
          loading={transactions.isLoading}
          dataSource={transactions.data?.content ?? []}
          columns={[
            { title: 'Deposit no.', dataIndex: 'depositNumber', width: 150 },
            { title: 'Agreement', dataIndex: 'agreementNumber', width: 130 },
            { title: 'Party', dataIndex: 'partyName' },
            { title: 'Type', dataIndex: 'transactionType', width: 170 },
            { title: 'Date', dataIndex: 'transactionDate', width: 120 },
            { title: 'Amount', dataIndex: 'amount', render: money, width: 130 },
            { title: 'Invoice', dataIndex: 'relatedInvoiceNumber', width: 150 },
            { title: 'Status', dataIndex: 'status', width: 110, render: (status: SecurityDepositTransaction['status']) => <Tag color={status === 'POSTED' ? 'green' : 'red'}>{status}</Tag> },
            { title: 'Actions', width: 120, render: (_value: unknown, row) => canReverse(roles) && row.status === 'POSTED' ? <Button danger icon={<UndoOutlined />} onClick={() => openReverse(row)}>Reverse</Button> : null },
          ]}
        />
      </Card>

      <Modal title="Security deposit transaction" open={modalOpen} onCancel={() => setModalOpen(false)} onOk={submit} okText="Post">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<PlusOutlined />} type={action === 'RECEIPT' ? 'primary' : 'default'} onClick={() => setAction('RECEIPT')}>Receipt</Button>
          <Button icon={<RetweetOutlined />} type={action === 'REFUND' ? 'primary' : 'default'} onClick={() => setAction('REFUND')}>Refund</Button>
          <Button icon={<SwapOutlined />} type={action === 'ADJUSTMENT_TO_INVOICE' ? 'primary' : 'default'} onClick={() => setAction('ADJUSTMENT_TO_INVOICE')}>Adjust</Button>
        </Space>
        <Form form={form} layout="vertical" initialValues={{ transactionDate: dayjs(), paymentMode: 'CASH' }}>
          <Form.Item name="agreementId" label="Agreement" rules={[{ required: true }]}>
            <Select options={(agreements.data ?? []).map((agreement) => ({ value: agreement.id, label: `${agreement.agreementNumber} · ${agreement.partyNameSnapshot}` }))} />
          </Form.Item>
          {action === 'ADJUSTMENT_TO_INVOICE' && <Form.Item name="invoiceId" label="Invoice id" rules={[{ required: true }]}><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>}
          <Form.Item name="transactionDate" label="Date" rules={[{ required: true }]}><DatePicker /></Form.Item>
          <Form.Item name="amount" label="Amount" rules={[{ required: true }]}><InputNumber min={0.01} precision={2} style={{ width: '100%' }} /></Form.Item>
          {action !== 'ADJUSTMENT_TO_INVOICE' && <Form.Item name="paymentMode" label="Mode" rules={[{ required: true }]}><Select options={['CASH', 'BANK_TRANSFER', 'CHEQUE', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'OTHER'].map((value) => ({ value, label: value }))} /></Form.Item>}
          {action === 'REFUND' ? <Form.Item name="reason" label="Reason" rules={[{ required: true }]}><Input /></Form.Item> : <Form.Item name="notes" label="Notes"><Input /></Form.Item>}
          <Form.Item name="referenceNumber" label="Reference"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
