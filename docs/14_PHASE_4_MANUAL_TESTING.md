# Phase 4 Manual Testing

## Prerequisites

1. Start MySQL, the backend with the `local` profile, and the Vite frontend.
2. Sign in as an administrator or operations user.
3. Create an active party, an open site belonging to that party, and at least one active item.

## Quotation

1. Open **Quotations** and create a quotation with one or more item lines.
2. Confirm the calculated subtotal, tax, and grand total.
3. Edit the draft, send it, and confirm a sent quotation can no longer be edited.
4. Approve it and use **Convert** to create an agreement draft.
5. Confirm converting the same quotation again is rejected.
6. Clone a quotation and confirm the clone is a new draft with a new number.

## Agreement

1. As an administrator, open **Agreements → Document templates** and upload a DOCX or PDF under 10 MB.
2. Review the converted agreement's party, site, charges, and item snapshot.
3. Generate the agreement and download the DOCX.
4. Activate the generated agreement.
5. Confirm edits and repeated generation are blocked after the state transition.

## Order

1. Open **Orders** and create a draft against the active agreement.
2. Add quantities within the agreement limit and confirm the order.
3. Create another order whose combined quantity exceeds the agreement.
4. Confirm it is rejected with `AGREEMENT_QUANTITY_EXCEEDED`.
5. Verify ordered, issued, and remaining quantities in order details.
6. Cancel an order before any Phase 5 issues exist and confirm its allocation is released.

## Permission Checks

1. As a viewer, confirm all three pages are readable but write actions are unavailable.
2. Confirm an operations user cannot upload templates or terminate an agreement.
3. As an administrator, confirm important transitions appear in **Audit Logs**.
