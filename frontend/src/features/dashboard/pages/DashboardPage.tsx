import { AppstoreOutlined, ArrowDownOutlined, ArrowUpOutlined, CheckCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { Card, Col, Row } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';

interface Summary { availableQuantity: number; scrappedQuantity: number; activeItems: number; belowMinimumItems: number }

export function DashboardPage() {
  const summary = useQuery({
    queryKey: ['stock-summary'],
    queryFn: async () => (await apiClient.get<Summary>('/stock/summary')).data,
  });
  const metrics = [
    { title: 'Current available', value: summary.data?.availableQuantity ?? 0, description: 'Total available item quantity', icon: <ArrowDownOutlined /> },
    { title: 'Scrapped stock', value: summary.data?.scrappedQuantity ?? 0, description: 'Quantity removed through scrap entries', icon: <ArrowUpOutlined /> },
    { title: 'Active items', value: summary.data?.activeItems ?? 0, description: `${summary.data?.belowMinimumItems ?? 0} below minimum threshold`, icon: <AppstoreOutlined /> },
  ];
  return (
    <div className="page-stack">
      <section className="dashboard-hero">
        <div>
          <span className="dashboard-kicker"><SafetyCertificateOutlined /> Secure workspace</span>
          <h1>Your inventory operations, clearly accounted for.</h1>
          <p>
            Review current stock, identify low material levels, and post controlled
            purchase, scrap and adjustment movements from one secure workspace.
          </p>
        </div>
        <div className="dashboard-phase-card">
          <CheckCircleOutlined style={{ color: '#72d9cc', fontSize: 22 }} />
          <strong>Inventory core active</strong>
          <span>Every stock change is protected by roles, concurrency control and an immutable ledger.</span>
        </div>
      </section>
      <Row gutter={[18, 18]} className="metric-grid">
        {metrics.map((metric) => (
          <Col xs={24} md={8} key={metric.title}>
            <Card className="metric-card">
              <div className="metric-icon">{metric.icon}</div>
              <span className="metric-label">{metric.title}</span>
              <span className="metric-value">{metric.value}</span>
              <span className="metric-note">{metric.description}</span>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
