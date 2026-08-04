import { useState } from 'react';
import { App, Button, Card, Form, Input, Select, Space } from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import type { BillingRun } from '../types';

type EligibleAgreement = {
  id: number;
  agreementNumber: string;
  partyName: string;
  siteName: string;
  effectiveDate: string;
  expiryDate?: string;
};

const errorMessage = (error: unknown, fallback: string) => {
  const err = error as { response?: { data?: { message?: string } } };
  return err.response?.data?.message ?? fallback;
};

export function CreateBillingRunPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const qc = useQueryClient();

  const [createAgreementId, setCreateAgreementId] = useState<number | undefined>();
  const [periodStart, setPeriodStart] = useState<string>('');
  const [periodEnd, setPeriodEnd] = useState<string>('');

  const eligibleAgreementsQuery = useQuery({
    queryKey: ['eligible-agreements'],
    queryFn: async () => (await apiClient.get<EligibleAgreement[]>('/billing-runs/eligible-agreements')).data,
  });

  useQuery({
    queryKey: ['suggested-period', createAgreementId],
    queryFn: async () => {
      const res = await apiClient.get<Record<string, string>>(`/billing-runs/agreement/${createAgreementId}/suggested-period`);
      setPeriodStart(res.data.periodStart || '');
      setPeriodEnd(res.data.periodEnd || '');
      return res.data;
    },
    enabled: !!createAgreementId
  });

  const createMutation = useMutation({
    mutationFn: async (v: { agreementId: number; periodStart: string; periodEnd: string }) => 
      (await apiClient.post<BillingRun>('/billing-runs', v)).data,
    onSuccess: (b) => {
      message.success(`Created billing run: ${b.billingRunNumber}`);
      void qc.invalidateQueries({ queryKey: ['billing-runs'] });
      navigate(`/billing-runs/${b.id}`); // Navigate to details page immediately
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to create billing run'))
  });

  const handleCreate = () => {
    if (!createAgreementId || !periodStart || !periodEnd) {
      message.error('All fields are required');
      return;
    }
    if (dayjs(periodEnd).isAfter(dayjs(), 'day')) {
      message.error('Billing period cannot end in the future');
      return;
    }
    createMutation.mutate({
      agreementId: createAgreementId,
      periodStart,
      periodEnd
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate('/billing-runs')} style={{ paddingLeft: 0, marginBottom: 8 }}>
            Back to Billing Runs
          </Button>
          <h1 className="page-heading">Prepare Monthly Rental</h1>
          <p className="page-description">Select an agreement and period to calculate rent from deployed materials.</p>
        </div>
        <Space>
          <Button 
            type="primary" 
            size="large" 
            icon={<SaveOutlined />} 
            onClick={handleCreate} 
            loading={createMutation.isPending}
          >
            Create Billing Run
          </Button>
        </Space>
      </div>

      <Card className="premium-card">
        <Form layout="vertical" size="large" style={{ maxWidth: 600 }}>
          <Form.Item label="Select Active Agreement" required>
            <Select
              placeholder="Search and select an agreement"
              showSearch
              optionFilterProp="label"
              onChange={(v) => setCreateAgreementId(v)}
              options={(eligibleAgreementsQuery.data || []).map((ag: any) => ({
                value: ag.id,
                label: `${ag.agreementNumber} — ${ag.partyName} (${ag.siteName})`
              }))}
            />
          </Form.Item>
          
          {createAgreementId && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginTop: 24 }}>
              <Form.Item label="Period Start Date" required>
                <Input type="date" value={periodStart} onChange={e => setPeriodStart(e.target.value)} />
              </Form.Item>
              <Form.Item label="Period End Date" required>
                <Input type="date" value={periodEnd} onChange={e => setPeriodEnd(e.target.value)} />
              </Form.Item>
            </div>
          )}

          {createAgreementId && (
            <div style={{ marginTop: 16, color: '#64748b' }}>
              ℹ️ The suggested period is automatically calculated based on the last billing run or the agreement's effective date.
            </div>
          )}
        </Form>
      </Card>
    </div>
  );
}
