import { useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Field } from '../components/ui';
import { colors, fonts, shadow } from '../theme';

export function LoginScreen() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    if (!username.trim() || !password) {
      setError('Enter your username and password.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await login(username.trim(), password);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Sign in failed. Check your credentials.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} automaticallyAdjustKeyboardInsets>
        <View style={styles.brand}>
          <View style={styles.mark}>
            <Ionicons name="layers-outline" size={30} color="#fff" />
          </View>
          <View>
            <Text style={styles.brandName}>StockSync</Text>
            <Text style={styles.brandTag}>SHUTTERING CONTROL</Text>
          </View>
        </View>

        <View style={styles.hero}>
          <Text style={styles.kicker}>SteelFab operations</Text>
          <Text style={styles.companyName}>SteelFab Scaffoldings &amp; Engineering Pvt. Ltd.</Text>
        </View>

        <View style={styles.form}>
          <Text style={styles.formTitle}>Sign in</Text>
          <Field
            autoCapitalize="none"
            autoCorrect={false}
            label="Username or email"
            onChangeText={setUsername}
            returnKeyType="next"
            value={username}
          />
          <View>
            <Field
              autoCapitalize="none"
              label="Password"
              onChangeText={setPassword}
              onSubmitEditing={submit}
              returnKeyType="done"
              secureTextEntry={!passwordVisible}
              value={password}
            />
            <Pressable
              accessibilityLabel={passwordVisible ? 'Hide password' : 'Show password'}
              onPress={() => setPasswordVisible((value) => !value)}
              style={styles.eye}
            >
              <Ionicons name={passwordVisible ? 'eye-off-outline' : 'eye-outline'} size={22} color={colors.muted} />
            </Pressable>
          </View>
          {error ? <Text style={styles.error}>{error}</Text> : null}
          <AppButton title="Sign in securely" onPress={submit} loading={submitting} />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { flexGrow: 1, paddingHorizontal: 22, paddingTop: 66, paddingBottom: 34 },
  brand: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  mark: { width: 50, height: 50, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  brandName: { color: colors.ink, fontSize: 23, fontFamily: fonts.extraBold },
  brandTag: { color: colors.primary, fontSize: 10, fontFamily: fonts.bold },
  hero: { marginTop: 44, marginBottom: 26 },
  kicker: { color: colors.primary, fontSize: 11, fontFamily: fonts.bold, textTransform: 'uppercase' },
  companyName: { color: colors.ink, fontSize: 30, lineHeight: 37, fontFamily: fonts.black, marginTop: 8, maxWidth: 350 },
  form: { backgroundColor: colors.surface, borderRadius: 8, borderWidth: 1, borderColor: colors.line, padding: 20, gap: 16, ...shadow },
  formTitle: { color: colors.ink, fontSize: 22, fontFamily: fonts.bold },
  eye: { position: 'absolute', right: 13, bottom: 14, padding: 3 },
  error: { color: colors.red, lineHeight: 20, fontWeight: '600' },
});
