# StockSync Mobile

React Native client for the existing StockSync Spring Boot service. The app includes:

- Session-based login using the existing `/api/v1/auth` endpoints
- Operational dashboard
- Searchable godown and site stock
- Customer and site creation
- Quotation creation, approval, and agreement activation
- Site order creation from active agreements, confirmation, and direct handoff to dispatch
- Issued and receiving challan history with PDF sharing
- Guided issued-challan creation from confirmed site orders
- Material return creation from issued challans or current site balances
- Good, damaged, lost, and administrator-approved extra-return reconciliation
- Runtime server configuration for local networks and deployed environments

The primary mobile workflow is:

```text
Customer -> Site -> Quotation -> Agreement -> Site Order
-> Issued Challan -> Receiving Challan -> Stock and billing reconciliation
```

## Run locally

Start MySQL and the Java backend from the repository root:

```bash
./start.sh
```

Install and start the mobile client:

```bash
cd mobile
npm install
npm start
```

For an Android emulator, use:

```text
http://10.0.2.2:8081/api/v1
```

For a physical Android phone, connect the phone and computer to the same network and use the computer's LAN address:

```text
http://192.168.x.x:8081/api/v1
```

The backend must listen on an interface reachable by the phone. Windows Firewall may ask for permission the first time.

## Ready-to-share local build

The locally signed arm64 test APK is available at:

```text
artifacts/StockSync-1.0.0-arm64.apk
```

This APK is for direct installation on modern Android phones. Its SHA-256 checksum is stored beside it in `StockSync-1.0.0-arm64.apk.sha256`.

## Build a shareable APK

Sign in to Expo once:

```bash
npx eas-cli login
```

Build the internal-distribution APK:

```bash
npm run build:apk
```

EAS prints a download URL when the build completes. That URL or the downloaded `.apk` can be shared directly with testers. The `preview` profile in `eas.json` explicitly builds an APK; the `production` profile builds an Android App Bundle for Play Store submission.

Set `EXPO_PUBLIC_API_URL` in the build environment to the deployed Java API URL for client builds. Testers can also change it in Account > Server connection.
