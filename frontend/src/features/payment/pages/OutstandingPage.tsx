import { useState } from 'react';
import { Card, Empty, Form, InputNumber, Radio, Space, Statistic } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { outstandingApi } from '../api';

type Scope = 'party' | 'site' | 'agreement' | 'invoice';
const moneyValue = (value?: number) => value ?? 0;

export function OutstandingPage() {
  const [scope, setScope] = useState<Scope>('party');
  const [id, setId] = useState<number>();
  const query = useQuery({
    queryKey: ['outstanding', scope, id],
    enabled: Boolean(id),
    queryFn: async () => {
      if (scope === 'invoice') return outstandingApi.invoice(id as number);
      if (scope === 'site') return outstandingApi.site(id as number);
      if (scope === 'agreement') return outstandingApi.agreement(id as number);
      return outstandingApi.party(id as number);
    },
  });

  const data = query.data;
  const isInvoice = data !== undefined && 'invoiceTotal' in data;
  const isAgreement = data !== undefined && 'totalInvoiced' in data;

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Outstanding</h1>
          <p className="page-description">Review billed totals, cash, TDS, deposits, advance and balances.</p>
        </div>
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Radio.Group value={scope} onChange={(event) => setScope(event.target.value as Scope)}>
            <Radio.Button value="party">Party</Radio.Button>
            <Radio.Button value="site">Site</Radio.Button>
            <Radio.Button value="agreement">Agreement</Radio.Button>
            <Radio.Button value="invoice">Invoice</Radio.Button>
          </Radio.Group>
          <Form.Item label={`${scope[0].toUpperCase()}${scope.slice(1)} id`} style={{ marginBottom: 0 }}>
            <InputNumber min={1} onChange={(value) => setId(typeof value === 'number' ? value : undefined)} />
          </Form.Item>
        </Space>
        {!data && <Empty description="Select a scope and id" />}
        {data && (
          <Space wrap size="large">
            {isInvoice && (
              <>
                <Statistic title="Invoice total" value={moneyValue(data.invoiceTotal)} prefix="Rs" precision={2} />
                <Statistic title="Cash" value={moneyValue(data.cashAllocated)} prefix="Rs" precision={2} />
                <Statistic title="TDS" value={moneyValue(data.tdsAllocated)} prefix="Rs" precision={2} />
                <Statistic title="Deposit adjusted" value={moneyValue(data.depositAdjusted)} prefix="Rs" precision={2} />
                <Statistic title="Outstanding" value={moneyValue(data.outstanding)} prefix="Rs" precision={2} />
              </>
            )}
            {isAgreement && (
              <>
                <Statistic title="Total invoiced" value={moneyValue(data.totalInvoiced)} prefix="Rs" precision={2} />
                <Statistic title="Total settled" value={moneyValue(data.totalSettled)} prefix="Rs" precision={2} />
                <Statistic title="Outstanding" value={moneyValue(data.outstanding)} prefix="Rs" precision={2} />
                <Statistic title="Required deposit" value={moneyValue(data.requiredDeposit)} prefix="Rs" precision={2} />
                <Statistic title="Available deposit" value={moneyValue(data.availableDeposit)} prefix="Rs" precision={2} />
              </>
            )}
            {!isInvoice && !isAgreement && (
              <>
                <Statistic title="Total billed" value={moneyValue(data.totalBilled)} prefix="Rs" precision={2} />
                <Statistic title="Cash received" value={moneyValue(data.cashReceived)} prefix="Rs" precision={2} />
                <Statistic title="TDS" value={moneyValue(data.tds)} prefix="Rs" precision={2} />
                <Statistic title="Deposit adjustments" value={moneyValue(data.depositAdjustments)} prefix="Rs" precision={2} />
                <Statistic title="Outstanding" value={moneyValue(data.outstanding)} prefix="Rs" precision={2} />
                <Statistic title="Advance" value={moneyValue(data.availableAdvance)} prefix="Rs" precision={2} />
                <Statistic title="Security deposit" value={moneyValue(data.availableSecurityDeposit)} prefix="Rs" precision={2} />
              </>
            )}
          </Space>
        )}
      </Card>
    </div>
  );
}
