import { Button, Result } from 'antd';
import { useNavigate } from 'react-router';

export function UnauthorizedPage() {
  const navigate = useNavigate();

  return (
    <div className="unauthorized-container">
      <Result
        status="403"
        title="403"
        subTitle="Sorry, you are not authorized to access this page."
        extra={
          <Button type="primary" size="large" onClick={() => navigate('/')}>
            Back to Dashboard
          </Button>
        }
      />
    </div>
  );
}
