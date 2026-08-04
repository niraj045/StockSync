import { App, Button, Card, DatePicker, Descriptions, Form, Input, Space, Table, Tag, Typography } from 'antd';
import { ArrowLeftOutlined, DownloadOutlined, FileDoneOutlined, SafetyCertificateOutlined, StopOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router';
import dayjs from 'dayjs';
import { apiClient } from '../../../../api/client';
import { paymentsApi } from '../../payment/api';
import type { PaymentReceipt } from '../../payment/types';
import { useAuth } from '../../../auth/context/AuthContext';

const money = (value?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value ?? 0);
const canPost = (roles: string[]) => roles.includes('ROLE_ADMIN') || roles.includes('ROLE_ACCOUNTS');
const canReverse = (roles: string[]) => roles.includes('ROLE_ADMIN');

export function PaymentDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { message, modal } = App.useApp();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const roles = user?.roles ?? [];

  const [tdsForm] = Form.useForm<{ deductionDate?: dayjs.Dayjs; section?: string; certificateNumber?: string; certificateDate?: dayjs.Dayjs }>();

  const paymentId = parseInt(id || '0', 10);

  const { data: selected, isLoading, isError } = useQuery({
    queryKey: ['payment', paymentId],
    queryFn: async () => {
      // Assuming a GET endpoint exists for single payment. If not, this might need adjustment depending on backend.
      return (await apiClient.get<PaymentReceipt>(`/payments/${paymentId}`)).data;
    },
    enabled: paymentId > 0,
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['payment', paymentId] });
    await queryClient.invalidateQueries({ queryKey: ['payments'] });
  };

  const postPayment = useMutation({ mutationFn: paymentsApi.post, onSuccess: () => { message.success('Posted'); invalidate(); } });
  
  const reversePayment = useMutation({
    mutationFn: ({ pId, reason }: { pId: number; reason: string }) => paymentsApi.reverse(pId, reason),
    onSuccess: () => { message.success('Reversed'); invalidate(); },
  });
  
  const updateTds = useMutation({
    mutationFn: ({ pId, values }: { pId: number; values: { deductionDate?: string; section?: string; certificateNumber?: string; certificateDate?: string } }) => paymentsApi.updateTds(pId, values),
    onSuccess: () => { message.success('TDS info saved'); invalidate(); },
  });
  
  const verifyTds = useMutation({ mutationFn: paymentsApi.verifyTds, onSuccess: () => { message.success('TDS Verified'); invalidate(); } });

  if (isError) {
    return <div className="page-stack"><Card>Error loading payment details. The payment might have been deleted.</Card></div>;
  }

  const openReverse = () => {
    if (!selected) return;
    let reasonText = '';
    modal.confirm({
      title: `Reverse ${selected.receiptNumber}`,
      content: <Input.TextArea placeholder="Required reason" rows={3} onChange={e => reasonText = e.target.value} />,
      okButtonProps: { danger: true },
      onOk: async () => {
        if (!reasonText.trim()) {
          message.error('Reversal reason is required');
          return Promise.reject();
        }
        await reversePayment.mutateAsync({ pId: selected.id, reason: reasonText.trim() });
      },
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate('/payments')} style={{ paddingLeft: 0, marginBottom: 8 }}>
            Back to Payments
          </Button>
          <h1 className="page-heading">Payment Detail</h1>
          <p className="page-description">Review receipt details, allocations, and TDS information.</p>
        </div>
        <Space>
          {selected?.status === 'DRAFT' && canPost(roles) && (
            <Button type="primary" icon={<FileDoneOutlined />} onClick={() => postPayment.mutate(selected.id)} loading={postPayment.isPending}>
              Post Payment
            </Button>
          )}
          {selected?.status === 'POSTED' && canReverse(roles) && (
            <Button danger icon={<StopOutlined />} onClick={openReverse} loading={reversePayment.isPending}>
              Reverse Payment
            </Button>
          )}
          {selected?.status === 'POSTED' && (
            <Button icon={<DownloadOutlined />} href={paymentsApi.receiptUrl(selected.id)}>
              Download PDF
            </Button>
          )}
        </Space>
      </div>

      <Card className="premium-card" loading={isLoading}>
        {selected && (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions bordered column={{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }}>
              <Descriptions.Item label="Receipt Number"><b>{selected.receiptNumber}</b></Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selected.status === 'POSTED' ? 'green' : selected.status === 'REVERSED' ? 'red' : 'orange'}>
                  {selected.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Party">{selected.partyName}</Descriptions.Item>
              <Descriptions.Item label="Payment Date">{selected.paymentDate}</Descriptions.Item>
              <Descriptions.Item label="Payment Mode">{selected.paymentMode}</Descriptions.Item>
              <Descriptions.Item label="Cash Amount">{money(selected.cashAmount)}</Descriptions.Item>
              <Descriptions.Item label="TDS Amount">{money(selected.tdsAmount)}</Descriptions.Item>
              <Descriptions.Item label="Unallocated Advance">{money(selected.unallocatedAmount)}</Descriptions.Item>
              <Descriptions.Item label="Total Settlement">{money(selected.totalSettlementAmount)}</Descriptions.Item>
            </Descriptions>

            {selected.allocations && selected.allocations.length > 0 ? (
              <>
                <Typography.Title level={4} style={{ marginTop: 24 }}>Invoice Allocations</Typography.Title>
                <Table 
                  rowKey="invoiceId" 
                  pagination={false} 
                  dataSource={selected.allocations} 
                  columns={[
                    { title: 'Invoice Number', dataIndex: 'invoiceNumber' },
                    { title: 'Cash Allocated', dataIndex: 'cashAllocated', render: money },
                    { title: 'TDS Allocated', dataIndex: 'tdsAllocated', render: money },
                  ]} 
                />
              </>
            ) : (
              <div style={{ marginTop: 24, padding: 24, background: '#f9f9f9', borderRadius: 8, textAlign: 'center', color: '#888' }}>
                No invoices were allocated during this payment. This amount acts as an advance.
              </div>
            )}

            {selected.tdsAmount > 0 && (
              <Card title="TDS Details & Certificate Info" style={{ marginTop: 24 }} bordered>
                <Form 
                  form={tdsForm} 
                  layout="vertical"
                  initialValues={{
                    deductionDate: selected.tdsDeductionDate ? dayjs(selected.tdsDeductionDate) : undefined,
                    section: selected.tdsSection,
                    certificateNumber: selected.tdsCertificateNumber,
                    certificateDate: selected.tdsCertificateDate ? dayjs(selected.tdsCertificateDate) : undefined
                  }}
                >
                  <Space wrap size="large">
                    <Form.Item name="deductionDate" label="Deduction Date"><DatePicker /></Form.Item>
                    <Form.Item name="section" label="Section"><Input placeholder="e.g. 194C" /></Form.Item>
                    <Form.Item name="certificateNumber" label="Certificate Number"><Input /></Form.Item>
                    <Form.Item name="certificateDate" label="Certificate Date"><DatePicker /></Form.Item>
                  </Space>
                  <div style={{ marginTop: 16 }}>
                    <Space>
                      <Button icon={<SafetyCertificateOutlined />} type="primary" onClick={async () => {
                        const values = await tdsForm.validateFields();
                        await updateTds.mutateAsync({ pId: selected.id, values: { ...values, deductionDate: values.deductionDate?.format('YYYY-MM-DD'), certificateDate: values.certificateDate?.format('YYYY-MM-DD') } });
                      }}>Save TDS Info</Button>
                      
                      {canPost(roles) && !selected.tdsVerified && (
                        <Button onClick={() => verifyTds.mutate(selected.id)} loading={verifyTds.isPending}>
                          Verify TDS Details
                        </Button>
                      )}
                      
                      {selected.tdsVerified && (
                        <Tag color="green" style={{ marginLeft: 16 }}>TDS Verified on {selected.tdsVerificationDate}</Tag>
                      )}
                    </Space>
                  </div>
                </Form>
              </Card>
            )}
          </Space>
        )}
      </Card>
    </div>
  );
}
