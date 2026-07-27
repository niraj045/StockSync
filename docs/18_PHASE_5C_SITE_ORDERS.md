# Phase 5C — Site Orders

Phase 5C implements recording total material requirements (orders) under active agreements. Orders do not reserve or modify stock.

## Numbering
* Document Type: `SITE_ORDER`
* Format: `ORD/2026-27/0001`
* Sequence is financial-year aware, April-reset, concurrency-safe, atomic, and generated dynamically when the draft order is created.

## Data Model
* **SiteOrder**:
  - `id`, `orderNumber`, `agreementId`, `partyId`, `siteId`, `orderDate`, `status` (`DRAFT`, `CONFIRMED`, `PARTIALLY_FULFILLED`, `FULFILLED`, `CANCELLED`), `notes`.
* **SiteOrderItem**:
  - `id`, `orderId`, `agreementItemId`, `itemId`, snapshots (`itemCodeSnapshot`, `itemNameSnapshot`, `descriptionSnapshot`, `sizeSnapshot`, `unitSnapshot`), `orderedQuantity`, `issuedQuantity` (starts at 0), `remainingQuantity` (stored generated column: `orderedQuantity - issuedQuantity`).

## Business Rules
* Active agreement is required. Expired, closed, or terminated agreements are blocked.
* Party and Site are automatically populated from the selected agreement.
* Order items must correspond to agreement items.
* Ordered quantities must be positive and must not exceed the contracted quantities in the agreement.
* Drafts are editable. Confirmed orders are immutable.
* A confirmed order may be cancelled with a reason only if its issued quantity is zero.
* Stock checks are read-only; insufficient stock does not block order creation or confirmation. No stock is reserved.

## APIs
* `GET    /api/v1/orders`
* `GET    /api/v1/orders/{id}`
* `POST   /api/v1/orders`
* `PUT    /api/v1/orders/{id}`
* `POST   /api/v1/orders/{id}/confirm`
* `POST   /api/v1/orders/{id}/cancel`
* `GET    /api/v1/orders/{id}/pdf`

## Authorization
* **ADMIN**: Full access (Create, edit, confirm, cancel, view, and PDF download).
* **OPERATIONS**: Create, edit draft, confirm, view, and PDF download. Cancellation permitted under the same policy.
* **ACCOUNTS** & **VIEWER**: Read-only access and PDF download.

## Verification
### Backend Tests
* `Phase4IntegrationTest.confirmedOrdersRespectAgreementAllocationAndExposeRemainingQuantity` verifies order creation, limits enforcement, release of limits upon order cancellation, and PDF download.

### Selenium E2E Tests
* `createAndConfirmOrder` in `daily-operation.e2e.mjs` verifies creating a draft order under an active agreement, confirming the order, verifying order status and item quantities in detail view, and downloading the PDF.
