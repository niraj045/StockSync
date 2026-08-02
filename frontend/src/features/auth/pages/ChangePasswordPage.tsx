import { useState } from 'react';
import { Card, Form, Input, Button, message, Alert } from 'antd';
import { LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../api/client';

export function ChangePasswordPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const onFinish = async (values: any) => {
    setLoading(true);
    setErrorMsg(null);
    try {
      await apiClient.post('/auth/change-password', {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      message.success('Password changed successfully. Please log in with your new password.');
      // After changing password, other sessions are invalidated and current user needs to log in again or stays logged in.
      // Wait, our backend requirement: "Invalidate other active sessions of the user while maintaining the current user session."
      // Since current session is maintained, the user is still logged in! They can just navigate back to dashboard.
      navigate('/');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to change password.';
      setErrorMsg(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-page">
      <div className="form-page-intro">
        <h1>Change password</h1>
        <p>Choose a strong password to keep your SteelFab account secure.</p>
      </div>
      <Card className="premium-card">
        {errorMsg && (
          <Alert
            message={errorMsg}
            type="error"
            showIcon
            style={{ marginBottom: 24, borderRadius: 8 }}
          />
        )}

        <Form
          form={form}
          name="change_password"
          layout="vertical"
          onFinish={onFinish}
          requiredMark={false}
        >
          <Form.Item
            label="Current Password"
            name="currentPassword"
            rules={[{ required: true, message: 'Please enter your current password' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Enter current password" size="large" />
          </Form.Item>

          <Form.Item
            label="New Password"
            name="newPassword"
            rules={[
              { required: true, message: 'Please enter your new password' },
              { min: 8, message: 'Password must be at least 8 characters long' },
              {
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
                message: 'Password must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number',
              },
            ]}
          >
            <Input.Password prefix={<SafetyCertificateOutlined />} placeholder="Enter new password" size="large" />
          </Form.Item>

          <Form.Item
            label="Confirm New Password"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Please confirm your new password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('The two passwords do not match!'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<SafetyCertificateOutlined />} placeholder="Confirm new password" size="large" />
          </Form.Item>

          <Form.Item style={{ marginTop: 32, marginBottom: 0 }}>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
              <Button size="large" onClick={() => navigate('/')}>
                Cancel
              </Button>
              <Button type="primary" htmlType="submit" size="large" loading={loading}>
                Change Password
              </Button>
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
