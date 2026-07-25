import { Button, Result } from 'antd';
import { useNavigate } from 'react-router';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <Result
      status="404"
      title="Page not found"
      subTitle="The page you requested does not exist."
      extra={
        <Button type="primary" onClick={() => navigate('/')}>
          Return to dashboard
        </Button>
      }
    />
  );
}
