import { Button, Result } from 'antd';
import { useNavigate } from 'react-router';

export function UnauthorizedPage() {
  const navigate = useNavigate();

  return (
    <div className="unauthorized-container">
      <div className="empty-state-card">
        <Result
          status="403"
          title="Access restricted"
          subTitle="Your assigned role does not allow access to this workspace."
          extra={
            <Button type="primary" size="large" onClick={() => navigate('/')}>
              Back to dashboard
            </Button>
          }
        />
      </div>
    </div>
  );
}
