# StockSync - Comprehensive Project & Architecture Documentation

This document serves as the master overview of the StockSync platform. It outlines the entire scope of the project, including its core modules, business workflows, technical architecture, and document generation strategies.

---

## 1. Project Purpose & Scope
StockSync is a full-scale **ERP and Material Management System** custom-built for **SteelFab Scaffoldings & Engineering Pvt. Ltd.**
Unlike a standard inventory app, StockSync manages the complete lifecycle of scaffolding materials being hired out on a returnable basis. It tracks everything from initial client inquiries and quotes, to site dispatch, returns, damages, and final billing.

---

## 2. Technical Architecture

### Tech Stack
* **Backend API:** Java 21, Spring Boot 3.5.3, Spring Data JPA
* **Frontend Web:** React (Vite), TypeScript, TailwindCSS
* **Mobile App:** React Native, TypeScript (for on-site & warehouse operations)
* **Database:** MySQL 8.4
* **Infrastructure:** Docker & Docker Compose on Ubuntu Linux Server
* **Document Generation:** 
  * Apache POI (DOCX cell manipulation)
  * LibreOffice Headless (DOCX to PDF conversion)
  * Apache POI / SheetJS (Excel Exports)

### Deployment Architecture
The production server (`66.116.253.40`) runs Docker Compose, orchestrating three main containers:
1. `mysql` (Database)
2. `backend` (Spring Boot API + internal LibreOffice installation)
3. `frontend` (Nginx serving built React static files)

---

## 3. Core Modules & Business Features

The system is broken down into several interconnected modules:

### A. CRM & Sales (`/quotation`, `/order`, `/agreement`)
* **Inquiries:** Lead generation and tracking potential client needs.
* **Quotations:** Generating pricing quotes for scaffolding hire.
* **Orders & Agreements:** Converting successful quotes into formal sales orders and binding agreements, locking in the hiring rates and terms.

### B. Client & Site Management (`/party`, `/site`)
* **Parties:** Managing client profiles and contact information.
* **Sites:** Managing specific physical locations where materials are deployed. (A single Party can have multiple active Sites).

### C. Operations & Challans (`/challan`)
This is the logistical heart of the platform.
* **Issued Challans (Delivery):** Dispatches materials to a site. Captures vehicle numbers, driver details, and specific item quantities. Generates a physical tracking number (e.g., `IC/2026-27/0017`).
* **Receiving Challans (Return):** Logs materials coming back from a site. Crucially, it categorizes returns into `Good`, `Damaged`, and `Extra` quantities, with specific remarks for accountability.

### D. Site Operations (`/workflow`, `/inventory`)
Manages edge-cases for materials that don't follow the standard Dispatch-Return flow:
* **Site Transfers:** Moving inventory directly from Site A to Site B without returning to the warehouse.
* **Stock Losses / Damages:** Registering materials that were permanently lost, stolen, or destroyed on-site.
* **Item Exchanges:** Swapping faulty items for fresh ones directly at the site.

### E. Accounting & Billing (`/billing`, `/gst`, `/payment`)
* **Billing Cycles:** Generates invoices based on the hired quantities and time duration (e.g., Quantity Billing, Sq.Ft Billing).
* **GST Exports:** Generates monthly Excel reports formatting all financial transactions for tax compliance and accounting software imports.
* **Payments:** Tracks client payments against generated invoices.

---

## 4. Document Generation Engine (PDF & Excel)

StockSync heavily relies on generating physical, print-ready documents for signatures and compliance.

### The PDF Workflow
Instead of hardcoding PDF layouts (which is rigid and hard to style), StockSync uses a **Template-Stamping Approach**:
1. **Templates:** Designed in Microsoft Word (`.docx`) and stored in `src/main/resources/pdf-templates/`.
2. **Stamping (Apache POI):** The Java backend opens the DOCX file and dynamically writes text into specific table cells, expanding rows as needed for items.
3. **Conversion (LibreOffice):** The backend triggers a headless LibreOffice process (`libreoffice --headless --convert-to pdf`) to perfectly render the DOCX into a high-quality PDF.

### Types of Generated Documents
* **Delivery Challans (`IC`)** & **Return Challans (`RC`)**: Generated via `SteelFabChallanTemplateStamper`. Includes Client details, Item tables, Vehicle info, Terms & Conditions, and Signature boxes for the driver and receiver.
* **Quotations / Agreements:** E.g., `steelfab-exact-hire-v1.pdf`.
* **Billing Formats:** Formats for Quantity-based and Sq.Ft-based billing (`steelfab-quantity-bill-format.pdf`).
* **Excel Reports:** GST exports and inventory ledgers are generated dynamically and downloaded via the Web/Mobile app.

---

## 5. Mobile Application Structure

The React Native application provides portable access to the ERP.
* **Primary Users:** Warehouse operators loading trucks, and field agents visiting sites.
* **Key Screens:** 
  * `CreateIssuedChallanScreen` / `CreateReceivingChallanScreen`: Allows operators to build challans while walking around the warehouse.
  * `SiteOperationsScreen`: Quick access to log damages or transfers on the go.
  * `GstExportScreen` / `ExcelReportsScreen`: Accessible via the scrollable `More` tab for admins to trigger exports remotely.
