import DateTimePicker, { DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { Ionicons } from '@expo/vector-icons';
import { PropsWithChildren, ReactNode, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardTypeOptions,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  ScrollView,
  Switch,
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
  error,
  keyboardType,
  ...props
}: TextInputProps & { label: string; error?: string; keyboardType?: KeyboardTypeOptions }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        {...props}
        keyboardType={keyboardType}
        placeholderTextColor="#98A2B3"
        style={[styles.input, error && styles.inputError, props.multiline && styles.inputMultiline, props.style]}
      />
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
    </View>
  );
}

type SelectOption = string | { value: string; label: string };
export function SelectField({ label, value, options, onChange }: { label: string; value: string; options: SelectOption[]; onChange: (value: string) => void }) {
  const [open, setOpen] = useState(false);
  const normalized = options.map((option) => typeof option === 'string' ? { value: option, label: option.replaceAll('_', ' ') } : option);
  const selected = normalized.find((option) => option.value === value);
  return <View style={styles.field}>
    <Text style={styles.label}>{label}</Text>
    <Pressable accessibilityRole="button" accessibilityLabel={`Choose ${label}`} onPress={() => setOpen(true)} style={styles.dateButton}>
      <Text style={[styles.dateValue, !selected && styles.datePlaceholder]}>{selected?.label ?? 'Select'}</Text>
      <Ionicons name="chevron-down" size={18} color={colors.muted} />
    </Pressable>
    <Modal transparent animationType="slide" visible={open} onRequestClose={() => setOpen(false)}>
      <View style={styles.selectBackdrop}><View style={styles.selectSheet}>
        <View style={styles.dateSheetHeader}><Text style={styles.dateSheetTitle}>{label}</Text><Pressable onPress={() => setOpen(false)}><Ionicons name="close" size={24} color={colors.ink} /></Pressable></View>
        <ScrollView>{normalized.map((option) => <Pressable key={option.value} style={styles.selectOption} onPress={() => { onChange(option.value); setOpen(false); }}><Text style={[styles.dateValue, option.value === value && styles.selectOptionActive]}>{option.label}</Text></Pressable>)}</ScrollView>
      </View></View>
    </Modal>
  </View>;
}

export function ToggleField({ label, value, onChange }: { label: string; value: boolean; onChange: (value: boolean) => void }) {
  return <View style={styles.toggleRow}><Text style={styles.label}>{label}</Text><Switch value={value} onValueChange={onChange} trackColor={{ true: colors.primarySoft }} thumbColor={value ? colors.primary : '#98A2B3'} /></View>;
}

type DateFieldProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  optional?: boolean;
  minimumDate?: Date;
  maximumDate?: Date;
  selection?: 'date' | 'month';
};

const parsePickerDate = (value: string, selection: 'date' | 'month') => {
  const match = selection === 'month'
    ? /^(\d{4})-(\d{2})$/.exec(value)
    : /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return new Date();
  return new Date(Number(match[1]), Number(match[2]) - 1, selection === 'month' ? 1 : Number(match[3]), 12);
};

const pickerValue = (value: Date, selection: 'date' | 'month') => {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  if (selection === 'month') return `${year}-${month}`;
  return `${year}-${month}-${String(value.getDate()).padStart(2, '0')}`;
};

const pickerLabel = (value: string, selection: 'date' | 'month') => {
  if (!value) return selection === 'month' ? 'Select month' : 'Select date';
  return parsePickerDate(value, selection).toLocaleDateString('en-IN', selection === 'month'
    ? { month: 'long', year: 'numeric' }
    : { day: '2-digit', month: 'short', year: 'numeric' });
};

