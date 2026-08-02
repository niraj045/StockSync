import { NativeStackScreenProps } from '@react-navigation/native-stack';
import axios from 'axios';
import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';
import { apiClient, setApiUrl } from '../api/client';
import { loadApiUrl, normalizeApiUrl, saveApiUrl } from '../api/config';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';

type Props = NativeStackScreenProps<RootStackParams, 'Settings'>;

export function SettingsScreen({ navigation }: Props) {
  const { reconnect } = useAuth();
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [testing, setTesting] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadApiUrl().then(setUrl);
  }, []);

  const test = async () => {
    setTesting(true);
    setStatus('');
    setError('');
    try {
      const normalized = normalizeApiUrl(url);
      await axios.get(`${normalized}/health`, { timeout: 8_000 });
      setStatus('Connected successfully. The Java service is reachable.');
    } catch (cause) {
      setError(axios.isAxiosError(cause) && !cause.response
        ? 'Cannot reach this address. Confirm the backend is running and both devices are on the same network.'
        : 'The address responded, but it is not a compatible StockSync service.');
    } finally {
      setTesting(false);
    }
  };

  const save = async () => {
    setSaving(true);
    setStatus('');
    setError('');
    try {
      const normalized = await saveApiUrl(url);
      setApiUrl(normalized);
      apiClient.defaults.baseURL = normalized;
      await reconnect();
      setStatus('Server address saved.');
      setTimeout(() => navigation.goBack(), 450);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to save the server address.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
      <View style={styles.content}>
        <Card>
          <Text style={styles.title}>Backend connection</Text>
          <Text style={styles.description}>
            Enter the Java backend address. A physical phone must use the computer's Wi-Fi IP, not 127.0.0.1.
          </Text>
          <View style={styles.form}>
            <Field
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
              label="Server URL"
              onChangeText={setUrl}
              placeholder="http://192.168.1.10:8081/api/v1"
              value={url}
            />
            {status ? <Text style={styles.success}>{status}</Text> : null}
            {error ? <Text style={styles.error}>{error}</Text> : null}
            <AppButton title="Test connection" variant="secondary" onPress={test} loading={testing} />
            <AppButton title="Save server" onPress={save} loading={saving} />
          </View>
        </Card>

        <View style={styles.hint}>
          <Text style={styles.hintTitle}>Android emulator</Text>
          <Text style={styles.hintValue}>http://10.0.2.2:8081/api/v1</Text>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { padding: 18, gap: 14 },
  title: { color: colors.ink, fontSize: 22, fontWeight: '900' },
  description: { color: colors.muted, lineHeight: 22, marginTop: 8 },
  form: { gap: 14, marginTop: 20 },
  success: { color: colors.green, backgroundColor: colors.greenSoft, padding: 11, borderRadius: 7, lineHeight: 19 },
  error: { color: colors.red, backgroundColor: colors.redSoft, padding: 11, borderRadius: 7, lineHeight: 19 },
  hint: { padding: 16 },
  hintTitle: { color: colors.muted, fontSize: 12, fontWeight: '700' },
  hintValue: { color: colors.ink, fontWeight: '800', marginTop: 4 },
});
