import { PropsWithChildren, ReactNode } from 'react';
import {
  ActivityIndicator,
  KeyboardTypeOptions,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
} from 'react-native';
import { colors, fonts, shadow } from '../theme';

export function PageHeader({ eyebrow, title, action }: { eyebrow?: string; title: string; action?: ReactNode }) {
  return (
    <View style={styles.pageHeader}>
      <View style={styles.pageHeaderCopy}>
        {eyebrow ? <Text style={styles.eyebrow}>{eyebrow}</Text> : null}
        <Text style={styles.pageTitle}>{title}</Text>
      </View>
      {action}
    </View>
  );
}

export function Card({ children, style }: PropsWithChildren<{ style?: object }>) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function AppButton({
  title,
  onPress,
  loading,
  disabled,
  variant = 'primary',
  icon,
}: {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
  icon?: ReactNode;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled || loading}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        variant === 'secondary' && styles.buttonSecondary,
        variant === 'danger' && styles.buttonDanger,
        (disabled || loading) && styles.buttonDisabled,
        pressed && styles.buttonPressed,
      ]}
    >
      {loading ? <ActivityIndicator color={variant === 'secondary' ? colors.primary : '#fff'} /> : icon}
      <Text style={[styles.buttonText, variant === 'secondary' && styles.buttonSecondaryText]}>{title}</Text>
    </Pressable>
  );
}

export function Field({
  label,
  keyboardType,
  ...props
}: TextInputProps & { label: string; keyboardType?: KeyboardTypeOptions }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        {...props}
        keyboardType={keyboardType}
        placeholderTextColor="#98A2B3"
        style={[styles.input, props.multiline && styles.inputMultiline, props.style]}
      />
    </View>
  );
}

export function StatusPill({ value }: { value: string }) {
  const positive = ['POSTED', 'ACTIVE', 'CONFIRMED', 'FULFILLED'].includes(value);
  const warning = ['DRAFT', 'PARTIALLY_FULFILLED', 'PENDING_EXTRA_APPROVAL'].includes(value);
  return (
    <View style={[styles.pill, positive ? styles.pillPositive : warning ? styles.pillWarning : styles.pillNeutral]}>
      <Text style={[styles.pillText, positive ? styles.pillPositiveText : warning ? styles.pillWarningText : null]}>
        {value.replaceAll('_', ' ')}
      </Text>
    </View>
  );
}

export function LoadingBlock() {
  return (
    <View style={styles.center}>
      <ActivityIndicator size="large" color={colors.primary} />
      <Text style={styles.muted}>Loading StockSync...</Text>
    </View>
  );
}

export function EmptyBlock({ title, message }: { title: string; message: string }) {
  return (
    <View style={styles.empty}>
      <Text style={styles.emptyTitle}>{title}</Text>
      <Text style={styles.muted}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  pageHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 },
  pageHeaderCopy: { flex: 1, paddingRight: 12 },
  eyebrow: { color: colors.muted, fontSize: 11, fontFamily: fonts.bold, textTransform: 'uppercase', marginBottom: 4 },
  pageTitle: { color: colors.ink, fontSize: 28, lineHeight: 34, fontFamily: fonts.extraBold },
  card: { backgroundColor: colors.surface, borderWidth: 1, borderColor: '#E8EBEE', borderRadius: 8, padding: 16, ...shadow },
  button: {
    minHeight: 48,
    paddingHorizontal: 18,
    borderRadius: 8,
    backgroundColor: colors.primary,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  buttonSecondary: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line },
  buttonDanger: { backgroundColor: colors.red },
  buttonDisabled: { opacity: 0.5 },
  buttonPressed: { opacity: 0.82 },
  buttonText: { color: '#fff', fontSize: 15, fontFamily: fonts.bold },
  buttonSecondaryText: { color: colors.ink },
  field: { gap: 7 },
  label: { color: colors.ink, fontSize: 13, fontFamily: fonts.semiBold },
  input: {
    minHeight: 50,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingHorizontal: 14,
    backgroundColor: '#fff',
    color: colors.ink,
    fontSize: 16,
  },
  inputMultiline: { minHeight: 88, paddingTop: 13, textAlignVertical: 'top' },
  pill: { borderRadius: 8, paddingHorizontal: 9, paddingVertical: 5, alignSelf: 'flex-start' },
  pillPositive: { backgroundColor: colors.greenSoft },
  pillWarning: { backgroundColor: colors.amberSoft },
  pillNeutral: { backgroundColor: '#EEF2F3' },
  pillText: { color: colors.muted, fontFamily: fonts.bold, fontSize: 10 },
  pillPositiveText: { color: colors.green },
  pillWarningText: { color: colors.amber },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 },
  muted: { color: colors.muted, lineHeight: 21, textAlign: 'center' },
  empty: { alignItems: 'center', padding: 28, gap: 6 },
  emptyTitle: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold },
});
