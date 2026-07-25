import { useState } from 'react';
import { Form, Input, Button, Alert } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const onFinish = async (values: any) => {
    setLoading(true);
    setErrorMsg(null);
    try {
      await login(values.usernameOrEmail, values.password);
      navigate('/');
    } catch (err: any) {
      const code = err.response?.data?.code;
      const msg = err.response?.data?.message;

      if (code === 'INVALID_CREDENTIALS') {
        setErrorMsg('Invalid username/email or password. Please try again.');
      } else if (code === 'ACCOUNT_DISABLED') {
        setErrorMsg('Your account has been deactivated. Please contact an administrator.');
      } else if (msg) {
        setErrorMsg(msg);
      } else {
        setErrorMsg('Unable to log in. Please check your network connection.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-background-glow" />
      <div className="login-background-glow-bottom" />

      <div className="login-card">
        <h1 className="login-logo">StockSync</h1>
        <p className="login-subtitle">Shuttering Inventory Management</p>

        {errorMsg && (
          <Alert
            message={errorMsg}
            type="error"
            showIcon
            style={{ marginBottom: 24, borderRadius: 8 }}
          />
        )}

        <Form
          name="login_form"
          layout="vertical"
          onFinish={onFinish}
          requiredMark={false}
        >
          <Form.Item
            label="Username or Email"
            name="usernameOrEmail"
            rules={[
              { required: true, message: 'Please input your Username or Email!' },
              { min: 3, message: 'Must be at least 3 characters long' }
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
              placeholder="Enter your username or email"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="Password"
            name="password"
            rules={[{ required: true, message: 'Please input your Password!' }]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
              placeholder="Enter your password"
              size="large"
            />
          </Form.Item>

          <Form.Item style={{ marginTop: 32 }}>
            <Button
              type="primary"
              htmlType="submit"
              className="login-submit-btn"
              block
              loading={loading}
            >
              Sign In
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
}
