# StockSync - Project & Architecture Overview

This document provides a high-level overview of the StockSync platform, its features, core business flows, and technical architecture.

## 1. Project Purpose
StockSync is a comprehensive inventory, dispatch, and material management system built specifically for **SteelFab Scaffoldings & Engineering Pvt. Ltd.** It manages the lifecycle of scaffolding materials being hired out to client sites on a returnable basis, tracking dispatches, returns, losses, damages, and transfers.

## 2. Tech Stack
* **Backend API:** Java 21, Spring Boot 3.5.3, Spring Data JPA
* **Frontend Web:** React (Vite), TypeScript
* **Mobile App:** React Native, TypeScript
* **Database:** MySQL 8.4
* **PDF Generation Engine:** Apache POI (DOCX manipulation) + LibreOffice Headless (DOCX to PDF conversion)
* **Deployment:** Docker & Docker Compose (Containerized Architecture)

## 3. Core Features & Business Flows

### A. Issued Challans (Delivery Challans)
* **Purpose:** To dispatch scaffolding materials (e.g., H frames, pipes, bracings) to a client's site.
* **Flow:** Admin or Operator creates an Issued Challan specifying the Client, Site Address, Driver details, Vehicle Number, and a list of dispatched items.
* **Tracking:** Each Issued Challan is assigned a unique tracking number (e.g., `IC/2026-27/0017`). 

### B. Receiving Challans (Return Challans)
* **Purpose:** To log materials that have been returned from a client site back to the warehouse.
* **Flow:** When materials arrive, a Receiving Challan is generated. It can be linked directly to an existing Issued Challan.
* **Item Status:** Upon receiving, materials are categorized into:
  * `Good Returned Quantity`
  * `Damaged Returned Quantity`
  * `Extra Returned Quantity`
* **Remarks:** Item-specific remarks can be added to explain damages or discrepancies.

### C. Other Daily Operations
* **Site Transfers:** Moving materials directly between two client sites without returning to the warehouse.
* **Stock Losses / Damages:** Registering materials that were permanently lost or damaged at a site to adjust global inventory counts.
* **Item Exchanges:** Exchanging faulty items with fresh ones on-site.
* **Inquiries & Site Costs:** Tracking lead generation and operational costs per site.
* **Monthly GST Export:** Exporting tax and billing data for accounting purposes.

---

## 4. PDF Generation Architecture

The platform generates highly formatted, print-ready PDFs for physical signatures at the warehouse and on-site.

### How it Works:
1. **The Template:** We use a standard Microsoft Word document (`steelfab_challan_template.docx`) stored in the backend resources (`src/main/resources/pdf-templates/`).
2. **Data Stamping (Apache POI):** When a user requests a PDF, the backend (`SteelFabChallanTemplateStamper.java`) opens the DOCX file and injects dynamic data into specific table cells (Client Name, Items, Quantities, Terms & Conditions).
3. **PDF Conversion (LibreOffice):** The backend saves a temporary version of the filled `.docx` and triggers a headless `libreoffice` system process to convert it to a `.pdf` file.
4. **Delivery:** The PDF bytes are returned to the frontend/mobile app for download.

### Types of Generated PDFs:
1. **Delivery Challan:**
   * Used for Issued Challans.
   * Header clearly states "Supply of Scaffolding Material On Hire. On Returnable Basis from Site."
   * Contains empty signature blocks for the Driver and Receiver to sign upon delivery.
2. **Return Challan:**
   * Used for Receiving Challans.
   * Visually identical template, but the header changes to "Return Challan" and it logs the returned quantities instead of dispatched quantities.

---

## 5. Mobile Application Highlights
The React Native mobile app is designed for field staff and warehouse operators to manage data on the go.
* **Responsive Layouts:** Uses scrollable views (like the `MoreScreen`) to ensure all features (GST Exports, Site Operations) are accessible on smaller mobile screens.
* **Real-time Syncing:** Communicates directly with the Spring Boot backend to ensure warehouse operators and office admins see the exact same real-time inventory counts.
