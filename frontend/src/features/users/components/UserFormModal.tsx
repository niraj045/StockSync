import { useEffect, useState } from 'react';
import { Modal, Form, Input, Checkbox, Alert } from 'antd';
import { apiClient } from '../../../api/client';
import { User } from '../../auth/context/AuthContext';

interface UserFormModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  userToEdit?: User | null;
}

const AVAILABLE_ROLES = [
  { label: 'Admin', value: 'ROLE_ADMIN' },
  { label: 'Operations', value: 'ROLE_OPERATIONS' },
  { label: 'Accounts', value: 'ROLE_ACCOUNTS' },
  { label: 'Viewer', value: 'ROLE_VIEWER' },
];

export function UserFormModal({ visible, onCancel, onSuccess, userToEdit }: UserFormModalProps) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      if (userToEdit) {
        form.setFieldsValue({
          fullName: userToEdit.fullName,
          username: userToEdit.username,
          email: userToEdit.email,
          roles: userToEdit.roles,
        });
      } else {
        form.resetFields();
      }
      setErrorMsg(null);
    }
  }, [visible, userToEdit, form]);

  const onFinish = async (values: any) => {
    setLoading(true);
    setErrorMsg(null);
    try {
      if (userToEdit) {
        // Sequentially execute both updates to honor optimistic locking version increments
        // 1. Update basic user details
        const detailsRes = await apiClient.put(`/users/${userToEdit.id}`, {
          fullName: values.fullName,
          email: values.email,
          version: userToEdit.version,
        });

        const nextVersion = detailsRes.data.version;

        // 2. Update user roles using the new version returned by the previous request
        await apiClient.put(`/users/${userToEdit.id}/roles`, {
          roles: values.roles,
          version: nextVersion,
        });

      } else {
        // Create user
        await apiClient.post('/users', {
          fullName: values.fullName,
          username: values.username,
          email: values.email,
          password: values.password,
          roles: values.roles,
        });
      }
      onSuccess();
    } catch (err: any) {
      const code = err.response?.data?.code;
      const msg = err.response?.data?.message;

      if (code === 'USERNAME_ALREADY_EXISTS') {
        form.setFields([
          { name: 'username', errors: ['Username already conflicts with an existing email or user'] }
        ]);
      } else if (code === 'EMAIL_ALREADY_EXISTS') {
        form.setFields([
          { name: 'email', errors: ['Email already conflicts with an existing username or user'] }
        ]);
      } else if (code === 'OPTIMISTIC_LOCK_CONFLICT') {
        setErrorMsg('This user has been modified by another administrator. Please close this modal and refresh the list.');
      } else if (msg) {
        setErrorMsg(msg);
      } else {
        setErrorMsg('An unexpected error occurred. Please verify your connection.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={visible}
      title={userToEdit ? 'Edit User' : 'Create User'}
      okText={userToEdit ? 'Save Changes' : 'Create'}
      cancelText="Cancel"
      onCancel={onCancel}
      confirmLoading={loading}
      onOk={() => form.submit()}
      destroyOnClose
      width={500}
    >
      {errorMsg && (
        <Alert
          message={errorMsg}
          type="error"
          showIcon
          style={{ marginBottom: 16, marginTop: 8 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        name="user_form"
        style={{ marginTop: 16 }}
      >
        <Form.Item
          name="fullName"
          label="Full Name"
          rules={[
            { required: true, message: 'Please enter full name' },
            { max: 100, message: 'Full name must not exceed 100 characters' }
          ]}
        >
          <Input placeholder="Enter full name" />
        </Form.Item>

        <Form.Item
          name="username"
          label="Username"
          rules={[
            { required: true, message: 'Please enter username' },
            { min: 3, message: 'Username must be at least 3 characters' },
            { max: 50, message: 'Username must not exceed 50 characters' }
          ]}
        >
          <Input placeholder="Enter username" disabled={!!userToEdit} />
        </Form.Item>

        <Form.Item
          name="email"
          label="Email Address"
          rules={[
            { required: true, message: 'Please enter email address' },
            { type: 'email', message: 'Please enter a valid email address' },
            { max: 100, message: 'Email must not exceed 100 characters' }
          ]}
        >
          <Input placeholder="Enter email address" disabled={!!userToEdit} />
        </Form.Item>

        {!userToEdit && (
          <Form.Item
            name="password"
            label="Password"
            rules={[
              { required: true, message: 'Please enter a temporary password' },
              { min: 8, message: 'Password must be at least 8 characters long' },
              {
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
                message: 'Password must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number',
              },
            ]}
          >
            <Input.Password placeholder="Enter temporary password" />
          </Form.Item>
        )}

        <Form.Item
          name="roles"
          label="Assigned Roles"
          rules={[{ required: true, message: 'Please assign at least one role' }]}
        >
          <Checkbox.Group options={AVAILABLE_ROLES} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
