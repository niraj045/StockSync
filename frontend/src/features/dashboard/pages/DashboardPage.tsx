import { Card, Col, Row, Space, Tag, Typography } from 'antd';

const placeholderMetrics = [
  { title: 'Total Inward', description: 'Available after inventory setup' },
  { title: 'Total Outward', description: 'Available after inventory setup' },
  { title: 'Current Available', description: 'Available after inventory setup' },
];

export function DashboardPage() {
  return (
    <Space direction="vertical" size="large" className="page-stack">
      <div>
        <Tag color="cyan">Phase 1 foundation</Tag>
        <Typography.Title level={2}>Dashboard</Typography.Title>
        <Typography.Paragraph type="secondary">
          The application shell is ready. Business metrics will be connected in the inventory phase.
        </Typography.Paragraph>
      </div>
      <Row gutter={[16, 16]}>
        {placeholderMetrics.map((metric) => (
          <Col xs={24} md={8} key={metric.title}>
            <Card title={metric.title} bordered={false}>
              <Typography.Text type="secondary">{metric.description}</Typography.Text>
            </Card>
          </Col>
        ))}
      </Row>
    </Space>
  );
}
