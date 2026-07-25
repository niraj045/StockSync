import { Button, Result } from 'antd';
import { useNavigate } from 'react-router';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="unauthorized-container">
      <div className="empty-state-card">
        <Result
          status="404"
          title="Page not found"
          subTitle="The workspace you requested does not exist or has moved."
          extra={
            <Button type="primary" onClick={() => navigate('/')}>
              Return to dashboard
            </Button>
          }
        />
      </div>
    </div>
  );
}
