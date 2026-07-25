# 1. Project Overview

## 1.1 Project Name

Shuttering Inventory Management System

## 1.2 Project Type

A desktop-friendly business web application for managing shuttering inventory, rental operations, sites, parties, challans, agreements, billing, payments, GST reports, and business reporting.

This is not a mobile application. It should work primarily on desktop and laptop browsers, while remaining usable on tablets.

## 1.3 Business Problem

The client currently manages shuttering materials across a godown, customer sites, and partner businesses. Manual records make it difficult to know:

- How much material has been purchased
- How much has been issued
- How much has been received back
- How much is available
- How much is pending at each site
- Which material has been lost, damaged, hired, or scrapped
- Which challans belong to which site
- How an order was split across multiple dispatches
- How much rent, GST, transport, loading, unloading, or damage charges apply
- How much each party has paid
- What is still outstanding
- Which monthly invoices or reports are missing

The system must create one reliable source of truth.

## 1.4 Primary Users

Initial users:

- Business owner
- Operations staff
- Accountant or billing staff

The client currently expects approximately three active users. All may initially have similar permissions, but separate accounts are preferred for auditability.

## 1.5 Primary Goals

1. Provide real-time stock visibility.
2. Maintain a permanent transaction history.
3. Support site-specific agreements and quotations.
4. Support partial or split issued challans.
5. Record received challans and uploaded documents.
6. Automatically calculate pending site quantities.
7. Generate site-wise monthly PDF reports.
8. Track invoices, payments, TDS, GST, and outstanding balances.
9. Minimize manual stock corrections.
10. Provide reliable backup, audit, and reporting.

## 1.6 Success Criteria

The project is successful when:

- Stock values are always derived from valid business transactions.
- A user can trace every stock change to its source transaction.
- One order can be dispatched using multiple challans.
- Every site shows issued, received, pending, lost, and damaged quantities.
- Monthly site reports can be generated without manual calculation.
- Payments and outstanding balances are visible.
- Uploaded agreement and challan files are easy to find.
- The system survives container restarts without losing database or uploaded files.
- Daily backups are available.

## 1.7 Non-Goals for Initial Release

The initial release will not include:

- Native Android application
- Native iOS application
- Microservices
- Kubernetes
- Kafka
- Redis
- Elasticsearch
- WebSocket-based live collaboration
- AI-based understanding of arbitrary scanned agreements
- Complex warehouse automation
- Barcode or RFID integration
- Public customer portal
- Multi-region deployment

These can be considered later if actual business demand appears.