export function DateField({
  label,
  value,
  onChange,
  optional = false,
  minimumDate,
  maximumDate,
  selection = 'date',
}: DateFieldProps) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(() => parsePickerDate(value, selection));

  const openPicker = () => {
    setDraft(parsePickerDate(value, selection));
    setOpen(true);
  };
  const selectAndroidDate = (event: DateTimePickerEvent, selected?: Date) => {
    setOpen(false);
    if (event.type === 'set' && selected) onChange(pickerValue(selected, selection));
  };

  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.dateRow}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`Choose ${label}`}
          onPress={openPicker}
          style={({ pressed }) => [styles.dateButton, pressed && styles.dateButtonPressed]}
        >
          <Ionicons name="calendar-outline" size={20} color={colors.primary} />
          <Text style={[styles.dateValue, !value && styles.datePlaceholder]}>{pickerLabel(value, selection)}</Text>
          <Ionicons name="chevron-down" size={18} color={colors.muted} />
        </Pressable>
        {optional && value ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Clear ${label}`}
            onPress={() => onChange('')}
            style={({ pressed }) => [styles.clearDate, pressed && styles.dateButtonPressed]}
          >
            <Ionicons name="close" size={21} color={colors.muted} />
          </Pressable>
        ) : null}
      </View>

      {open && Platform.OS === 'android' ? (
        <DateTimePicker
          value={draft}
          mode="date"
          display="calendar"
          minimumDate={minimumDate}
          maximumDate={maximumDate}
          onChange={selectAndroidDate}
        />
      ) : null}

      <Modal transparent animationType="fade" visible={open && Platform.OS === 'ios'} onRequestClose={() => setOpen(false)}>
        <View style={styles.dateBackdrop}>
          <View style={styles.dateSheet}>
            <View style={styles.dateSheetHeader}>
              <Pressable onPress={() => setOpen(false)}><Text style={styles.dateSheetCancel}>Cancel</Text></Pressable>
              <Text style={styles.dateSheetTitle}>{label}</Text>
              <Pressable onPress={() => { onChange(pickerValue(draft, selection)); setOpen(false); }}>
                <Text style={styles.dateSheetDone}>Done</Text>
              </Pressable>
            </View>
            <DateTimePicker
              value={draft}
              mode="date"
              display="inline"
              minimumDate={minimumDate}
              maximumDate={maximumDate}
              onChange={(_, selected) => selected && setDraft(selected)}
            />
          </View>
        </View>
      </Modal>
    </View>
  );
}

export function StatusPill({ value }: { value: string }) {
  const positive = ['POSTED', 'ACTIVE', 'CONFIRMED', 'FULFILLED'].includes(value);
  const warning = ['DRAFT', 'PARTIALLY_FULFILLED', 'PENDING_EXTRA_APPROVAL'].includes(value);
  const info = ['SENT', 'INWARD', 'RECEIVED'].includes(value);
  const hired = ['ISSUED', 'HIRED', 'OUTWARD'].includes(value);
  const danger = ['OVERDUE', 'LOST', 'REJECTED', 'TERMINATED'].includes(value);
  return (
    <View style={[styles.pill, positive ? styles.pillPositive : warning ? styles.pillWarning : info ? styles.pillInfo
      : hired ? styles.pillHired : danger ? styles.pillDanger : styles.pillNeutral]}>
      <Text style={[styles.pillText, positive ? styles.pillPositiveText : warning ? styles.pillWarningText : info ? styles.pillInfoText
        : hired ? styles.pillHiredText : danger ? styles.pillDangerText : null]}>
        {value.replaceAll('_', ' ')}
      </Text>
    </View>
  );
}

export function LoadingBlock() {
  return (
    <View style={styles.center}>
      <ActivityIndicator size="large" color={colors.primary} />
      <Text style={styles.muted}>Loading SteelFab...</Text>
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
  card: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, borderRadius: 8, padding: 16, ...shadow },
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
  buttonSecondaryText: { color: colors.primary },
  field: { gap: 7 },
  toggleRow: { minHeight: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  selectBackdrop: { flex: 1, backgroundColor: 'rgba(4,25,23,0.45)', justifyContent: 'flex-end' },
  selectSheet: { maxHeight: '72%', backgroundColor: colors.surface, borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingBottom: 24 },
  selectOption: { minHeight: 54, justifyContent: 'center', paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line },
  selectOptionActive: { color: colors.primary, fontFamily: fonts.bold },
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
  inputError: { borderColor: colors.red, backgroundColor: colors.redSoft },
  errorText: { color: colors.red, fontSize: 12, fontFamily: fonts.semiBold },
  dateRow: { flexDirection: 'row', gap: 8 },
  dateButton: {
    minHeight: 50,
    flex: 1,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingHorizontal: 14,
    backgroundColor: '#fff',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  dateButtonPressed: { opacity: 0.72 },
  dateValue: { flex: 1, color: colors.ink, fontSize: 16 },
  datePlaceholder: { color: '#98A2B3' },
  clearDate: {
    width: 50,
    minHeight: 50,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    backgroundColor: '#fff',
  },
  dateBackdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.42)' },
  dateSheet: { backgroundColor: '#fff', paddingBottom: 24, borderTopLeftRadius: 12, borderTopRightRadius: 12 },
  dateSheetHeader: { minHeight: 58, paddingHorizontal: 18, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line },
  dateSheetTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: 16 },
  dateSheetCancel: { color: colors.muted, fontFamily: fonts.semiBold },
  dateSheetDone: { color: colors.primary, fontFamily: fonts.bold },
  pill: { borderRadius: 8, paddingHorizontal: 9, paddingVertical: 5, alignSelf: 'flex-start' },
  pillPositive: { backgroundColor: colors.greenSoft },
  pillWarning: { backgroundColor: colors.amberSoft },
  pillInfo: { backgroundColor: colors.blueSoft },
  pillHired: { backgroundColor: colors.purpleSoft },
  pillDanger: { backgroundColor: colors.redSoft },
  pillNeutral: { backgroundColor: colors.neutralSoft },
  pillText: { color: colors.muted, fontFamily: fonts.bold, fontSize: 10 },
  pillPositiveText: { color: colors.green },
  pillWarningText: { color: colors.amber },
  pillInfoText: { color: colors.blue },
  pillHiredText: { color: colors.purple },
  pillDangerText: { color: colors.red },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 },
  muted: { color: colors.muted, lineHeight: 21, textAlign: 'center' },
  empty: { alignItems: 'center', padding: 28, gap: 6 },
  emptyTitle: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold },
});
