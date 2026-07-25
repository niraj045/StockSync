import { useState } from 'react';
import { Form, Input, Button, Alert } from 'antd';
import { BuildOutlined, LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
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
      <section className="login-brand-panel">
        <div className="login-brand">
          <span className="brand-mark"><BuildOutlined /></span>
          StockSync
        </div>
        <div className="login-message">
          <span className="dashboard-kicker"><SafetyCertificateOutlined /> Secure operations</span>
          <h1>Every asset.<br />Clearly accounted for.</h1>
          <p>
            A focused workspace for shuttering inventory, movement, accountability,
            and the teams that keep every site supplied.
          </p>
        </div>
        <div className="login-panel-footer">StockSync · Inventory operations platform</div>
      </section>

      <section className="login-form-panel">
        <div className="login-card">
          <span className="login-eyebrow">Welcome back</span>
          <h2 className="login-logo">Sign in to your workspace</h2>
          <p className="login-subtitle">Use your StockSync username or registered email.</p>

          {errorMsg && (
            <Alert message={errorMsg} type="error" showIcon style={{ marginBottom: 24 }} />
          )}

          <Form name="login_form" layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            label="Username or Email"
            name="usernameOrEmail"
            rules={[
              { required: true, message: 'Please input your Username or Email!' },
              { min: 3, message: 'Must be at least 3 characters long' }
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#78908c' }} />}
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
              prefix={<LockOutlined style={{ color: '#78908c' }} />}
              placeholder="Enter your password"
              size="large"
            />
          </Form.Item>

            <Form.Item style={{ marginTop: 30, marginBottom: 0 }}>
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
          <div className="login-help">Access is restricted to authorised business users.</div>
        </div>
      </section>
    </div>
  );
}
