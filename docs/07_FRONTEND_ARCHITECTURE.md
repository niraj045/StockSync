# 7. Frontend Architecture

## 7.1 Stack

- React
- TypeScript
- Vite
- Ant Design
- React Router
- TanStack Query
- Axios
- Day.js
- Recharts

## 7.2 Project Structure

```text
src/
├── app/
│   ├── router/
│   ├── providers/
│   └── config/
├── api/
├── components/
│   ├── common/
│   ├── forms/
│   ├── tables/
│   └── feedback/
├── layouts/
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── inventory/
│   ├── parties/
│   ├── sites/
│   ├── quotations/
│   ├── agreements/
│   ├── challans/
│   ├── billing/
│   ├── payments/
│   └── reports/
├── hooks/
├── types/
├── utils/
└── main.tsx
```

## 7.3 Feature Structure

Example:

```text
features/challans/
├── api/
├── components/
├── pages/
├── hooks/
├── types/
├── schemas/
└── utils/
```

## 7.4 Data Fetching

Use TanStack Query for server state.

Use query keys such as:

```text
['items', filters]
['sites', partyId]
['issued-challans', filters]
['site-summary', siteId]
['monthly-report', siteId, month]
```

Do not duplicate server data into a global state store without need.

## 7.5 Forms

Use Ant Design Form.

Recommended form behaviour:

- Clear field labels
- Required indicators
- Inline validation
- Unsaved-change warning
- Save as draft
- Post action separated from save
- Confirmation before posting
- Disabled editing after posting
- Attachment preview

## 7.6 Tables

All business tables should support where relevant:

- Pagination
- Sorting
- Search
- Date filter
- Party filter
- Site filter
- Status filter
- Export
- Column visibility
- View details
- Edit draft
- Post
- Cancel
- Download PDF

## 7.7 Dashboard UI

Recommended layout:

- Summary cards at top
- Stock status chart
- Recent challans
- Outstanding sites
- Low-stock items
- Current month billing vs payment

## 7.8 Type Safety

Create explicit API types.

Example:

```ts
export interface StockSummary {
  totalInward: number;
  totalOutward: number;
  currentAvailable: number;
  issued: number;
  lost: number;
  hired: number;
  scrapped: number;
}
```

Avoid `any`.

## 7.9 Error Handling

Implement:

- Global API error interceptor
- Friendly error messages
- Field-level validation mapping
- Session-expired handling
- Retry only for safe read requests
- No automatic retry for posting stock or payments

## 7.10 Accessibility and Usability

- Keyboard-accessible forms
- Visible focus
- Sufficient contrast
- Descriptive button labels
- Confirmation for destructive actions
- Consistent spacing
- Consistent status colours
- Responsive desktop-first layout

## 7.11 Implemented Phase 2 and Phase 3 UI

Authenticated routes are lazy loaded. The application includes reusable master-data pages for
categories, items, parties, sites, and vendors; an entity-linked document page; an inventory page
with balance and movement-history tabs; and dashboard stock totals.

Write controls are hidden unless the current role is authorized. Inventory posting creates a new
idempotency key for each submission and TanStack Query does not retry mutations. Business screens
use the established navy, teal, warm-neutral visual system and remain responsive at narrow widths.
