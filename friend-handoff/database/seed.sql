-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: shuttering_inventory_e2e
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `shuttering_inventory_e2e`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `shuttering_inventory_e2e` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `shuttering_inventory_e2e`;

--
-- Table structure for table `agreement_item_slabs`
--

DROP TABLE IF EXISTS `agreement_item_slabs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agreement_item_slabs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agreement_item_id` bigint NOT NULL,
  `start_day` int NOT NULL,
  `end_day` int DEFAULT NULL,
  `rate` decimal(19,4) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_agreement_item_slabs_item` (`agreement_item_id`),
  CONSTRAINT `fk_agreement_item_slabs_item` FOREIGN KEY (`agreement_item_id`) REFERENCES `agreement_items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agreement_item_slabs`
--

LOCK TABLES `agreement_item_slabs` WRITE;
/*!40000 ALTER TABLE `agreement_item_slabs` DISABLE KEYS */;
/*!40000 ALTER TABLE `agreement_item_slabs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agreement_items`
--

DROP TABLE IF EXISTS `agreement_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agreement_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agreement_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `agreed_quantity` decimal(19,4) NOT NULL,
  `unit_rate` decimal(19,2) NOT NULL,
  `rental_rate` decimal(19,4) NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_quotation_item_id` bigint DEFAULT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size_snapshot` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_snapshot` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight_snapshot` decimal(19,4) DEFAULT NULL,
  `rental_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_rate` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `weight_rate` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `loss_rate_per_piece` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `loss_rate_per_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `damage_rate` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `sequence_number` int NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0',
  `area` decimal(19,4) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agreement_items_item` (`agreement_id`,`item_id`),
  KEY `fk_agreement_items_item` (`item_id`),
  KEY `idx_agreement_items_source` (`source_quotation_item_id`),
  CONSTRAINT `fk_agreement_item_source` FOREIGN KEY (`source_quotation_item_id`) REFERENCES `quotation_items` (`id`),
  CONSTRAINT `fk_agreement_items_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_agreement_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `ck_agreement_item_rates` CHECK (((`agreed_quantity` > 0) and (`unit_rate` >= 0) and (`rental_rate` >= 0) and (`area_rate` >= 0) and (`weight_rate` >= 0) and (`loss_rate_per_piece` >= 0) and (`loss_rate_per_weight` >= 0) and (`damage_rate` >= 0))),
  CONSTRAINT `ck_agreement_items_values` CHECK (((`agreed_quantity` > 0) and (`unit_rate` >= 0) and (`rental_rate` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agreement_items`
--

LOCK TABLES `agreement_items` WRITE;
/*!40000 ALTER TABLE `agreement_items` DISABLE KEYS */;
INSERT INTO `agreement_items` VALUES (1,1,1,100.0000,35.00,35.0000,'H Frame',1,'MAT-001','H frames','H Frame rental',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,1500.0000,0.0000,500.0000,1,1,NULL),(2,1,4,200.0000,10.00,10.0000,'Bracing',2,'MAT-004','7.5 ft Bracings','7.5 ft Bracing rental',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,250.0000,0.0000,100.0000,2,1,NULL),(3,2,1,10.0000,35.00,35.0000,'H Frame',3,'MAT-001','H frames','H Frame rental',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,1500.0000,0.0000,500.0000,1,1,NULL),(4,2,4,10.0000,10.00,10.0000,'Bracing',4,'MAT-004','7.5 ft Bracings','Bracing rental',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,250.0000,0.0000,100.0000,2,1,NULL),(5,3,1,100.0000,35.00,35.0000,'H Frame',5,'MAT-001','H frames','H Frame',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,1500.0000,0.0000,500.0000,1,1,NULL),(6,3,4,200.0000,10.00,10.0000,'Bracing',6,'MAT-004','7.5 ft Bracings','Bracing',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,250.0000,0.0000,100.0000,2,1,NULL),(7,4,1,10.0000,35.00,35.0000,'H Frame',7,'MAT-001','H frames','H Frame',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,1500.0000,0.0000,500.0000,1,1,NULL),(8,4,4,10.0000,10.00,10.0000,'Bracing',8,'MAT-004','7.5 ft Bracings','Bracing',NULL,'PIECE',NULL,'PER_PIECE_PER_DAY',0.0000,0.0000,250.0000,0.0000,100.0000,2,1,NULL);
/*!40000 ALTER TABLE `agreement_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agreement_templates`
--

DROP TABLE IF EXISTS `agreement_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agreement_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `storage_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agreement_templates_name` (`name`),
  UNIQUE KEY `uk_agreement_templates_stored_filename` (`stored_filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agreement_templates`
--

LOCK TABLES `agreement_templates` WRITE;
/*!40000 ALTER TABLE `agreement_templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `agreement_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agreements`
--

DROP TABLE IF EXISTS `agreements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agreements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agreement_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quotation_id` bigint DEFAULT NULL,
  `template_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `agreement_date` date NOT NULL,
  `effective_date` date NOT NULL,
  `expiry_date` date DEFAULT NULL,
  `rental_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `billing_cycle` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MONTHLY',
  `custom_billing_cycle_days` int DEFAULT NULL,
  `grace_period_days` int NOT NULL DEFAULT '0',
  `minimum_billing_days` int NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `security_deposit` decimal(19,2) NOT NULL DEFAULT '0.00',
  `transport_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `loading_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `unloading_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `terms` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `generated_filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `generated_storage_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `generated_at` timestamp(6) NULL DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_legal_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_trade_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_gstin_snapshot` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_pan_snapshot` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_address_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_state_snapshot` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_contact_snapshot` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `site_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_address_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `site_contact_snapshot` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quotation_number_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quotation_date_snapshot` date DEFAULT NULL,
  `quotation_approved_at_snapshot` timestamp(6) NULL DEFAULT NULL,
  `subtotal` decimal(19,2) NOT NULL DEFAULT '0.00',
  `discount_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `taxable_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `cgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `sgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `igst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `total_tax` decimal(19,2) NOT NULL DEFAULT '0.00',
  `other_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `round_off` decimal(19,2) NOT NULL DEFAULT '0.00',
  `grand_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `generated_document_attachment_id` bigint DEFAULT NULL,
  `ready_for_review_at` timestamp(6) NULL DEFAULT NULL,
  `ready_for_review_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activated_at` timestamp(6) NULL DEFAULT NULL,
  `activated_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expired_at` timestamp(6) NULL DEFAULT NULL,
  `expired_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `termination_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `terminated_at` timestamp(6) NULL DEFAULT NULL,
  `terminated_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `closed_at` timestamp(6) NULL DEFAULT NULL,
  `closed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `billing_start_rule` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ISSUE_DATE_INCLUDED',
  `billing_end_rule` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RETURN_DATE_EXCLUDED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agreements_number` (`agreement_number`),
  UNIQUE KEY `uk_agreements_quotation` (`quotation_id`),
  KEY `fk_agreements_template` (`template_id`),
  KEY `fk_agreements_site` (`site_id`),
  KEY `idx_agreements_party_site` (`party_id`,`site_id`),
  KEY `idx_agreements_status_date` (`status`,`effective_date`),
  KEY `fk_agreement_generated_attachment` (`generated_document_attachment_id`),
  KEY `idx_agreements_quotation` (`quotation_id`),
  KEY `idx_agreements_expiry` (`expiry_date`),
  KEY `idx_agreements_status_site` (`status`,`site_id`),
  CONSTRAINT `fk_agreement_generated_attachment` FOREIGN KEY (`generated_document_attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_agreements_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_agreements_quotation` FOREIGN KEY (`quotation_id`) REFERENCES `quotations` (`id`),
  CONSTRAINT `fk_agreements_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `fk_agreements_template` FOREIGN KEY (`template_id`) REFERENCES `agreement_templates` (`id`),
  CONSTRAINT `ck_agreements_amounts` CHECK (((`security_deposit` >= 0) and (`subtotal` >= 0) and (`discount_amount` >= 0) and (`taxable_amount` >= 0) and (`cgst_amount` >= 0) and (`sgst_amount` >= 0) and (`igst_amount` >= 0) and (`total_tax` >= 0) and (`transport_charge` >= 0) and (`loading_charge` >= 0) and (`unloading_charge` >= 0) and (`other_charge` >= 0) and (`grand_total` >= 0))),
  CONSTRAINT `ck_agreements_billing` CHECK (((`billing_cycle` in (_utf8mb4'WEEKLY',_utf8mb4'MONTHLY',_utf8mb4'CUSTOM')) and ((`billing_cycle` <> _utf8mb4'CUSTOM') or (`custom_billing_cycle_days` > 0)) and (`grace_period_days` >= 0) and (`minimum_billing_days` >= 0))),
  CONSTRAINT `ck_agreements_dates` CHECK ((((`expiry_date` is null) or (`expiry_date` >= `effective_date`)) and ((`expiry_date` is null) or (`agreement_date` <= `expiry_date`)))),
  CONSTRAINT `ck_agreements_rental_type` CHECK ((`rental_type` in (_utf8mb4'PER_PIECE_PER_DAY',_utf8mb4'PLATE_AREA_PER_DAY',_utf8mb4'SCAFFOLD_AREA_PER_DAY',_utf8mb4'PLOT_AREA_PER_DAY',_utf8mb4'FIXED_RATE',_utf8mb4'SLAB_BASED'))),
  CONSTRAINT `ck_agreements_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'READY_FOR_REVIEW',_utf8mb4'ACTIVE',_utf8mb4'EXPIRED',_utf8mb4'TERMINATED',_utf8mb4'CLOSED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agreements`
--

LOCK TABLES `agreements` WRITE;
/*!40000 ALTER TABLE `agreements` DISABLE KEYS */;
INSERT INTO `agreements` VALUES (1,'AGR/2026-27/0001',1,NULL,17,17,'2026-08-01','2026-08-01','2027-07-31','PER_PIECE_PER_DAY','MONTHLY',NULL,2,7,'ACTIVE',25000.00,1200.00,300.00,0.00,'Rental billed on actual movements with seven-day minimum.','E2E primary active agreement','agreement-AGR-2026-27-0001.pdf','agreements/1/9bedb02b-895c-4f5c-82dd-3aa2ac03bd9e.pdf','2026-07-28 00:10:57.982964',4,'2026-07-28 00:10:57.745568','admin','2026-07-28 00:10:58.050334','admin','E2E ABC Construction Pvt Ltd 20260728','ABC Construction E2E','27ABCDE1234F1Z5','ABCDE1234F','Wakad, Pune, Maharashtra','MAHARASHTRA','Ravi Patil / 9876543210 / e2e.abc@example.test','E2E Wakad Commercial Project 20260728','E2E-WAKAD-20260728','Wakad, Pune','Site Manager Wakad','QT/2026-27/0001','2026-08-01','2026-07-28 00:10:15.190707',5500.00,0.00,7100.00,639.00,639.00,0.00,1278.00,100.00,0.00,8378.00,1,'2026-07-28 00:10:58.023167','admin','2026-07-28 00:10:58.046108','admin',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'ISSUE_DATE_INCLUDED','RETURN_DATE_EXCLUDED'),(2,'AGR/2026-27/0002',2,NULL,17,18,'2026-08-01','2026-08-01','2027-07-31','PER_PIECE_PER_DAY','MONTHLY',NULL,2,7,'ACTIVE',10000.00,0.00,0.00,0.00,'Destination E2E agreement','Baner active agreement','agreement-AGR-2026-27-0002.pdf','agreements/2/3d58bb67-53de-4984-9a55-77c4a6278a73.pdf','2026-07-28 00:11:21.094688',4,'2026-07-28 00:11:20.933219','admin','2026-07-28 00:11:21.163409','admin','E2E ABC Construction Pvt Ltd 20260728','ABC Construction E2E','27ABCDE1234F1Z5','ABCDE1234F','Wakad, Pune, Maharashtra','MAHARASHTRA','Ravi Patil / 9876543210 / e2e.abc@example.test','E2E Baner Residential Project 20260728','E2E-BANER-20260728','Baner, Pune','Site Manager Baner','QT/2026-27/0002','2026-08-01','2026-07-28 00:11:20.909670',450.00,0.00,450.00,40.50,40.50,0.00,81.00,0.00,0.00,531.00,2,'2026-07-28 00:11:21.129822','admin','2026-07-28 00:11:21.157143','admin',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'ISSUE_DATE_INCLUDED','RETURN_DATE_EXCLUDED'),(3,'AGR/2026-27/0003',3,NULL,17,19,'2026-07-01','2026-07-01','2027-06-30','PER_PIECE_PER_DAY','MONTHLY',NULL,0,7,'ACTIVE',25000.00,1200.00,300.00,0.00,'Historical E2E billing agreement','Billing segmentation','agreement-AGR-2026-27-0003.pdf','agreements/3/a92902f0-9cc4-40ba-9f86-83e104404596.pdf','2026-07-28 00:17:28.629359',4,'2026-07-28 00:17:28.416270','admin','2026-07-28 00:17:28.689368','admin','E2E ABC Construction Pvt Ltd 20260728','ABC Construction E2E','27ABCDE1234F1Z5','ABCDE1234F','Wakad, Pune, Maharashtra','MAHARASHTRA','Ravi Patil / 9876543210 / e2e.abc@example.test','E2E Historical Wakad Billing 20260728','E2E-HIST-WAKAD','Wakad Pune','Billing Test Manager','QT/2026-27/0003','2026-07-01','2026-07-28 00:17:28.383817',5500.00,0.00,7100.00,639.00,639.00,0.00,1278.00,100.00,0.00,8378.00,3,'2026-07-28 00:17:28.654253','admin','2026-07-28 00:17:28.682676','admin',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'ISSUE_DATE_INCLUDED','RETURN_DATE_EXCLUDED'),(4,'AGR/2026-27/0004',4,NULL,17,20,'2026-07-01','2026-07-01','2027-06-30','PER_PIECE_PER_DAY','MONTHLY',NULL,0,7,'ACTIVE',25000.00,1200.00,300.00,0.00,'Historical E2E billing agreement','Billing segmentation','agreement-AGR-2026-27-0004.pdf','agreements/4/4699c67c-11ea-46c8-8936-0e484bea0be9.pdf','2026-07-28 00:17:29.044330',4,'2026-07-28 00:17:28.777038','admin','2026-07-28 00:17:29.116650','admin','E2E ABC Construction Pvt Ltd 20260728','ABC Construction E2E','27ABCDE1234F1Z5','ABCDE1234F','Wakad, Pune, Maharashtra','MAHARASHTRA','Ravi Patil / 9876543210 / e2e.abc@example.test','E2E Historical Baner Billing 20260728','E2E-HIST-BANER','Baner Pune','Billing Transfer Manager','QT/2026-27/0004','2026-07-01','2026-07-28 00:17:28.748799',450.00,0.00,2050.00,184.50,184.50,0.00,369.00,100.00,0.00,2419.00,4,'2026-07-28 00:17:29.067980','admin','2026-07-28 00:17:29.095397','admin',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'ISSUE_DATE_INCLUDED','RETURN_DATE_EXCLUDED');
/*!40000 ALTER TABLE `agreements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_run_charges`
--

DROP TABLE IF EXISTS `billing_run_charges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_run_charges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `billing_run_id` bigint NOT NULL,
  `source_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint NOT NULL,
  `source_document_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `charge_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` decimal(19,4) NOT NULL DEFAULT '1.0000',
  `rate` decimal(19,4) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `taxable` tinyint(1) NOT NULL DEFAULT '1',
  `selected` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_billing_run_charges_run` (`billing_run_id`),
  CONSTRAINT `fk_billing_run_charges_run` FOREIGN KEY (`billing_run_id`) REFERENCES `billing_runs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_run_charges`
--

LOCK TABLES `billing_run_charges` WRITE;
/*!40000 ALTER TABLE `billing_run_charges` DISABLE KEYS */;
INSERT INTO `billing_run_charges` VALUES (3,2,'STOCK_LOSS_RECOVERY',3,'LOSS/2026-27/0003','LOSS_RECOVERY','Loss Recovery Charge for H frames (LOSS/2026-27/0003)',1.0000,1500.0000,1500.00,1,1),(4,2,'STOCK_DAMAGE_RECOVERY',4,'DMG/2026-27/0004','DAMAGE_RECOVERY','Damage Recovery for H frames (DMG/2026-27/0004)',1.0000,500.0000,500.00,1,1);
/*!40000 ALTER TABLE `billing_run_charges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_run_segments`
--

DROP TABLE IF EXISTS `billing_run_segments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_run_segments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `billing_run_id` bigint NOT NULL,
  `agreement_item_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_snapshot` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight_snapshot` decimal(19,4) DEFAULT NULL,
  `source_issue_reference` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_end_reference` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rental_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `area` decimal(19,4) DEFAULT NULL,
  `weight` decimal(19,4) DEFAULT NULL,
  `segment_start` date NOT NULL,
  `segment_end` date NOT NULL,
  `billable_days` int NOT NULL,
  `base_rate` decimal(19,4) NOT NULL,
  `applied_slab_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(19,2) NOT NULL,
  `calculation_explanation` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sequence_number` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_billing_run_segments_run` (`billing_run_id`),
  KEY `fk_billing_run_segments_agreement_item` (`agreement_item_id`),
  KEY `fk_billing_run_segments_item` (`item_id`),
  CONSTRAINT `fk_billing_run_segments_agreement_item` FOREIGN KEY (`agreement_item_id`) REFERENCES `agreement_items` (`id`),
  CONSTRAINT `fk_billing_run_segments_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_billing_run_segments_run` FOREIGN KEY (`billing_run_id`) REFERENCES `billing_runs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_run_segments`
--

LOCK TABLES `billing_run_segments` WRITE;
/*!40000 ALTER TABLE `billing_run_segments` DISABLE KEYS */;
INSERT INTO `billing_run_segments` VALUES (7,1,5,1,'MAT-001','H frames',NULL,'PIECE',NULL,'ISSUED_CHALLAN:3 (IC/2026-27/0003)','RECEIVING_CHALLAN:2 (RC/2026-27/0002)','PER_PIECE_PER_DAY',60.0000,NULL,NULL,'2026-07-01','2026-07-09',9,35.0000,NULL,18900.00,'Rental for 60.0000 PIECE @ 35.0000/day for 9 days',1),(8,1,5,1,'MAT-001','H frames',NULL,'PIECE',NULL,'ISSUED_CHALLAN:4 (IC/2026-27/0004)','RECEIVING_CHALLAN:2 (RC/2026-27/0002)','PER_PIECE_PER_DAY',35.0000,NULL,NULL,'2026-07-02','2026-07-09',8,35.0000,NULL,9800.00,'Rental for 35.0000 PIECE @ 35.0000/day for 8 days',2),(9,1,5,1,'MAT-001','H frames',NULL,'PIECE',NULL,'ISSUED_CHALLAN:4 (IC/2026-27/0004)','SITE_TRANSFER_OUT:2 (ST/2026-27/0002)','PER_PIECE_PER_DAY',3.0000,NULL,NULL,'2026-07-02','2026-07-11',10,35.0000,NULL,1050.00,'Rental for 3.0000 PIECE @ 35.0000/day for 10 days',3),(10,1,5,1,'MAT-001','H frames',NULL,'PIECE',NULL,'ISSUED_CHALLAN:4 (IC/2026-27/0004)',NULL,'PER_PIECE_PER_DAY',2.0000,NULL,NULL,'2026-07-02','2026-07-28',27,35.0000,NULL,1890.00,'Rental for 2.0000 PIECE @ 35.0000/day for 27 days',4),(11,1,6,4,'MAT-004','7.5 ft Bracings',NULL,'PIECE',NULL,'ISSUED_CHALLAN:3 (IC/2026-27/0003)',NULL,'PER_PIECE_PER_DAY',120.0000,NULL,NULL,'2026-07-01','2026-07-28',28,10.0000,NULL,33600.00,'Rental for 120.0000 PIECE @ 10.0000/day for 28 days',5),(12,1,6,4,'MAT-004','7.5 ft Bracings',NULL,'PIECE',NULL,'ISSUED_CHALLAN:4 (IC/2026-27/0004)',NULL,'PER_PIECE_PER_DAY',80.0000,NULL,NULL,'2026-07-02','2026-07-28',27,10.0000,NULL,21600.00,'Rental for 80.0000 PIECE @ 10.0000/day for 27 days',6),(16,2,7,1,'MAT-001','H frames',NULL,'PIECE',NULL,'SITE_TRANSFER_IN:2 (ST/2026-27/0002)','STOCK_LOSS:3 (LOSS/2026-27/0003)','PER_PIECE_PER_DAY',1.0000,NULL,NULL,'2026-07-12','2026-07-18',7,35.0000,NULL,245.00,'Rental for 1.0000 PIECE @ 35.0000/day for 7 days',1),(17,2,7,1,'MAT-001','H frames',NULL,'PIECE',NULL,'SITE_TRANSFER_IN:2 (ST/2026-27/0002)','STOCK_DAMAGE:4 (DMG/2026-27/0004)','PER_PIECE_PER_DAY',1.0000,NULL,NULL,'2026-07-12','2026-07-18',7,35.0000,NULL,245.00,'Rental for 1.0000 PIECE @ 35.0000/day for 7 days',2),(18,2,7,1,'MAT-001','H frames',NULL,'PIECE',NULL,'SITE_TRANSFER_IN:2 (ST/2026-27/0002)',NULL,'PER_PIECE_PER_DAY',1.0000,NULL,NULL,'2026-07-12','2026-07-28',17,35.0000,NULL,595.00,'Rental for 1.0000 PIECE @ 35.0000/day for 17 days',3);
/*!40000 ALTER TABLE `billing_run_segments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_runs`
--

DROP TABLE IF EXISTS `billing_runs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_runs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `billing_run_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agreement_id` bigint NOT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rental_subtotal` decimal(19,2) NOT NULL DEFAULT '0.00',
  `loss_charge_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `damage_charge_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `operational_charge_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `manual_adjustment_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `discount_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE',
  `discount_value` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `discount_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `taxable_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `cgst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `cgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `sgst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `sgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `igst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `igst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `total_tax` decimal(19,2) NOT NULL DEFAULT '0.00',
  `round_off` decimal(19,2) NOT NULL DEFAULT '0.00',
  `grand_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `calculated_at` timestamp(6) NULL DEFAULT NULL,
  `calculated_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `finalized_at` timestamp(6) NULL DEFAULT NULL,
  `finalized_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_billing_runs_number` (`billing_run_number`),
  KEY `fk_billing_runs_agreement` (`agreement_id`),
  KEY `fk_billing_runs_party` (`party_id`),
  KEY `fk_billing_runs_site` (`site_id`),
  KEY `idx_billing_runs_period_status` (`period_start`,`period_end`,`status`),
  CONSTRAINT `fk_billing_runs_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_billing_runs_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_billing_runs_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_runs`
--

LOCK TABLES `billing_runs` WRITE;
/*!40000 ALTER TABLE `billing_runs` DISABLE KEYS */;
INSERT INTO `billing_runs` VALUES (1,'BR/2026-27/0001',3,17,19,'2026-07-01','2026-07-28','FINALIZED',86840.00,0.00,0.00,0.00,0.00,'NONE',0.0000,0.00,86840.00,0.0000,0.00,0.0000,0.00,18.0000,15631.20,15631.20,-0.20,102471.00,'2026-07-28 00:18:22.349091','admin','2026-07-28 00:18:22.394611','admin',NULL,NULL,NULL,'2026-07-28 00:18:21.982261','admin','2026-07-28 00:18:22.403592','admin',3),(2,'BR/2026-27/0002',4,17,20,'2026-07-12','2026-07-28','CALCULATED',1085.00,1500.00,500.00,0.00,0.00,'NONE',0.0000,0.00,3085.00,0.0000,0.00,0.0000,0.00,18.0000,555.30,555.30,-0.30,3640.00,'2026-07-28 00:20:20.630520','admin',NULL,NULL,NULL,NULL,NULL,'2026-07-28 00:20:20.468988','admin','2026-07-28 00:20:20.640675','admin',2);
/*!40000 ALTER TABLE `billing_runs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `billing_source_allocations`
--

DROP TABLE IF EXISTS `billing_source_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `billing_source_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agreement_id` bigint NOT NULL,
  `source_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint NOT NULL,
  `billing_run_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_allocation` (`source_type`,`source_id`),
  KEY `fk_billing_allocations_agreement` (`agreement_id`),
  KEY `fk_billing_allocations_run` (`billing_run_id`),
  CONSTRAINT `fk_billing_allocations_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_billing_allocations_run` FOREIGN KEY (`billing_run_id`) REFERENCES `billing_runs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `billing_source_allocations`
--

LOCK TABLES `billing_source_allocations` WRITE;
/*!40000 ALTER TABLE `billing_source_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `billing_source_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `damage_records`
--

DROP TABLE IF EXISTS `damage_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `damage_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `damage_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_receiving_challan_id` bigint DEFAULT NULL,
  `source_receiving_challan_item_id` bigint DEFAULT NULL,
  `agreement_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `damage_date` date NOT NULL,
  `quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `repairable` tinyint(1) NOT NULL DEFAULT '1',
  `damage_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `condition_notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `charge_method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `damage_rate` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `calculated_damage_amount` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `estimated_repair_cost` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `actual_repair_cost` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `attachment_id` bigint DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recorded_at` timestamp(6) NULL DEFAULT NULL,
  `recorded_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `repair_started_at` timestamp(6) NULL DEFAULT NULL,
  `repair_started_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `repaired_at` timestamp(6) NULL DEFAULT NULL,
  `repaired_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scrapped_at` timestamp(6) NULL DEFAULT NULL,
  `scrapped_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversed_at` timestamp(6) NULL DEFAULT NULL,
  `reversed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversal_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_damage_records_number` (`damage_number`),
  KEY `fk_damage_records_receiving` (`source_receiving_challan_id`),
  KEY `fk_damage_records_receiving_item` (`source_receiving_challan_item_id`),
  KEY `fk_damage_records_agreement` (`agreement_id`),
  KEY `fk_damage_records_party` (`party_id`),
  KEY `fk_damage_records_item` (`item_id`),
  KEY `fk_damage_records_attachment` (`attachment_id`),
  KEY `idx_damage_records_status` (`status`),
  KEY `idx_damage_records_date` (`damage_date`),
  KEY `idx_damage_records_site_item` (`site_id`,`item_id`),
  CONSTRAINT `fk_damage_records_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_damage_records_attachment` FOREIGN KEY (`attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_damage_records_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_damage_records_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_damage_records_receiving` FOREIGN KEY (`source_receiving_challan_id`) REFERENCES `receiving_challans` (`id`),
  CONSTRAINT `fk_damage_records_receiving_item` FOREIGN KEY (`source_receiving_challan_item_id`) REFERENCES `receiving_challan_items` (`id`),
  CONSTRAINT `fk_damage_records_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `damage_records`
--

LOCK TABLES `damage_records` WRITE;
/*!40000 ALTER TABLE `damage_records` DISABLE KEYS */;
INSERT INTO `damage_records` VALUES (1,'DMG/2026-27/0001','RECEIVING_CHALLAN',1,1,1,17,17,1,'2026-08-10',10.0000,0.0000,1,'REPAIRABLE','Originating from Receiving Challan RC/2026-27/0001','PER_PIECE',500.0000,5000.0000,0.0000,1200.0000,NULL,'REPAIRED','2026-07-28 00:13:30.256977',NULL,'2026-07-28 00:15:03.882143','admin','2026-07-28 00:15:03.960560','admin',NULL,NULL,NULL,NULL,NULL,2,'2026-07-28 00:13:30.258632','admin','2026-07-28 00:15:03.968482','admin'),(2,'DMG/2026-27/0002','MANUAL_SITE_DECLARATION',NULL,NULL,1,17,17,4,'2026-08-11',2.0000,0.0000,0,'NON_REPAIRABLE','Two bent bracings selected for scrap validation','PER_PIECE',100.0000,200.0000,0.0000,0.0000,NULL,'SCRAPPED','2026-07-28 00:15:34.853128','admin',NULL,NULL,NULL,NULL,'2026-07-28 00:15:34.885188','admin',NULL,NULL,NULL,2,'2026-07-28 00:15:34.797574','admin','2026-07-28 00:15:34.955532','admin'),(3,'DMG/2026-27/0003','RECEIVING_CHALLAN',2,2,3,17,19,1,'2026-07-10',10.0000,0.0000,1,'REPAIRABLE','Originating from Receiving Challan RC/2026-27/0002','PER_PIECE',500.0000,5000.0000,0.0000,0.0000,NULL,'RECORDED','2026-07-28 00:17:55.873253',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'2026-07-28 00:17:55.874580','admin','2026-07-28 00:17:55.874580','admin'),(4,'DMG/2026-27/0004','MANUAL_SITE_DECLARATION',NULL,NULL,4,17,20,1,'2026-07-16',1.0000,0.0000,0,'NON_REPAIRABLE','Historical destination damage recovery test','PER_PIECE',500.0000,500.0000,0.0000,0.0000,NULL,'RECORDED','2026-07-28 00:20:20.432538','admin',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-07-28 00:20:20.346296','admin','2026-07-28 00:20:20.439561','admin');
/*!40000 ALTER TABLE `damage_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deposit_invoice_allocations`
--

DROP TABLE IF EXISTS `deposit_invoice_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deposit_invoice_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `deposit_transaction_id` bigint NOT NULL,
  `invoice_id` bigint NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deposit_invoice_allocation` (`deposit_transaction_id`,`invoice_id`),
  KEY `idx_deposit_invoice_allocations_invoice` (`invoice_id`),
  CONSTRAINT `fk_deposit_invoice_allocations_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`),
  CONSTRAINT `fk_deposit_invoice_allocations_transaction` FOREIGN KEY (`deposit_transaction_id`) REFERENCES `security_deposit_transactions` (`id`),
  CONSTRAINT `ck_deposit_invoice_allocations_amount` CHECK ((`amount` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deposit_invoice_allocations`
--

LOCK TABLES `deposit_invoice_allocations` WRITE;
/*!40000 ALTER TABLE `deposit_invoice_allocations` DISABLE KEYS */;
INSERT INTO `deposit_invoice_allocations` VALUES (1,2,1,5000.00,'2026-07-28 00:19:24.074836','admin','2026-07-28 00:19:24.074836','admin',0);
/*!40000 ALTER TABLE `deposit_invoice_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_number_sequences`
--

DROP TABLE IF EXISTS `document_number_sequences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_number_sequences` (
  `document_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `financial_year` varchar(9) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prefix` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_number` bigint NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0',
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`document_type`,`financial_year`),
  CONSTRAINT `ck_document_sequence_number` CHECK ((`last_number` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_number_sequences`
--

LOCK TABLES `document_number_sequences` WRITE;
/*!40000 ALTER TABLE `document_number_sequences` DISABLE KEYS */;
INSERT INTO `document_number_sequences` VALUES ('AGREEMENT','2026-27','AGR',4,3,'2026-07-28 05:47:28.776147'),('BILLING_RUN','2026-27','BR',2,1,'2026-07-28 05:50:20.467533'),('INVOICE','2026-27','INV',1,0,'2026-07-28 05:48:22.432430'),('ISSUED_CHALLAN','2026-27','IC',4,3,'2026-07-28 05:47:54.478748'),('PAYMENT_RECEIPT','2026-27','PR',2,1,'2026-07-28 05:49:24.131441'),('QUOTATION','2026-27','QT',4,3,'2026-07-28 05:47:28.293935'),('RECEIVING_CHALLAN','2026-27','RC',2,1,'2026-07-28 05:47:54.522958'),('SECURITY_DEPOSIT','2026-27','SD',2,1,'2026-07-28 05:49:24.072644'),('SITE_ORDER','2026-27','ORD',3,2,'2026-07-28 05:57:39.384446'),('SITE_TRANSFER','2026-27','ST',2,1,'2026-07-28 05:47:55.969061'),('STOCK_DAMAGE','2026-27','DMG',4,3,'2026-07-28 05:50:20.343563'),('STOCK_LOSS','2026-27','LOSS',3,2,'2026-07-28 05:50:20.181663');
/*!40000 ALTER TABLE `document_number_sequences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `file_attachments`
--

DROP TABLE IF EXISTS `file_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` bigint NOT NULL,
  `document_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `storage_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `uploaded_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_attachments_stored_name` (`stored_filename`),
  KEY `idx_file_attachments_entity` (`entity_type`,`entity_id`),
  CONSTRAINT `ck_file_attachments_entity_type` CHECK ((`entity_type` in (_utf8mb4'PARTY',_utf8mb4'SITE',_utf8mb4'VENDOR',_utf8mb4'ITEM',_utf8mb4'AGREEMENT',_utf8mb4'INVOICE',_utf8mb4'PAYMENT_RECEIPT',_utf8mb4'SECURITY_DEPOSIT')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_attachments`
--

LOCK TABLES `file_attachments` WRITE;
/*!40000 ALTER TABLE `file_attachments` DISABLE KEYS */;
INSERT INTO `file_attachments` VALUES (1,'AGREEMENT',1,'AGREEMENT_PDF','agreement-AGR-2026-27-0001.pdf','9bedb02b-895c-4f5c-82dd-3aa2ac03bd9e.pdf','application/pdf',4664,'agreements/1/9bedb02b-895c-4f5c-82dd-3aa2ac03bd9e.pdf','Generated agreement PDF','admin','2026-07-28 00:10:57.981030'),(2,'AGREEMENT',2,'AGREEMENT_PDF','agreement-AGR-2026-27-0002.pdf','3d58bb67-53de-4984-9a55-77c4a6278a73.pdf','application/pdf',4603,'agreements/2/3d58bb67-53de-4984-9a55-77c4a6278a73.pdf','Generated agreement PDF','admin','2026-07-28 00:11:21.092483'),(3,'AGREEMENT',3,'AGREEMENT_PDF','agreement-AGR-2026-27-0003.pdf','a92902f0-9cc4-40ba-9f86-83e104404596.pdf','application/pdf',4603,'agreements/3/a92902f0-9cc4-40ba-9f86-83e104404596.pdf','Generated agreement PDF','admin','2026-07-28 00:17:28.627928'),(4,'AGREEMENT',4,'AGREEMENT_PDF','agreement-AGR-2026-27-0004.pdf','4699c67c-11ea-46c8-8936-0e484bea0be9.pdf','application/pdf',4610,'agreements/4/4699c67c-11ea-46c8-8936-0e484bea0be9.pdf','Generated agreement PDF','admin','2026-07-28 00:17:29.042952'),(5,'INVOICE',1,'INVOICE_PDF','invoice-INV-2026-27-0001.pdf','5a78adf1-a695-479d-aa95-82cb12be27b8.pdf','application/pdf',5395,'invoices/1/5a78adf1-a695-479d-aa95-82cb12be27b8.pdf','System generated tax invoice PDF','admin','2026-07-28 00:18:22.546204'),(6,'PAYMENT_RECEIPT',1,'PAYMENT_RECEIPT_PDF','payment-receipt-PR-2026-27-0001.pdf','c0da0be7-682c-4129-9d91-a43426a0c3a6.pdf','application/pdf',2779,'payment-receipts/1/c0da0be7-682c-4129-9d91-a43426a0c3a6.pdf','System generated payment receipt PDF','admin','2026-07-28 00:19:23.388113');
/*!40000 ALTER TABLE `file_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','baseline','SQL','V1__baseline.sql',538686102,'inventory_user','2026-07-28 05:37:32',2,1),(2,'2','auth and users','SQL','V2__auth_and_users.sql',-443922332,'inventory_user','2026-07-28 05:37:32',806,1),(3,'3','master data','SQL','V3__master_data.sql',647974245,'inventory_user','2026-07-28 05:37:33',1047,1),(4,'4','inventory core','SQL','V4__inventory_core.sql',-425515988,'inventory_user','2026-07-28 05:37:35',1177,1),(5,'5','agreements and orders','SQL','V5__agreements_and_orders.sql',-1337384626,'inventory_user','2026-07-28 05:37:36',1165,1),(6,'6','opening stock import','SQL','V6__opening_stock_import.sql',-1870593496,'inventory_user','2026-07-28 05:37:37',1103,1),(7,'7','nullable imported item minimum stock','SQL','V7__nullable_imported_item_minimum_stock.sql',1347365059,'inventory_user','2026-07-28 05:37:37',250,1),(8,'8','phase 5a quotation management','SQL','V8__phase_5a_quotation_management.sql',320279301,'inventory_user','2026-07-28 05:37:38',1021,1),(9,'9','quotation party site snapshots','SQL','V9__quotation_party_site_snapshots.sql',1000309943,'inventory_user','2026-07-28 05:37:39',637,1),(10,'10','phase 5b agreements','SQL','V10__phase_5b_agreements.sql',-1988644142,'inventory_user','2026-07-28 05:37:40',1251,1),(11,'11','allow agreement attachments','SQL','V11__allow_agreement_attachments.sql',-975478470,'inventory_user','2026-07-28 05:37:40',204,1),(12,'12','site stock balances and issued challans','SQL','V12__site_stock_balances_and_issued_challans.sql',196738521,'inventory_user','2026-07-28 05:37:41',621,1),(13,'13','receiving challans','SQL','V13__receiving_challans.sql',-710866098,'inventory_user','2026-07-28 05:37:41',339,1),(14,'14','stock exceptions and site transfers','SQL','V14__stock_exceptions_and_site_transfers.sql',-1658032665,'inventory_user','2026-07-28 05:37:43',1875,1),(15,'15','rental billing and invoice management','SQL','V15__rental_billing_and_invoice_management.sql',-979812367,'inventory_user','2026-07-28 05:37:48',5004,1),(16,'16','payments tds deposits outstanding','SQL','V16__payments_tds_deposits_outstanding.sql',1247589530,'inventory_user','2026-07-28 05:37:50',1792,1),(17,'17','reports gst analytics','SQL','V17__reports_gst_analytics.sql',1394952753,'inventory_user','2026-07-28 05:37:51',855,1),(18,'18','dashboard scale validation indexes','SQL','V18__dashboard_scale_validation_indexes.sql',-1686600947,'inventory_user','2026-07-28 05:37:51',257,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gst_export_config_versions`
--

DROP TABLE IF EXISTS `gst_export_config_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gst_export_config_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `format_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version_label` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mapping_json` json NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gst_export_config_version` (`format_code`,`version_label`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gst_export_config_versions`
--

LOCK TABLES `gst_export_config_versions` WRITE;
/*!40000 ALTER TABLE `gst_export_config_versions` DISABLE KEYS */;
INSERT INTO `gst_export_config_versions` VALUES (1,'GSTR1_PREPARATION','v1','{\"cgst\": \"cgst_amount\", \"igst\": \"igst_amount\", \"sgst\": \"sgst_amount\", \"invoiceDate\": \"invoice_date\", \"invoiceType\": \"derived_b2b_b2c\", \"customerName\": \"party_legal_name_snapshot\", \"invoiceValue\": \"grand_total\", \"taxableValue\": \"taxable_amount\", \"customerGstin\": \"party_gstin_snapshot\", \"invoiceNumber\": \"invoice_number\", \"placeOfSupply\": \"party_state_snapshot\", \"supplierGstin\": \"company_gstin_snapshot\"}',1,'2026-07-28 05:37:50.772780','migration');
/*!40000 ALTER TABLE `gst_export_config_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoice_items`
--

DROP TABLE IF EXISTS `invoice_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  `line_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agreement_item_id` bigint DEFAULT NULL,
  `source_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_document_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_snapshot` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `area` decimal(19,4) DEFAULT NULL,
  `weight` decimal(19,4) DEFAULT NULL,
  `billable_days` int DEFAULT NULL,
  `rate` decimal(19,4) NOT NULL,
  `taxable` tinyint(1) NOT NULL DEFAULT '1',
  `amount` decimal(19,2) NOT NULL,
  `sequence_number` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_invoice_items_invoice` (`invoice_id`),
  CONSTRAINT `fk_invoice_items_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoice_items`
--

LOCK TABLES `invoice_items` WRITE;
/*!40000 ALTER TABLE `invoice_items` DISABLE KEYS */;
INSERT INTO `invoice_items` VALUES (1,1,'RENTAL',5,NULL,NULL,NULL,1,'MAT-001','H frames',NULL,'PIECE','Rental for 60.0000 PIECE @ 35.0000/day for 9 days',60.0000,NULL,NULL,9,35.0000,1,18900.00,1),(2,1,'RENTAL',5,NULL,NULL,NULL,1,'MAT-001','H frames',NULL,'PIECE','Rental for 35.0000 PIECE @ 35.0000/day for 8 days',35.0000,NULL,NULL,8,35.0000,1,9800.00,2),(3,1,'RENTAL',5,NULL,NULL,NULL,1,'MAT-001','H frames',NULL,'PIECE','Rental for 3.0000 PIECE @ 35.0000/day for 10 days',3.0000,NULL,NULL,10,35.0000,1,1050.00,3),(4,1,'RENTAL',5,NULL,NULL,NULL,1,'MAT-001','H frames',NULL,'PIECE','Rental for 2.0000 PIECE @ 35.0000/day for 27 days',2.0000,NULL,NULL,27,35.0000,1,1890.00,4),(5,1,'RENTAL',6,NULL,NULL,NULL,4,'MAT-004','7.5 ft Bracings',NULL,'PIECE','Rental for 120.0000 PIECE @ 10.0000/day for 28 days',120.0000,NULL,NULL,28,10.0000,1,33600.00,5),(6,1,'RENTAL',6,NULL,NULL,NULL,4,'MAT-004','7.5 ft Bracings',NULL,'PIECE','Rental for 80.0000 PIECE @ 10.0000/day for 27 days',80.0000,NULL,NULL,27,10.0000,1,21600.00,6);
/*!40000 ALTER TABLE `invoice_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoices`
--

DROP TABLE IF EXISTS `invoices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `billing_run_id` bigint NOT NULL,
  `agreement_id` bigint NOT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `invoice_date` date NOT NULL,
  `due_date` date NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `company_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `company_address_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `company_gstin_snapshot` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_legal_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_gstin_snapshot` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_pan_snapshot` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_address_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_state_snapshot` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_address_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_contact_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agreement_number_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subtotal` decimal(19,2) NOT NULL DEFAULT '0.00',
  `discount_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `taxable_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `cgst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `cgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `sgst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `sgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `igst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `igst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `total_tax` decimal(19,2) NOT NULL DEFAULT '0.00',
  `round_off` decimal(19,2) NOT NULL DEFAULT '0.00',
  `grand_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `terms` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `generated_pdf_attachment_id` bigint DEFAULT NULL,
  `issued_at` timestamp(6) NULL DEFAULT NULL,
  `issued_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `cash_allocated_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `tds_allocated_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `deposit_adjusted_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `outstanding_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoices_number` (`invoice_number`),
  UNIQUE KEY `uk_invoices_run` (`billing_run_id`),
  KEY `fk_invoices_agreement` (`agreement_id`),
  KEY `fk_invoices_pdf` (`generated_pdf_attachment_id`),
  KEY `idx_invoices_outstanding_party` (`party_id`,`status`,`outstanding_amount`),
  KEY `idx_invoices_outstanding_site` (`site_id`,`status`,`outstanding_amount`),
  KEY `idx_invoices_date_status` (`invoice_date`,`status`),
  KEY `idx_dashboard_invoices_due_status` (`due_date`,`status`),
  CONSTRAINT `fk_invoices_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_invoices_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_invoices_pdf` FOREIGN KEY (`generated_pdf_attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_invoices_run` FOREIGN KEY (`billing_run_id`) REFERENCES `billing_runs` (`id`),
  CONSTRAINT `fk_invoices_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoices`
--

LOCK TABLES `invoices` WRITE;
/*!40000 ALTER TABLE `invoices` DISABLE KEYS */;
INSERT INTO `invoices` VALUES (1,'INV/2026-27/0001',1,3,17,19,'2026-07-28','2026-08-27','2026-07-01','2026-07-28','ISSUED','StockSync Test Company','Pune, Maharashtra','27AAAAA1111A1Z1','E2E ABC Construction Pvt Ltd 20260728','27ABCDE1234F1Z5','ABCDE1234F','Wakad, Pune, Maharashtra','MAHARASHTRA','E2E Historical Wakad Billing 20260728','E2E-HIST-WAKAD','Wakad Pune','Billing Test Manager','AGR/2026-27/0003',86840.00,0.00,86840.00,0.0000,0.00,0.0000,0.00,18.0000,15631.20,15631.20,-0.20,102471.00,'Historical E2E billing agreement',NULL,5,'2026-07-28 00:18:22.565934','admin',NULL,NULL,NULL,'2026-07-28 00:18:22.439101','admin','2026-07-28 00:19:24.284042','admin',6,40000.00,2000.00,5000.00,55471.00);
/*!40000 ALTER TABLE `invoices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issued_challan_items`
--

DROP TABLE IF EXISTS `issued_challan_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issued_challan_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `issued_challan_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_snapshot` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_issued_challan_items_challan` (`issued_challan_id`),
  KEY `fk_issued_challan_items_item` (`item_id`),
  CONSTRAINT `fk_issued_challan_items_challan` FOREIGN KEY (`issued_challan_id`) REFERENCES `issued_challans` (`id`),
  CONSTRAINT `fk_issued_challan_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `ck_issued_challan_items_qty` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issued_challan_items`
--

LOCK TABLES `issued_challan_items` WRITE;
/*!40000 ALTER TABLE `issued_challan_items` DISABLE KEYS */;
INSERT INTO `issued_challan_items` VALUES (1,1,1,60.0000,'MAT-001','H frames','PIECE'),(2,1,4,120.0000,'MAT-004','7.5 ft Bracings','PIECE'),(3,2,1,40.0000,'MAT-001','H frames','PIECE'),(4,2,4,80.0000,'MAT-004','7.5 ft Bracings','PIECE'),(5,3,1,60.0000,'MAT-001','H frames','PIECE'),(6,3,4,120.0000,'MAT-004','7.5 ft Bracings','PIECE'),(7,4,1,40.0000,'MAT-001','H frames','PIECE'),(8,4,4,80.0000,'MAT-004','7.5 ft Bracings','PIECE');
/*!40000 ALTER TABLE `issued_challan_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issued_challans`
--

DROP TABLE IF EXISTS `issued_challans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issued_challans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `challan_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_order_id` bigint NOT NULL,
  `dispatch_date` date NOT NULL,
  `vehicle_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `transport_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `loading_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `unloading_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_issued_challans_number` (`challan_number`),
  KEY `idx_issued_challans_dispatch` (`dispatch_date`),
  KEY `idx_dashboard_issued_order_date` (`site_order_id`,`dispatch_date`),
  CONSTRAINT `fk_issued_challans_order` FOREIGN KEY (`site_order_id`) REFERENCES `site_orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issued_challans`
--

LOCK TABLES `issued_challans` WRITE;
/*!40000 ALTER TABLE `issued_challans` DISABLE KEYS */;
INSERT INTO `issued_challans` VALUES (1,'IC/2026-27/0001',1,'2026-08-01','MH12E2E01','E2E Driver One','Partial E2E issue','admin','2026-07-28 00:11:54.222026',0.00,0.00,0.00),(2,'IC/2026-27/0002',1,'2026-08-02','MH12E2E02','E2E Driver Two','Final E2E issue','admin','2026-07-28 00:11:54.410350',0.00,0.00,0.00),(3,'IC/2026-27/0003',2,'2026-07-01','MH12HIST1','Historical Driver 1','First split','admin','2026-07-28 00:17:54.429321',0.00,0.00,0.00),(4,'IC/2026-27/0004',2,'2026-07-02','MH12HIST2','Historical Driver 2','Second split','admin','2026-07-28 00:17:54.493305',0.00,0.00,0.00);
/*!40000 ALTER TABLE `issued_challans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `item_aliases`
--

DROP TABLE IF EXISTS `item_aliases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_aliases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint NOT NULL,
  `alias` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_system` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_alias_source` (`alias`,`source_system`),
  KEY `idx_item_aliases_item` (`item_id`),
  CONSTRAINT `fk_item_aliases_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item_aliases`
--

LOCK TABLES `item_aliases` WRITE;
/*!40000 ALTER TABLE `item_aliases` DISABLE KEYS */;
INSERT INTO `item_aliases` VALUES (1,1,'H frames','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:23.774681'),(2,2,'Out size H frame','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:23.934201'),(3,3,'Damage H frame','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:24.081976'),(4,4,'7.5 ft Bracings','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:24.226031'),(5,5,'6ft Bracing','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:24.486169'),(6,6,'7ft Bracing','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:24.631021'),(7,7,'Platforms','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:24.774280'),(8,8,'3m vertical','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:24.927649'),(9,9,'2.5m vertical','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.077988'),(10,10,'2m vertical','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.224256'),(11,11,'1m vertical','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.373509'),(12,12,'1.5m vertical','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.534437'),(13,13,'2m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.680597'),(14,14,'1850m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.825947'),(15,15,'1.5m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:25.977132'),(16,16,'1480m Ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.132733'),(17,17,'1450m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.273550'),(18,18,'1150m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.416275'),(19,19,'1m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.555863'),(20,20,'950m Ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.698297'),(21,21,'980m ledger','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.848956'),(22,22,'20ft pipe','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:26.992171'),(23,23,'10ft pipe','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:27.135838'),(24,24,'8ft pipe','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:27.281966'),(25,25,'U jack','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:27.420653'),(26,26,'Base jack','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:27.562677'),(27,27,'Coupler','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:27.714429'),(28,28,'Spiggot pin','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:27.861871'),(29,29,'Joint pins','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:28.002140'),(30,30,'Props','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:28.163951'),(31,31,'Castor wheel','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:28.317273'),(32,32,'7ft pipe','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:28.457261'),(33,33,'8ft & 10ft  plate pipe','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:28.597051'),(34,34,'10ft chaneel','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:28.740003'),(35,36,'10f ladder','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.015819'),(36,37,'Ladder pipe','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.153077'),(37,38,'Ladder steps','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.291873'),(38,39,'20ft Aluminium Ladder','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.435135'),(39,40,'7.5 ft Iron Ladder','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.572877'),(40,41,'Ladder coupler','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.710805'),(41,42,'Toe board','STEELFAB_STOCK_SNAPSHOT_V1',1,'2026-07-28 00:08:29.847911');
/*!40000 ALTER TABLE `item_aliases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `item_categories`
--

DROP TABLE IF EXISTS `item_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_categories_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item_categories`
--

LOCK TABLES `item_categories` WRITE;
/*!40000 ALTER TABLE `item_categories` DISABLE KEYS */;
INSERT INTO `item_categories` VALUES (1,'SCAFFOLDING','Scaffolding materials imported from legacy opening stock',1,0,'2026-07-28 00:08:23.769525','admin','2026-07-28 00:08:23.769525','admin');
/*!40000 ALTER TABLE `item_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `item_exchange_records`
--

DROP TABLE IF EXISTS `item_exchange_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_exchange_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exchange_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_receiving_challan_id` bigint DEFAULT NULL,
  `source_receiving_challan_item_id` bigint DEFAULT NULL,
  `agreement_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `expected_item_id` bigint NOT NULL,
  `actual_item_id` bigint NOT NULL,
  `exchange_date` date NOT NULL,
  `expected_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `actual_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `expected_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `actual_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `destination_stock_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `posted_at` timestamp(6) NULL DEFAULT NULL,
  `posted_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exchange_records_number` (`exchange_number`),
  KEY `fk_exchange_records_receiving` (`source_receiving_challan_id`),
  KEY `fk_exchange_records_receiving_item` (`source_receiving_challan_item_id`),
  KEY `fk_exchange_records_agreement` (`agreement_id`),
  KEY `fk_exchange_records_party` (`party_id`),
  KEY `fk_exchange_records_expected` (`expected_item_id`),
  KEY `fk_exchange_records_actual` (`actual_item_id`),
  KEY `idx_exchange_records_status` (`status`),
  KEY `idx_exchange_records_site` (`site_id`),
  CONSTRAINT `fk_exchange_records_actual` FOREIGN KEY (`actual_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_exchange_records_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_exchange_records_expected` FOREIGN KEY (`expected_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_exchange_records_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_exchange_records_receiving` FOREIGN KEY (`source_receiving_challan_id`) REFERENCES `receiving_challans` (`id`),
  CONSTRAINT `fk_exchange_records_receiving_item` FOREIGN KEY (`source_receiving_challan_item_id`) REFERENCES `receiving_challan_items` (`id`),
  CONSTRAINT `fk_exchange_records_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item_exchange_records`
--

LOCK TABLES `item_exchange_records` WRITE;
/*!40000 ALTER TABLE `item_exchange_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `item_exchange_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `items`
--

DROP TABLE IF EXISTS `items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `size` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight_per_piece` decimal(19,4) DEFAULT NULL,
  `purchase_value` decimal(19,2) DEFAULT NULL,
  `rental_configuration` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loss_rate` decimal(19,2) DEFAULT NULL,
  `scrap_value` decimal(19,2) DEFAULT NULL,
  `minimum_stock` decimal(19,4) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_items_code` (`item_code`),
  KEY `idx_items_category` (`category_id`),
  KEY `idx_items_active` (`active`),
  KEY `idx_items_name` (`item_name`),
  CONSTRAINT `fk_items_category` FOREIGN KEY (`category_id`) REFERENCES `item_categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `items`
--

LOCK TABLES `items` WRITE;
/*!40000 ALTER TABLE `items` DISABLE KEYS */;
INSERT INTO `items` VALUES (1,'MAT-001','H frames',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:23.770695','admin','2026-07-28 00:08:23.770695','admin'),(2,'MAT-002','Out size H frame',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:23.931809','admin','2026-07-28 00:08:23.931809','admin'),(3,'MAT-003','Damage H frame',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:24.079699','admin','2026-07-28 00:08:24.079699','admin'),(4,'MAT-004','7.5 ft Bracings',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:24.222941','admin','2026-07-28 00:08:24.222941','admin'),(5,'MAT-005','6ft Bracing',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:24.483532','admin','2026-07-28 00:08:24.483532','admin'),(6,'MAT-006','7ft Bracing',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:24.627992','admin','2026-07-28 00:08:24.627992','admin'),(7,'MAT-007','Platforms',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:24.771646','admin','2026-07-28 00:08:24.771646','admin'),(8,'MAT-008','3m vertical',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:24.925354','admin','2026-07-28 00:08:24.925354','admin'),(9,'MAT-009','2.5m vertical',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.075523','admin','2026-07-28 00:08:25.075523','admin'),(10,'MAT-010','2m vertical',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.221832','admin','2026-07-28 00:08:25.221832','admin'),(11,'MAT-011','1m vertical',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.371278','admin','2026-07-28 00:08:25.371278','admin'),(12,'MAT-012','1.5m vertical',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.531388','admin','2026-07-28 00:08:25.531388','admin'),(13,'MAT-013','2m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.678287','admin','2026-07-28 00:08:25.678287','admin'),(14,'MAT-014','1850m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.823901','admin','2026-07-28 00:08:25.823901','admin'),(15,'MAT-015','1.5m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:25.973811','admin','2026-07-28 00:08:25.973811','admin'),(16,'MAT-016','1480m Ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.130780','admin','2026-07-28 00:08:26.130780','admin'),(17,'MAT-017','1450m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.271664','admin','2026-07-28 00:08:26.271664','admin'),(18,'MAT-018','1150m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.414143','admin','2026-07-28 00:08:26.414143','admin'),(19,'MAT-019','1m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.553901','admin','2026-07-28 00:08:26.553901','admin'),(20,'MAT-020','950m Ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.695528','admin','2026-07-28 00:08:26.695528','admin'),(21,'MAT-021','980m ledger',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.846412','admin','2026-07-28 00:08:26.846412','admin'),(22,'MAT-022','20ft pipe',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:26.990095','admin','2026-07-28 00:08:26.990095','admin'),(23,'MAT-023','10ft pipe',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.133810','admin','2026-07-28 00:08:27.133810','admin'),(24,'MAT-024','8ft pipe',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.279761','admin','2026-07-28 00:08:27.279761','admin'),(25,'MAT-025','U jack',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.418888','admin','2026-07-28 00:08:27.418888','admin'),(26,'MAT-026','Base jack',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.559380','admin','2026-07-28 00:08:27.559380','admin'),(27,'MAT-027','Coupler',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.711847','admin','2026-07-28 00:08:27.711847','admin'),(28,'MAT-028','Spiggot pin',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.859878','admin','2026-07-28 00:08:27.859878','admin'),(29,'MAT-029','Joint pins',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:27.998148','admin','2026-07-28 00:08:27.998148','admin'),(30,'MAT-030','Props',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:28.160915','admin','2026-07-28 00:08:28.160915','admin'),(31,'MAT-031','Castor wheel',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:28.315192','admin','2026-07-28 00:08:28.315192','admin'),(32,'MAT-032','7ft pipe',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:28.455320','admin','2026-07-28 00:08:28.455320','admin'),(33,'MAT-033','8ft & 10ft plate pipe',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:28.594677','admin','2026-07-28 00:08:28.594677','admin'),(34,'MAT-034','10ft chaneel',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:28.738217','admin','2026-07-28 00:08:28.738217','admin'),(35,'MAT-035','8ft & 10ft plate pipe (Source Sr 35)',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:28.875770','admin','2026-07-28 00:08:28.875770','admin'),(36,'MAT-036','10f ladder',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.013882','admin','2026-07-28 00:08:29.013882','admin'),(37,'MAT-037','Ladder pipe',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.151403','admin','2026-07-28 00:08:29.151403','admin'),(38,'MAT-038','Ladder steps',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.290233','admin','2026-07-28 00:08:29.290233','admin'),(39,'MAT-039','20ft Aluminium Ladder',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.433399','admin','2026-07-28 00:08:29.433399','admin'),(40,'MAT-040','7.5 ft Iron Ladder',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.571150','admin','2026-07-28 00:08:29.571150','admin'),(41,'MAT-041','Ladder coupler',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.709034','admin','2026-07-28 00:08:29.709034','admin'),(42,'MAT-042','Toe board',1,NULL,'PIECE',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-28 00:08:29.845922','admin','2026-07-28 00:08:29.845922','admin');
/*!40000 ALTER TABLE `items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loss_records`
--

DROP TABLE IF EXISTS `loss_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loss_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loss_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_receiving_challan_id` bigint DEFAULT NULL,
  `source_receiving_challan_item_id` bigint DEFAULT NULL,
  `agreement_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `loss_date` date NOT NULL,
  `quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `charge_method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recovery_rate` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `calculated_recovery_amount` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachment_id` bigint DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approved_at` timestamp(6) NULL DEFAULT NULL,
  `approved_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversed_at` timestamp(6) NULL DEFAULT NULL,
  `reversed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversal_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loss_records_number` (`loss_number`),
  KEY `fk_loss_records_receiving` (`source_receiving_challan_id`),
  KEY `fk_loss_records_receiving_item` (`source_receiving_challan_item_id`),
  KEY `fk_loss_records_agreement` (`agreement_id`),
  KEY `fk_loss_records_party` (`party_id`),
  KEY `fk_loss_records_item` (`item_id`),
  KEY `fk_loss_records_attachment` (`attachment_id`),
  KEY `idx_loss_records_status` (`status`),
  KEY `idx_loss_records_date` (`loss_date`),
  KEY `idx_loss_records_site_item` (`site_id`,`item_id`),
  CONSTRAINT `fk_loss_records_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_loss_records_attachment` FOREIGN KEY (`attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_loss_records_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_loss_records_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_loss_records_receiving` FOREIGN KEY (`source_receiving_challan_id`) REFERENCES `receiving_challans` (`id`),
  CONSTRAINT `fk_loss_records_receiving_item` FOREIGN KEY (`source_receiving_challan_item_id`) REFERENCES `receiving_challan_items` (`id`),
  CONSTRAINT `fk_loss_records_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loss_records`
--

LOCK TABLES `loss_records` WRITE;
/*!40000 ALTER TABLE `loss_records` DISABLE KEYS */;
INSERT INTO `loss_records` VALUES (1,'LOSS/2026-27/0001','RECEIVING_CHALLAN',1,1,1,17,17,1,'2026-08-10',5.0000,0.0000,'PER_PIECE',1500.0000,7500.0000,'Five H Frames remain at Wakad',NULL,'APPROVED','2026-07-28 00:13:30.241360',NULL,NULL,NULL,NULL,0,'2026-07-28 00:13:30.243141','admin','2026-07-28 00:13:30.243141','admin'),(2,'LOSS/2026-27/0002','RECEIVING_CHALLAN',2,2,3,17,19,1,'2026-07-10',5.0000,0.0000,'PER_PIECE',1500.0000,7500.0000,'5 remain',NULL,'APPROVED','2026-07-28 00:17:55.800928',NULL,NULL,NULL,NULL,0,'2026-07-28 00:17:55.802271','admin','2026-07-28 00:17:55.802271','admin'),(3,'LOSS/2026-27/0003','MANUAL_SITE_DECLARATION',NULL,NULL,4,17,20,1,'2026-07-15',1.0000,0.0000,'PER_PIECE',1500.0000,1500.0000,'Historical destination loss recovery test',NULL,'APPROVED','2026-07-28 00:20:20.301138','admin',NULL,NULL,NULL,1,'2026-07-28 00:20:20.184791','admin','2026-07-28 00:20:20.308183','admin');
/*!40000 ALTER TABLE `loss_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parties`
--

DROP TABLE IF EXISTS `parties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parties` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `legal_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trade_name` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gstin` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pan` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parties_gstin` (`gstin`),
  UNIQUE KEY `uk_parties_pan` (`pan`),
  KEY `idx_parties_active` (`active`),
  KEY `idx_parties_legal_name` (`legal_name`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parties`
--

LOCK TABLES `parties` WRITE;
/*!40000 ALTER TABLE `parties` DISABLE KEYS */;
INSERT INTO `parties` VALUES (1,'E2E Client 4m Façade',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:29.970880','admin','2026-07-28 00:08:29.970880','admin'),(2,'E2E Client Ali Designer',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.077501','admin','2026-07-28 00:08:30.077501','admin'),(3,'E2E Client Avighnaa Kandivali',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.186825','admin','2026-07-28 00:08:30.186825','admin'),(4,'E2E Client Engarc',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.289117','admin','2026-07-28 00:08:30.289117','admin'),(5,'E2E Client Imperial',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.397209','admin','2026-07-28 00:08:30.397209','admin'),(6,'E2E Client Noble',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.507387','admin','2026-07-28 00:08:30.507387','admin'),(7,'E2E Client Zeeco Media',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.619972','admin','2026-07-28 00:08:30.619972','admin'),(8,'E2E Client Raymond GS',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.737226','admin','2026-07-28 00:08:30.737226','admin'),(9,'E2E Client Raymond Tenex',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.849448','admin','2026-07-28 00:08:30.849448','admin'),(10,'E2E Client Epilson',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:30.962289','admin','2026-07-28 00:08:30.962289','admin'),(11,'E2E Client SBUT',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:31.075587','admin','2026-07-28 00:08:31.075587','admin'),(12,'E2E Client SK Interior',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:31.205891','admin','2026-07-28 00:08:31.205891','admin'),(13,'E2E Client Sukoon',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:31.326475','admin','2026-07-28 00:08:31.326475','admin'),(14,'E2E Client ANV',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:31.536231','admin','2026-07-28 00:08:31.536231','admin'),(15,'E2E Client Rocks & Logs',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:31.655661','admin','2026-07-28 00:08:31.655661','admin'),(16,'E2E Client Innovator Façade',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Created during legacy opening-stock mapping',1,0,'2026-07-28 00:08:31.774652','admin','2026-07-28 00:08:31.774652','admin'),(17,'E2E ABC Construction Pvt Ltd 20260728','ABC Construction E2E','27ABCDE1234F1Z5','ABCDE1234F','Ravi Patil','9876543210','e2e.abc@example.test','Wakad, Pune, Maharashtra','MAHARASHTRA','Isolated complete E2E validation',1,0,'2026-07-28 00:09:49.435489','admin','2026-07-28 00:09:49.435489','admin');
/*!40000 ALTER TABLE `parties` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_allocations`
--

DROP TABLE IF EXISTS `payment_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_receipt_id` bigint NOT NULL,
  `invoice_id` bigint NOT NULL,
  `cash_allocated` decimal(19,2) NOT NULL DEFAULT '0.00',
  `tds_allocated` decimal(19,2) NOT NULL DEFAULT '0.00',
  `total_allocated` decimal(19,2) NOT NULL DEFAULT '0.00',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_allocation_invoice` (`payment_receipt_id`,`invoice_id`),
  KEY `idx_payment_allocations_invoice` (`invoice_id`),
  CONSTRAINT `fk_payment_allocations_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`),
  CONSTRAINT `fk_payment_allocations_receipt` FOREIGN KEY (`payment_receipt_id`) REFERENCES `payment_receipts` (`id`),
  CONSTRAINT `ck_payment_allocations_amounts` CHECK (((`cash_allocated` >= 0) and (`tds_allocated` >= 0) and (`total_allocated` = (`cash_allocated` + `tds_allocated`))))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_allocations`
--

LOCK TABLES `payment_allocations` WRITE;
/*!40000 ALTER TABLE `payment_allocations` DISABLE KEYS */;
INSERT INTO `payment_allocations` VALUES (1,1,1,40000.00,2000.00,42000.00,'2026-07-28 00:19:23.238572','admin','2026-07-28 00:19:23.238572','admin',0),(2,2,1,1000.00,0.00,1000.00,'2026-07-28 00:19:24.134085','admin','2026-07-28 00:19:24.134085','admin',0);
/*!40000 ALTER TABLE `payment_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_receipts`
--

DROP TABLE IF EXISTS `payment_receipts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_receipts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint DEFAULT NULL,
  `party_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_date` date NOT NULL,
  `payment_mode` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reference_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cheque_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cheque_date` date DEFAULT NULL,
  `cash_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `tds_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `total_settlement_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `unallocated_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachment_id` bigint DEFAULT NULL,
  `posted_at` timestamp(6) NULL DEFAULT NULL,
  `posted_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversed_at` timestamp(6) NULL DEFAULT NULL,
  `reversed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversal_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_receipts_number` (`receipt_number`),
  KEY `fk_payment_receipts_attachment` (`attachment_id`),
  KEY `idx_payment_receipts_party_date` (`party_id`,`payment_date`),
  KEY `idx_payment_receipts_site_status` (`site_id`,`status`),
  KEY `idx_payment_receipts_mode` (`payment_mode`),
  KEY `idx_payment_receipts_date_status` (`payment_date`,`status`),
  KEY `idx_dashboard_payment_status_date` (`status`,`payment_date`),
  CONSTRAINT `fk_payment_receipts_attachment` FOREIGN KEY (`attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_payment_receipts_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_payment_receipts_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `ck_payment_receipts_amounts` CHECK (((`cash_amount` >= 0) and (`tds_amount` >= 0) and (`total_settlement_amount` = (`cash_amount` + `tds_amount`)) and (`unallocated_amount` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipts`
--

LOCK TABLES `payment_receipts` WRITE;
/*!40000 ALTER TABLE `payment_receipts` DISABLE KEYS */;
INSERT INTO `payment_receipts` VALUES (1,'PR/2026-27/0001',17,19,'E2E ABC Construction Pvt Ltd 20260728','E2E Historical Wakad Billing 20260728','2026-07-28','NEFT','E2E-NEFT-20260728','E2E Test Bank',NULL,NULL,40000.00,2000.00,42000.00,0.00,'POSTED','Partial cash plus TDS settlement',6,'2026-07-28 00:19:23.306042','admin',NULL,NULL,NULL,'2026-07-28 00:19:23.235193','admin','2026-07-28 00:19:24.002579','admin',2),(2,'PR/2026-27/0002',17,19,'E2E ABC Construction Pvt Ltd 20260728','E2E Historical Wakad Billing 20260728','2026-07-28','CASH',NULL,NULL,NULL,NULL,1000.00,0.00,1000.00,0.00,'REVERSED','E2E reversal validation',NULL,'2026-07-28 00:19:24.175075','admin','2026-07-28 00:19:24.279879','admin','Controlled E2E payment reversal','2026-07-28 00:19:24.132197','admin','2026-07-28 00:19:24.283987','admin',2);
/*!40000 ALTER TABLE `payment_receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_items`
--

DROP TABLE IF EXISTS `purchase_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `unit_rate` decimal(19,2) NOT NULL,
  `line_value` decimal(19,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_purchase_items_purchase` (`purchase_id`),
  KEY `fk_purchase_items_item` (`item_id`),
  CONSTRAINT `fk_purchase_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_purchase_items_purchase` FOREIGN KEY (`purchase_id`) REFERENCES `purchases` (`id`),
  CONSTRAINT `ck_purchase_items_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `ck_purchase_items_rate` CHECK ((`unit_rate` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_items`
--

LOCK TABLES `purchase_items` WRITE;
/*!40000 ALTER TABLE `purchase_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchase_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchases`
--

DROP TABLE IF EXISTS `purchases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `vendor_id` bigint NOT NULL,
  `purchase_date` date NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_value` decimal(19,2) NOT NULL DEFAULT '0.00',
  `idempotency_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchases_number` (`purchase_number`),
  UNIQUE KEY `uk_purchases_idempotency` (`idempotency_key`),
  KEY `fk_purchases_vendor` (`vendor_id`),
  KEY `idx_purchases_date` (`purchase_date`),
  CONSTRAINT `fk_purchases_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchases`
--

LOCK TABLES `purchases` WRITE;
/*!40000 ALTER TABLE `purchases` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quotation_items`
--

DROP TABLE IF EXISTS `quotation_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quotation_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quotation_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `unit_rate` decimal(19,2) NOT NULL,
  `rental_rate` decimal(19,4) NOT NULL,
  `line_amount` decimal(19,2) NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size_snapshot` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_snapshot` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rental_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area` decimal(19,4) DEFAULT NULL,
  `weight` decimal(19,4) DEFAULT NULL,
  `sequence_number` int NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quotation_items_item` (`quotation_id`,`item_id`),
  KEY `fk_quotation_items_item` (`item_id`),
  CONSTRAINT `fk_quotation_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_quotation_items_quotation` FOREIGN KEY (`quotation_id`) REFERENCES `quotations` (`id`),
  CONSTRAINT `ck_quotation_items_values` CHECK (((`quantity` > 0) and (`unit_rate` >= 0) and (`rental_rate` >= 0) and (`line_amount` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quotation_items`
--

LOCK TABLES `quotation_items` WRITE;
/*!40000 ALTER TABLE `quotation_items` DISABLE KEYS */;
INSERT INTO `quotation_items` VALUES (1,1,1,100.0000,35.00,35.0000,3500.00,NULL,'MAT-001','H frames','H Frame rental',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,1,0),(2,1,4,200.0000,10.00,10.0000,2000.00,NULL,'MAT-004','7.5 ft Bracings','7.5 ft Bracing rental',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,2,0),(3,2,1,10.0000,35.00,35.0000,350.00,NULL,'MAT-001','H frames','H Frame rental',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,1,0),(4,2,4,10.0000,10.00,10.0000,100.00,NULL,'MAT-004','7.5 ft Bracings','Bracing rental',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,2,0),(5,3,1,100.0000,35.00,35.0000,3500.00,NULL,'MAT-001','H frames','H Frame',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,1,0),(6,3,4,200.0000,10.00,10.0000,2000.00,NULL,'MAT-004','7.5 ft Bracings','Bracing',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,2,0),(7,4,1,10.0000,35.00,35.0000,350.00,NULL,'MAT-001','H frames','H Frame',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,1,0),(8,4,4,10.0000,10.00,10.0000,100.00,NULL,'MAT-004','7.5 ft Bracings','Bracing',NULL,'PIECE','PER_PIECE_PER_DAY',NULL,NULL,2,0);
/*!40000 ALTER TABLE `quotation_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quotation_templates`
--

DROP TABLE IF EXISTS `quotation_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quotation_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company_address` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company_gstin` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `header_text` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `footer_text` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `default_terms` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `default_notes` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_attachment_id` bigint DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quotation_template_code` (`template_code`),
  KEY `fk_quotation_template_logo` (`logo_attachment_id`),
  KEY `idx_quotation_templates_active_name` (`active`,`name`),
  CONSTRAINT `fk_quotation_template_logo` FOREIGN KEY (`logo_attachment_id`) REFERENCES `file_attachments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quotation_templates`
--

LOCK TABLES `quotation_templates` WRITE;
/*!40000 ALTER TABLE `quotation_templates` DISABLE KEYS */;
INSERT INTO `quotation_templates` VALUES (1,'E2E-QT-20260728','E2E Standard Rental Quotation','E2E validation template','StockSync Test Company','Pune, Maharashtra','27AAAAA1111A1Z1','Shuttering material rental quotation','Computer-generated E2E document','Monthly rental. Material remains company property.','E2E validation only',NULL,1,0,'2026-07-28 00:09:49.641175','admin','2026-07-28 00:09:49.641175','admin');
/*!40000 ALTER TABLE `quotation_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quotations`
--

DROP TABLE IF EXISTS `quotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quotations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quotation_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quotation_template_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `party_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_id` bigint NOT NULL,
  `site_name_snapshot` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quotation_date` date NOT NULL,
  `valid_until` date NOT NULL,
  `rental_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `transport_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `loading_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `unloading_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `other_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `tax_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `subtotal` decimal(19,2) NOT NULL DEFAULT '0.00',
  `discount_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE',
  `discount_value` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `discount_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `taxable_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `cgst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `cgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `sgst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `sgst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `igst_rate` decimal(7,4) NOT NULL DEFAULT '0.0000',
  `igst_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `total_tax` decimal(19,2) NOT NULL DEFAULT '0.00',
  `round_off` decimal(19,2) NOT NULL DEFAULT '0.00',
  `tax_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `grand_total` decimal(19,2) NOT NULL DEFAULT '0.00',
  `security_deposit` decimal(19,2) NOT NULL DEFAULT '0.00',
  `terms` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rejection_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sent_at` timestamp(6) NULL DEFAULT NULL,
  `sent_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_at` timestamp(6) NULL DEFAULT NULL,
  `approved_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rejected_at` timestamp(6) NULL DEFAULT NULL,
  `rejected_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quotations_number` (`quotation_number`),
  KEY `fk_quotations_site` (`site_id`),
  KEY `idx_quotations_party_site` (`party_id`,`site_id`),
  KEY `idx_quotations_status_date` (`status`,`quotation_date`),
  KEY `idx_quotations_valid_until` (`valid_until`),
  KEY `idx_quotations_template` (`quotation_template_id`),
  CONSTRAINT `fk_quotation_template` FOREIGN KEY (`quotation_template_id`) REFERENCES `quotation_templates` (`id`),
  CONSTRAINT `fk_quotations_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_quotations_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `ck_quotation_amounts` CHECK (((`transport_charge` >= 0) and (`loading_charge` >= 0) and (`unloading_charge` >= 0) and (`other_charge` >= 0) and (`discount_value` >= 0) and (`discount_amount` >= 0) and (`taxable_amount` >= 0) and (`cgst_rate` between 0 and 100) and (`sgst_rate` between 0 and 100) and (`igst_rate` between 0 and 100) and (`cgst_amount` >= 0) and (`sgst_amount` >= 0) and (`igst_amount` >= 0) and (`total_tax` >= 0) and (`subtotal` >= 0) and (`grand_total` >= 0) and (`security_deposit` >= 0))),
  CONSTRAINT `ck_quotation_discount_type` CHECK ((`discount_type` in (_utf8mb4'NONE',_utf8mb4'PERCENTAGE',_utf8mb4'FIXED'))),
  CONSTRAINT `ck_quotations_dates` CHECK ((`valid_until` >= `quotation_date`)),
  CONSTRAINT `ck_quotations_rental_type` CHECK ((`rental_type` in (_utf8mb4'PER_PIECE_PER_DAY',_utf8mb4'PLATE_AREA_PER_DAY',_utf8mb4'SCAFFOLD_AREA_PER_DAY',_utf8mb4'PLOT_AREA_PER_DAY',_utf8mb4'FIXED_RATE',_utf8mb4'SLAB_BASED'))),
  CONSTRAINT `ck_quotations_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'SENT',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'EXPIRED',_utf8mb4'CANCELLED',_utf8mb4'CONVERTED')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quotations`
--

LOCK TABLES `quotations` WRITE;
/*!40000 ALTER TABLE `quotations` DISABLE KEYS */;
INSERT INTO `quotations` VALUES (1,'QT/2026-27/0001',1,17,'E2E ABC Construction Pvt Ltd 20260728',17,'E2E Wakad Commercial Project 20260728','2026-08-01','2026-08-31','PER_PIECE_PER_DAY','CONVERTED',1200.00,300.00,0.00,100.00,18.0000,5500.00,'NONE',0.0000,0.00,7100.00,9.0000,639.00,9.0000,639.00,0.0000,0.00,1278.00,0.00,1278.00,8378.00,25000.00,'Monthly rental billed by actual issue and return dates. Minimum billing applies.','E2E complete realistic flow',3,'2026-07-28 00:10:15.026767','admin','2026-07-28 00:10:57.764547','admin',NULL,'2026-07-28 00:10:15.093492','admin','2026-07-28 00:10:15.190707','admin',NULL,NULL,NULL,NULL,NULL),(2,'QT/2026-27/0002',1,17,'E2E ABC Construction Pvt Ltd 20260728',18,'E2E Baner Residential Project 20260728','2026-08-01','2026-08-31','PER_PIECE_PER_DAY','CONVERTED',0.00,0.00,0.00,0.00,18.0000,450.00,'NONE',0.0000,0.00,450.00,9.0000,40.50,9.0000,40.50,0.0000,0.00,81.00,0.00,81.00,531.00,10000.00,'Destination site agreement for E2E transfer','E2E Baner agreement',3,'2026-07-28 00:11:20.853214','admin','2026-07-28 00:11:20.942859','admin',NULL,'2026-07-28 00:11:20.892523','admin','2026-07-28 00:11:20.909670','admin',NULL,NULL,NULL,NULL,NULL),(3,'QT/2026-27/0003',1,17,'E2E ABC Construction Pvt Ltd 20260728',19,'E2E Historical Wakad Billing 20260728','2026-07-01','2026-07-31','PER_PIECE_PER_DAY','CONVERTED',1200.00,300.00,0.00,100.00,18.0000,5500.00,'NONE',0.0000,0.00,7100.00,9.0000,639.00,9.0000,639.00,0.0000,0.00,1278.00,0.00,1278.00,8378.00,25000.00,'Historical E2E rental terms','Billing segmentation scenario',3,'2026-07-28 00:17:28.222118','admin','2026-07-28 00:17:28.431158','admin',NULL,'2026-07-28 00:17:28.359324','admin','2026-07-28 00:17:28.383817','admin',NULL,NULL,NULL,NULL,NULL),(4,'QT/2026-27/0004',1,17,'E2E ABC Construction Pvt Ltd 20260728',20,'E2E Historical Baner Billing 20260728','2026-07-01','2026-07-31','PER_PIECE_PER_DAY','CONVERTED',1200.00,300.00,0.00,100.00,18.0000,450.00,'NONE',0.0000,0.00,2050.00,9.0000,184.50,9.0000,184.50,0.0000,0.00,369.00,0.00,369.00,2419.00,25000.00,'Historical E2E rental terms','Billing segmentation scenario',3,'2026-07-28 00:17:28.303147','admin','2026-07-28 00:17:28.790064','admin',NULL,'2026-07-28 00:17:28.727381','admin','2026-07-28 00:17:28.748799','admin',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `quotations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receiving_challan_items`
--

DROP TABLE IF EXISTS `receiving_challan_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receiving_challan_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiving_challan_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `linked_issued_challan_item_id` bigint DEFAULT NULL,
  `opening_import_transaction_id` bigint DEFAULT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_snapshot` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `pending_quantity_snapshot` decimal(19,4) NOT NULL,
  `good_returned_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `damaged_returned_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `lost_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `extra_returned_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `exchanged_from_item_id` bigint DEFAULT NULL,
  `exchanged_to_item_id` bigint DEFAULT NULL,
  `exchanged_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `weight_per_piece_snapshot` decimal(19,4) DEFAULT NULL,
  `good_returned_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `damaged_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `lost_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sequence` int NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_receiving_items_challan` (`receiving_challan_id`),
  KEY `fk_receiving_items_item` (`item_id`),
  KEY `fk_receiving_items_issued_item` (`linked_issued_challan_item_id`),
  KEY `fk_receiving_items_exchanged_from` (`exchanged_from_item_id`),
  KEY `fk_receiving_items_exchanged_to` (`exchanged_to_item_id`),
  CONSTRAINT `fk_receiving_items_challan` FOREIGN KEY (`receiving_challan_id`) REFERENCES `receiving_challans` (`id`),
  CONSTRAINT `fk_receiving_items_exchanged_from` FOREIGN KEY (`exchanged_from_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_receiving_items_exchanged_to` FOREIGN KEY (`exchanged_to_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_receiving_items_issued_item` FOREIGN KEY (`linked_issued_challan_item_id`) REFERENCES `issued_challan_items` (`id`),
  CONSTRAINT `fk_receiving_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receiving_challan_items`
--

LOCK TABLES `receiving_challan_items` WRITE;
/*!40000 ALTER TABLE `receiving_challan_items` DISABLE KEYS */;
INSERT INTO `receiving_challan_items` VALUES (1,1,1,NULL,NULL,'MAT-001','H frames',NULL,'PIECE',100.0000,80.0000,10.0000,5.0000,0.0000,NULL,NULL,0.0000,NULL,0.0000,0.0000,0.0000,'Five H Frames remain at Wakad',1,0),(2,2,1,NULL,NULL,'MAT-001','H frames',NULL,'PIECE',100.0000,80.0000,10.0000,5.0000,0.0000,NULL,NULL,0.0000,NULL,0.0000,0.0000,0.0000,'5 remain',1,0);
/*!40000 ALTER TABLE `receiving_challan_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receiving_challans`
--

DROP TABLE IF EXISTS `receiving_challans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receiving_challans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiving_challan_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agreement_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `linked_issued_challan_id` bigint DEFAULT NULL,
  `receive_date` date NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `vehicle_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transporter_id` bigint DEFAULT NULL,
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `posted_at` timestamp(6) NULL DEFAULT NULL,
  `posted_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transport_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  `handling_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receiving_challans_number` (`receiving_challan_number`),
  KEY `fk_receiving_challans_party` (`party_id`),
  KEY `fk_receiving_challans_agreement` (`agreement_id`),
  KEY `fk_receiving_challans_issued` (`linked_issued_challan_id`),
  KEY `idx_receiving_challans_receive_status` (`receive_date`,`status`),
  KEY `idx_dashboard_receiving_site_status_date` (`site_id`,`status`,`receive_date`),
  CONSTRAINT `fk_receiving_challans_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_receiving_challans_issued` FOREIGN KEY (`linked_issued_challan_id`) REFERENCES `issued_challans` (`id`),
  CONSTRAINT `fk_receiving_challans_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_receiving_challans_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receiving_challans`
--

LOCK TABLES `receiving_challans` WRITE;
/*!40000 ALTER TABLE `receiving_challans` DISABLE KEYS */;
INSERT INTO `receiving_challans` VALUES (1,'RC/2026-27/0001',1,17,17,NULL,'2026-08-10','POSTED','MH12RET01','E2E Return Driver','9876500000',NULL,'SITE_PENDING_BALANCE','E2E H Frame return: 80 good, 10 damaged, 5 lost','2026-07-28 00:13:30.267127','admin',NULL,NULL,NULL,1,'2026-07-28 00:13:30.125402','admin','2026-07-28 00:13:30.274860','admin',0.00,0.00),(2,'RC/2026-27/0002',3,17,19,NULL,'2026-07-10','POSTED','MH12HISTR','Historical Return','9876522222',NULL,'SITE_PENDING_BALANCE','Historical partial return','2026-07-28 00:17:55.882019','admin',NULL,NULL,NULL,1,'2026-07-28 00:17:54.531464','admin','2026-07-28 00:17:55.886365','admin',0.00,0.00);
/*!40000 ALTER TABLE `receiving_challans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_export_history`
--

DROP TABLE IF EXISTS `report_export_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_export_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_type` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `export_format` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `filter_json` json NOT NULL,
  `generated_by_user_id` bigint DEFAULT NULL,
  `generated_by_username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `generated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `storage_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_report_exports_user_generated` (`generated_by_user_id`,`generated_at`),
  KEY `idx_report_exports_type_format` (`report_type`,`export_format`),
  CONSTRAINT `fk_report_exports_user` FOREIGN KEY (`generated_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_report_exports_format` CHECK ((`export_format` in (_utf8mb4'PDF',_utf8mb4'EXCEL',_utf8mb4'CSV'))),
  CONSTRAINT `ck_report_exports_status` CHECK ((`status` in (_utf8mb4'SUCCESS',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_export_history`
--

LOCK TABLES `report_export_history` WRITE;
/*!40000 ALTER TABLE `report_export_history` DISABLE KEYS */;
INSERT INTO `report_export_history` VALUES (1,'CURRENT_STOCK_SUMMARY','CSV','{\"page\": 0, \"size\": 100, \"user\": null, \"month\": null, \"itemId\": null, \"siteId\": null, \"status\": null, \"endDate\": \"2026-07-28\", \"partyId\": null, \"startDate\": \"2026-07-01\", \"categoryId\": null, \"agreementId\": null, \"documentNumber\": null}',NULL,'admin','2026-07-28 05:55:49.615384','1785218149610-current-stock-summary-2026-07-28.csv','current-stock-summary-2026-07-28.csv','text/csv',3717,'SUCCESS',NULL),(2,'CURRENT_STOCK_SUMMARY','EXCEL','{\"page\": 0, \"size\": 100, \"user\": null, \"month\": null, \"itemId\": null, \"siteId\": null, \"status\": null, \"endDate\": \"2026-07-28\", \"partyId\": null, \"startDate\": \"2026-07-01\", \"categoryId\": null, \"agreementId\": null, \"documentNumber\": null}',NULL,'admin','2026-07-28 05:55:50.728351','1785218149754-current-stock-summary-2026-07-28.xlsx','current-stock-summary-2026-07-28.xlsx','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',5799,'SUCCESS',NULL),(3,'CURRENT_STOCK_SUMMARY','PDF','{\"page\": 0, \"size\": 100, \"user\": null, \"month\": null, \"itemId\": null, \"siteId\": null, \"status\": null, \"endDate\": \"2026-07-28\", \"partyId\": null, \"startDate\": \"2026-07-01\", \"categoryId\": null, \"agreementId\": null, \"documentNumber\": null}',NULL,'admin','2026-07-28 05:55:51.801405','1785218151389-current-stock-summary-2026-07-28.pdf','current-stock-summary-2026-07-28.pdf','application/pdf',23685,'SUCCESS',NULL),(4,'GST_SALES_REGISTER','EXCEL','{\"page\": 0, \"size\": 100, \"user\": null, \"month\": null, \"itemId\": null, \"siteId\": null, \"status\": null, \"endDate\": \"2026-07-28\", \"partyId\": null, \"startDate\": \"2026-07-01\", \"categoryId\": null, \"agreementId\": null, \"documentNumber\": null}',NULL,'admin','2026-07-28 05:55:52.128380','1785218152110-gst-sales-register-2026-07-28.xlsx','gst-sales-register-2026-07-28.xlsx','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',3937,'SUCCESS',NULL),(5,'GSTR3B_SUMMARY','EXCEL','{\"page\": 0, \"size\": 100, \"user\": null, \"month\": null, \"itemId\": null, \"siteId\": null, \"status\": null, \"endDate\": \"2026-07-28\", \"partyId\": null, \"startDate\": \"2026-07-01\", \"categoryId\": null, \"agreementId\": null, \"documentNumber\": null}',NULL,'admin','2026-07-28 05:55:52.174677','1785218152163-gstr3b-summary-2026-07-28.xlsx','gstr3b-summary-2026-07-28.xlsx','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',3640,'SUCCESS',NULL);
/*!40000 ALTER TABLE `report_export_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES ('ROLE_ACCOUNTS','Financial manager for billing, payments, and GST'),('ROLE_ADMIN','System Administrator with full access'),('ROLE_OPERATIONS','Operations manager for inventory and challans'),('ROLE_VIEWER','Read-only access across the system');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `saved_report_filters`
--

DROP TABLE IF EXISTS `saved_report_filters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `saved_report_filters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_type` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `filter_json` json NOT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `owner_username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shared_flag` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_saved_report_filters_owner_type` (`owner_user_id`,`report_type`),
  KEY `idx_saved_report_filters_shared_type` (`shared_flag`,`report_type`),
  CONSTRAINT `fk_saved_report_filters_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `saved_report_filters`
--

LOCK TABLES `saved_report_filters` WRITE;
/*!40000 ALTER TABLE `saved_report_filters` DISABLE KEYS */;
/*!40000 ALTER TABLE `saved_report_filters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scrap_entries`
--

DROP TABLE IF EXISTS `scrap_entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scrap_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scrap_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scrap_date` date NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scrap_entries_number` (`scrap_number`),
  UNIQUE KEY `uk_scrap_entries_idempotency` (`idempotency_key`),
  KEY `idx_scrap_entries_date` (`scrap_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scrap_entries`
--

LOCK TABLES `scrap_entries` WRITE;
/*!40000 ALTER TABLE `scrap_entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `scrap_entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scrap_items`
--

DROP TABLE IF EXISTS `scrap_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scrap_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scrap_entry_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_scrap_items_entry` (`scrap_entry_id`),
  KEY `fk_scrap_items_item` (`item_id`),
  CONSTRAINT `fk_scrap_items_entry` FOREIGN KEY (`scrap_entry_id`) REFERENCES `scrap_entries` (`id`),
  CONSTRAINT `fk_scrap_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `ck_scrap_items_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scrap_items`
--

LOCK TABLES `scrap_items` WRITE;
/*!40000 ALTER TABLE `scrap_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `scrap_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_deposit_transactions`
--

DROP TABLE IF EXISTS `security_deposit_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `security_deposit_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `deposit_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agreement_id` bigint NOT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `agreement_number_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_date` date NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `payment_mode` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_invoice_id` bigint DEFAULT NULL,
  `source_deposit_transaction_id` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachment_id` bigint DEFAULT NULL,
  `posted_at` timestamp(6) NULL DEFAULT NULL,
  `posted_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversed_at` timestamp(6) NULL DEFAULT NULL,
  `reversed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversal_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_security_deposit_number` (`deposit_number`),
  KEY `fk_security_deposits_site` (`site_id`),
  KEY `fk_security_deposits_invoice` (`related_invoice_id`),
  KEY `fk_security_deposits_source` (`source_deposit_transaction_id`),
  KEY `fk_security_deposits_attachment` (`attachment_id`),
  KEY `idx_security_deposits_agreement` (`agreement_id`,`transaction_date`),
  KEY `idx_security_deposits_party_site` (`party_id`,`site_id`),
  KEY `idx_security_deposits_status_type` (`status`,`transaction_type`),
  CONSTRAINT `fk_security_deposits_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_security_deposits_attachment` FOREIGN KEY (`attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_security_deposits_invoice` FOREIGN KEY (`related_invoice_id`) REFERENCES `invoices` (`id`),
  CONSTRAINT `fk_security_deposits_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_security_deposits_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `fk_security_deposits_source` FOREIGN KEY (`source_deposit_transaction_id`) REFERENCES `security_deposit_transactions` (`id`),
  CONSTRAINT `ck_security_deposits_amount` CHECK ((`amount` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_deposit_transactions`
--

LOCK TABLES `security_deposit_transactions` WRITE;
/*!40000 ALTER TABLE `security_deposit_transactions` DISABLE KEYS */;
INSERT INTO `security_deposit_transactions` VALUES (1,'SD/2026-27/0001',3,17,19,'AGR/2026-27/0003','E2E ABC Construction Pvt Ltd 20260728','E2E Historical Wakad Billing 20260728','RECEIPT','2026-07-28',10000.00,'NEFT','E2E-DEP-001',NULL,NULL,'POSTED','E2E security deposit',NULL,'2026-07-28 00:19:24.019750','admin',NULL,NULL,NULL,'2026-07-28 00:19:24.020085','admin','2026-07-28 00:19:24.020085','admin',0),(2,'SD/2026-27/0002',3,17,19,'AGR/2026-27/0003','E2E ABC Construction Pvt Ltd 20260728','E2E Historical Wakad Billing 20260728','ADJUSTMENT_TO_INVOICE','2026-07-28',5000.00,NULL,NULL,1,NULL,'POSTED','Partial deposit adjustment',NULL,'2026-07-28 00:19:24.073213','admin',NULL,NULL,NULL,'2026-07-28 00:19:24.073340','admin','2026-07-28 00:19:24.073340','admin',0);
/*!40000 ALTER TABLE `security_deposit_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `site_order_items`
--

DROP TABLE IF EXISTS `site_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `ordered_quantity` decimal(19,4) NOT NULL,
  `issued_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `remaining_quantity` decimal(19,4) GENERATED ALWAYS AS ((`ordered_quantity` - `issued_quantity`)) STORED,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_order_items_item` (`order_id`,`item_id`),
  KEY `fk_site_order_items_item` (`item_id`),
  CONSTRAINT `fk_site_order_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_site_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `site_orders` (`id`),
  CONSTRAINT `ck_site_order_items_values` CHECK (((`ordered_quantity` > 0) and (`issued_quantity` >= 0) and (`issued_quantity` <= `ordered_quantity`)))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `site_order_items`
--

LOCK TABLES `site_order_items` WRITE;
/*!40000 ALTER TABLE `site_order_items` DISABLE KEYS */;
INSERT INTO `site_order_items` (`id`, `order_id`, `item_id`, `ordered_quantity`, `issued_quantity`, `version`) VALUES (1,1,1,100.0000,100.0000,2),(2,1,4,200.0000,200.0000,2),(3,2,1,100.0000,100.0000,2),(4,2,4,200.0000,200.0000,2),(5,3,1,1.0000,0.0000,0);
/*!40000 ALTER TABLE `site_order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `site_orders`
--

DROP TABLE IF EXISTS `site_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agreement_id` bigint NOT NULL,
  `party_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  `order_date` date NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_orders_number` (`order_number`),
  KEY `fk_site_orders_party` (`party_id`),
  KEY `idx_site_orders_agreement` (`agreement_id`),
  KEY `idx_site_orders_site_status` (`site_id`,`status`),
  KEY `idx_dashboard_site_orders_status_date` (`status`,`order_date`),
  CONSTRAINT `fk_site_orders_agreement` FOREIGN KEY (`agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_site_orders_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_site_orders_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `ck_site_orders_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'CONFIRMED',_utf8mb4'PARTIALLY_FULFILLED',_utf8mb4'FULFILLED',_utf8mb4'COMPLETED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `site_orders`
--

LOCK TABLES `site_orders` WRITE;
/*!40000 ALTER TABLE `site_orders` DISABLE KEYS */;
INSERT INTO `site_orders` VALUES (1,'ORD/2026-27/0001',1,17,17,'2026-08-01','FULFILLED','E2E split fulfilment order',3,'2026-07-28 00:11:54.085571','admin','2026-07-28 00:11:54.416846','admin'),(2,'ORD/2026-27/0002',3,17,19,'2026-07-01','FULFILLED','Historical billing source order',3,'2026-07-28 00:17:54.291840','admin','2026-07-28 00:17:54.503216','admin'),(3,'ORD/2026-27/0003',3,17,19,'2026-07-05','DRAFT','E2E role authorization probe',0,'2026-07-28 00:27:39.387913','e2eops','2026-07-28 00:27:39.387913','e2eops');
/*!40000 ALTER TABLE `site_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `site_stock_balances`
--

DROP TABLE IF EXISTS `site_stock_balances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_stock_balances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `site_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `pending_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `version` bigint NOT NULL DEFAULT '0',
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_stock_balances` (`site_id`,`item_id`),
  KEY `fk_site_stock_balances_item` (`item_id`),
  KEY `idx_site_stock_balances_site_item` (`site_id`,`item_id`),
  CONSTRAINT `fk_site_stock_balances_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_site_stock_balances_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `ck_site_stock_balances_pending` CHECK ((`pending_quantity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=127 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `site_stock_balances`
--

LOCK TABLES `site_stock_balances` WRITE;
/*!40000 ALTER TABLE `site_stock_balances` DISABLE KEYS */;
INSERT INTO `site_stock_balances` VALUES (1,1,1,33.0000,3,'2026-07-28 00:29:42.793050'),(2,2,1,20.0000,3,'2026-07-28 00:29:42.803269'),(3,3,1,319.0000,3,'2026-07-28 00:29:42.813155'),(4,5,1,151.0000,3,'2026-07-28 00:29:42.822868'),(5,6,1,190.0000,3,'2026-07-28 00:29:42.832929'),(6,8,1,287.0000,3,'2026-07-28 00:29:42.841848'),(7,11,1,1031.0000,3,'2026-07-28 00:29:42.851960'),(8,13,1,130.0000,3,'2026-07-28 00:29:42.861679'),(9,1,4,66.0000,3,'2026-07-28 00:29:42.889851'),(10,2,4,20.0000,3,'2026-07-28 00:29:42.897587'),(11,3,4,310.0000,3,'2026-07-28 00:29:42.904780'),(12,5,4,246.0000,3,'2026-07-28 00:29:42.911657'),(13,6,4,462.0000,3,'2026-07-28 00:29:42.919819'),(14,8,4,431.0000,3,'2026-07-28 00:29:42.927285'),(15,11,4,1722.0000,3,'2026-07-28 00:29:42.935948'),(16,13,4,167.0000,3,'2026-07-28 00:29:42.943120'),(17,1,7,27.0000,3,'2026-07-28 00:29:42.968124'),(18,2,7,102.0000,3,'2026-07-28 00:29:42.974497'),(19,5,7,15.0000,3,'2026-07-28 00:29:42.981773'),(20,6,7,85.0000,3,'2026-07-28 00:29:42.988308'),(21,7,7,31.0000,3,'2026-07-28 00:29:42.994771'),(22,8,7,252.0000,3,'2026-07-28 00:29:43.002134'),(23,9,7,255.0000,3,'2026-07-28 00:29:43.008920'),(24,11,7,10.0000,3,'2026-07-28 00:29:43.017187'),(25,12,7,100.0000,3,'2026-07-28 00:29:43.024661'),(26,13,7,20.0000,3,'2026-07-28 00:29:43.032092'),(27,14,7,10.0000,3,'2026-07-28 00:29:43.038726'),(28,15,7,10.0000,3,'2026-07-28 00:29:43.045210'),(29,2,8,267.0000,3,'2026-07-28 00:29:43.057688'),(30,4,8,190.0000,3,'2026-07-28 00:29:43.065298'),(31,7,8,140.0000,3,'2026-07-28 00:29:43.072284'),(32,9,8,230.0000,3,'2026-07-28 00:29:43.079053'),(33,10,8,120.0000,3,'2026-07-28 00:29:43.087064'),(34,15,8,22.0000,3,'2026-07-28 00:29:43.093585'),(35,16,8,160.0000,3,'2026-07-28 00:29:43.101728'),(36,4,9,120.0000,3,'2026-07-28 00:29:43.114593'),(37,10,9,155.0000,3,'2026-07-28 00:29:43.122106'),(38,2,10,20.0000,3,'2026-07-28 00:29:44.274099'),(39,4,10,40.0000,3,'2026-07-28 00:29:44.355777'),(40,9,10,150.0000,3,'2026-07-28 00:29:44.363616'),(41,12,10,210.0000,3,'2026-07-28 00:29:44.371124'),(42,14,10,100.0000,3,'2026-07-28 00:29:44.379057'),(43,2,11,20.0000,3,'2026-07-28 00:29:44.393276'),(44,4,11,180.0000,3,'2026-07-28 00:29:44.402552'),(45,2,12,30.0000,3,'2026-07-28 00:29:44.418010'),(46,4,12,600.0000,3,'2026-07-28 00:29:44.425380'),(47,7,13,385.0000,3,'2026-07-28 00:29:44.439459'),(48,16,13,580.0000,3,'2026-07-28 00:29:44.447202'),(49,2,14,300.0000,3,'2026-07-28 00:29:44.460327'),(50,7,15,300.0000,3,'2026-07-28 00:29:44.474189'),(51,12,15,240.0000,3,'2026-07-28 00:29:44.482067'),(52,15,15,120.0000,3,'2026-07-28 00:29:44.489502'),(53,9,17,615.0000,3,'2026-07-28 00:29:44.507659'),(54,14,17,220.0000,3,'2026-07-28 00:29:44.515173'),(55,4,18,736.0000,3,'2026-07-28 00:29:44.527919'),(56,4,19,545.0000,3,'2026-07-28 00:29:44.541188'),(57,10,19,47.0000,3,'2026-07-28 00:29:44.550588'),(58,12,19,140.0000,3,'2026-07-28 00:29:44.558189'),(59,15,19,72.0000,3,'2026-07-28 00:29:44.567318'),(60,16,19,580.0000,3,'2026-07-28 00:29:44.574951'),(61,2,20,500.0000,3,'2026-07-28 00:29:44.584486'),(62,2,21,500.0000,3,'2026-07-28 00:29:44.591606'),(63,4,21,293.0000,3,'2026-07-28 00:29:44.600580'),(64,9,21,734.0000,3,'2026-07-28 00:29:44.607907'),(65,6,22,22.0000,3,'2026-07-28 00:29:44.622653'),(66,13,22,12.0000,3,'2026-07-28 00:29:44.630937'),(67,1,23,4.0000,3,'2026-07-28 00:29:44.649950'),(68,5,23,18.0000,3,'2026-07-28 00:29:44.663568'),(69,6,23,39.0000,3,'2026-07-28 00:29:44.675412'),(70,7,23,28.0000,3,'2026-07-28 00:29:44.686380'),(71,8,23,39.0000,3,'2026-07-28 00:29:44.695315'),(72,12,23,80.0000,3,'2026-07-28 00:29:45.486937'),(73,13,23,44.0000,3,'2026-07-28 00:29:45.494513'),(74,14,23,50.0000,3,'2026-07-28 00:29:45.503715'),(75,15,23,10.0000,3,'2026-07-28 00:29:45.512017'),(76,7,24,6.0000,3,'2026-07-28 00:29:45.939236'),(77,1,26,22.0000,3,'2026-07-28 00:29:45.954131'),(78,2,26,126.0000,3,'2026-07-28 00:29:45.962764'),(79,5,26,58.0000,3,'2026-07-28 00:29:45.972642'),(80,6,26,70.0000,3,'2026-07-28 00:29:45.981839'),(81,7,26,41.0000,3,'2026-07-28 00:29:45.989966'),(82,8,26,32.0000,3,'2026-07-28 00:29:45.999068'),(83,9,26,133.0000,3,'2026-07-28 00:29:46.007677'),(84,10,26,96.0000,3,'2026-07-28 00:29:46.016549'),(85,11,26,74.0000,3,'2026-07-28 00:29:46.024500'),(86,12,26,50.0000,3,'2026-07-28 00:29:46.033140'),(87,13,26,38.0000,3,'2026-07-28 00:29:46.041028'),(88,14,26,50.0000,3,'2026-07-28 00:29:46.050277'),(89,15,26,22.0000,3,'2026-07-28 00:29:46.164204'),(90,1,27,8.0000,3,'2026-07-28 00:29:46.181122'),(91,5,27,60.0000,3,'2026-07-28 00:29:46.192254'),(92,6,27,229.0000,3,'2026-07-28 00:29:46.201812'),(93,7,27,117.0000,3,'2026-07-28 00:29:46.210214'),(94,8,27,90.0000,3,'2026-07-28 00:29:46.219525'),(95,11,27,200.0000,3,'2026-07-28 00:29:46.228292'),(96,12,27,180.0000,3,'2026-07-28 00:29:46.237203'),(97,13,27,114.0000,3,'2026-07-28 00:29:46.246050'),(98,14,27,150.0000,3,'2026-07-28 00:29:46.255886'),(99,15,27,34.0000,3,'2026-07-28 00:29:46.265576'),(100,4,29,1000.0000,3,'2026-07-28 00:29:46.279605'),(101,6,29,10.0000,3,'2026-07-28 00:29:46.288388'),(102,7,29,146.0000,3,'2026-07-28 00:29:46.297231'),(103,9,29,141.0000,3,'2026-07-28 00:29:46.307381'),(104,12,29,175.0000,3,'2026-07-28 00:29:46.316925'),(105,14,29,100.0000,3,'2026-07-28 00:29:46.327520'),(106,15,29,22.0000,3,'2026-07-28 00:29:46.335766'),(107,16,29,80.0000,3,'2026-07-28 00:29:46.343385'),(108,7,30,2.0000,3,'2026-07-28 00:29:47.107369'),(109,13,30,4.0000,3,'2026-07-28 00:29:47.344088'),(110,6,31,4.0000,3,'2026-07-28 00:29:47.358759'),(111,8,32,6.0000,3,'2026-07-28 00:29:47.367495'),(112,14,32,20.0000,3,'2026-07-28 00:29:47.375426'),(113,7,33,20.0000,3,'2026-07-28 00:29:47.390416'),(114,11,33,195.0000,3,'2026-07-28 00:29:47.398895'),(115,15,33,6.0000,3,'2026-07-28 00:29:47.408045'),(116,5,34,2.0000,3,'2026-07-28 00:29:47.424610'),(117,5,35,20.0000,3,'2026-07-28 00:29:47.520566'),(118,12,36,25.0000,3,'2026-07-28 00:29:47.529000'),(119,12,41,100.0000,3,'2026-07-28 00:29:47.559651'),(120,12,42,50.0000,3,'2026-07-28 00:29:47.569435'),(121,17,1,2.0000,4,'2026-07-28 00:15:59.851506'),(122,17,4,198.0000,3,'2026-07-28 00:15:34.859778'),(123,18,1,3.0000,1,'2026-07-28 00:15:59.851545'),(124,19,1,2.0000,4,'2026-07-28 00:17:56.293336'),(125,19,4,200.0000,2,'2026-07-28 00:17:54.503332'),(126,20,1,1.0000,3,'2026-07-28 00:20:20.440021');
/*!40000 ALTER TABLE `site_stock_balances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `site_transfer_items`
--

DROP TABLE IF EXISTS `site_transfer_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_transfer_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_id` bigint NOT NULL,
  `source_agreement_item_id` bigint NOT NULL,
  `destination_agreement_item_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `item_code_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_snapshot` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight_per_piece_snapshot` decimal(19,4) NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `total_weight` decimal(19,4) NOT NULL,
  `source_pending_before` decimal(19,4) NOT NULL,
  `source_pending_after` decimal(19,4) NOT NULL,
  `destination_pending_before` decimal(19,4) NOT NULL,
  `destination_pending_after` decimal(19,4) NOT NULL,
  `sequence` int NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_transfer_items_transfer` (`transfer_id`),
  KEY `fk_transfer_items_source_ag_item` (`source_agreement_item_id`),
  KEY `fk_transfer_items_dest_ag_item` (`destination_agreement_item_id`),
  KEY `fk_transfer_items_item` (`item_id`),
  CONSTRAINT `fk_transfer_items_dest_ag_item` FOREIGN KEY (`destination_agreement_item_id`) REFERENCES `agreement_items` (`id`),
  CONSTRAINT `fk_transfer_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_transfer_items_source_ag_item` FOREIGN KEY (`source_agreement_item_id`) REFERENCES `agreement_items` (`id`),
  CONSTRAINT `fk_transfer_items_transfer` FOREIGN KEY (`transfer_id`) REFERENCES `site_transfers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `site_transfer_items`
--

LOCK TABLES `site_transfer_items` WRITE;
/*!40000 ALTER TABLE `site_transfer_items` DISABLE KEYS */;
INSERT INTO `site_transfer_items` VALUES (1,1,1,3,1,'MAT-001','H frames','H frames',NULL,'PIECE',0.0000,3.0000,0.0000,5.0000,2.0000,0.0000,3.0000,1,1),(2,2,5,7,1,'MAT-001','H frames','H frames',NULL,'PIECE',0.0000,3.0000,0.0000,5.0000,2.0000,0.0000,3.0000,1,1);
/*!40000 ALTER TABLE `site_transfer_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `site_transfers`
--

DROP TABLE IF EXISTS `site_transfers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_transfers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_agreement_id` bigint NOT NULL,
  `destination_agreement_id` bigint NOT NULL,
  `source_party_id` bigint NOT NULL,
  `source_site_id` bigint NOT NULL,
  `destination_party_id` bigint NOT NULL,
  `destination_site_id` bigint NOT NULL,
  `transfer_date` date NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `vehicle_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transporter_id` bigint DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `posted_at` timestamp(6) NULL DEFAULT NULL,
  `posted_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` timestamp(6) NULL DEFAULT NULL,
  `cancelled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transport_charge` decimal(19,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_transfers_number` (`transfer_number`),
  KEY `fk_site_transfers_source_ag` (`source_agreement_id`),
  KEY `fk_site_transfers_dest_ag` (`destination_agreement_id`),
  KEY `fk_site_transfers_source_pt` (`source_party_id`),
  KEY `fk_site_transfers_dest_pt` (`destination_party_id`),
  KEY `fk_site_transfers_dest_st` (`destination_site_id`),
  KEY `idx_site_transfers_status` (`status`),
  KEY `idx_site_transfers_date` (`transfer_date`),
  KEY `idx_site_transfers_sites` (`source_site_id`,`destination_site_id`),
  CONSTRAINT `fk_site_transfers_dest_ag` FOREIGN KEY (`destination_agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_site_transfers_dest_pt` FOREIGN KEY (`destination_party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_site_transfers_dest_st` FOREIGN KEY (`destination_site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `fk_site_transfers_source_ag` FOREIGN KEY (`source_agreement_id`) REFERENCES `agreements` (`id`),
  CONSTRAINT `fk_site_transfers_source_pt` FOREIGN KEY (`source_party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_site_transfers_source_st` FOREIGN KEY (`source_site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `site_transfers`
--

LOCK TABLES `site_transfers` WRITE;
/*!40000 ALTER TABLE `site_transfers` DISABLE KEYS */;
INSERT INTO `site_transfers` VALUES (1,'ST/2026-27/0001',1,2,17,17,17,18,'2026-08-12','POSTED','MH12TRF01','E2E Transfer Driver','9876511111',NULL,'Transfer three remaining H Frames to Baner','2026-07-28 00:15:59.842081','admin',NULL,NULL,NULL,1,'2026-07-28 00:15:59.739747','admin','2026-07-28 00:15:59.851386','admin',0.00),(2,'ST/2026-27/0002',3,4,17,19,17,20,'2026-07-12','POSTED','MH12HISTT','Historical Transfer','9876533333',NULL,'Historical transfer after partial return','2026-07-28 00:17:56.287433','admin',NULL,NULL,NULL,1,'2026-07-28 00:17:56.232368','admin','2026-07-28 00:17:56.293246','admin',0.00);
/*!40000 ALTER TABLE `site_transfers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sites`
--

DROP TABLE IF EXISTS `sites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `party_id` bigint NOT NULL,
  `site_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `site_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `expected_end_date` date DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `defaulter` tinyint(1) NOT NULL DEFAULT '0',
  `closed_date` date DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sites_code` (`site_code`),
  KEY `idx_sites_party` (`party_id`),
  KEY `idx_sites_status` (`status`),
  KEY `idx_sites_defaulter` (`defaulter`),
  CONSTRAINT `fk_sites_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `ck_sites_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'ON_HOLD',_utf8mb4'DEFAULTER',_utf8mb4'CLOSED')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sites`
--

LOCK TABLES `sites` WRITE;
/*!40000 ALTER TABLE `sites` DISABLE KEYS */;
INSERT INTO `sites` VALUES (1,1,'E2E 4m Façade','E2E-C',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:29.974001','admin','2026-07-28 00:08:29.974001','admin'),(2,2,'E2E Ali Designer','E2E-D',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.079517','admin','2026-07-28 00:08:30.079517','admin'),(3,3,'E2E Avighnaa Kandivali','E2E-E',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.188200','admin','2026-07-28 00:08:30.188200','admin'),(4,4,'E2E Engarc','E2E-F',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.290628','admin','2026-07-28 00:08:30.290628','admin'),(5,5,'E2E Imperial','E2E-G',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.399067','admin','2026-07-28 00:08:30.399067','admin'),(6,6,'E2E Noble','E2E-H',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.508953','admin','2026-07-28 00:08:30.508953','admin'),(7,7,'E2E Zeeco Media','E2E-I',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.622259','admin','2026-07-28 00:08:30.622259','admin'),(8,8,'E2E Raymond GS','E2E-J',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.738731','admin','2026-07-28 00:08:30.738731','admin'),(9,9,'E2E Raymond Tenex','E2E-K',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.851104','admin','2026-07-28 00:08:30.851104','admin'),(10,10,'E2E Epilson','E2E-L',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:30.964058','admin','2026-07-28 00:08:30.964058','admin'),(11,11,'E2E SBUT','E2E-M',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:31.078404','admin','2026-07-28 00:08:31.078404','admin'),(12,12,'E2E SK Interior','E2E-N',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:31.207423','admin','2026-07-28 00:08:31.207423','admin'),(13,13,'E2E Sukoon','E2E-O',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:31.328182','admin','2026-07-28 00:08:31.328182','admin'),(14,14,'E2E ANV','E2E-P',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:31.537758','admin','2026-07-28 00:08:31.537758','admin'),(15,15,'E2E Rocks & Logs','E2E-Q',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:31.657083','admin','2026-07-28 00:08:31.657083','admin'),(16,16,'E2E Innovator Façade','E2E-R',NULL,NULL,NULL,NULL,'ACTIVE',0,NULL,'Created during legacy opening-stock mapping',0,'2026-07-28 00:08:31.776079','admin','2026-07-28 00:08:31.776079','admin'),(17,17,'E2E Wakad Commercial Project 20260728','E2E-WAKAD-20260728','Wakad, Pune','Site Manager Wakad','2026-08-01','2027-07-31','ACTIVE',0,NULL,'Primary E2E workflow site',0,'2026-07-28 00:09:49.582707','admin','2026-07-28 00:09:49.582707','admin'),(18,17,'E2E Baner Residential Project 20260728','E2E-BANER-20260728','Baner, Pune','Site Manager Baner','2026-08-01','2027-07-31','ACTIVE',0,NULL,'Transfer destination E2E site',0,'2026-07-28 00:09:49.621833','admin','2026-07-28 00:09:49.621833','admin'),(19,17,'E2E Historical Wakad Billing 20260728','E2E-HIST-WAKAD','Wakad Pune','Billing Test Manager','2026-07-01','2027-06-30','ACTIVE',0,NULL,'Historical billing segmentation site',0,'2026-07-28 00:17:28.062601','admin','2026-07-28 00:17:28.062601','admin'),(20,17,'E2E Historical Baner Billing 20260728','E2E-HIST-BANER','Baner Pune','Billing Transfer Manager','2026-07-01','2027-06-30','ACTIVE',0,NULL,'Historical transfer destination',0,'2026-07-28 00:17:28.104547','admin','2026-07-28 00:17:28.104547','admin');
/*!40000 ALTER TABLE `sites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_adjustment_items`
--

DROP TABLE IF EXISTS `stock_adjustment_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_adjustment_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `adjustment_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_adjustment_items_adjustment` (`adjustment_id`),
  KEY `fk_adjustment_items_item` (`item_id`),
  CONSTRAINT `fk_adjustment_items_adjustment` FOREIGN KEY (`adjustment_id`) REFERENCES `stock_adjustments` (`id`),
  CONSTRAINT `fk_adjustment_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `ck_adjustment_items_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_adjustment_items`
--

LOCK TABLES `stock_adjustment_items` WRITE;
/*!40000 ALTER TABLE `stock_adjustment_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `stock_adjustment_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_adjustments`
--

DROP TABLE IF EXISTS `stock_adjustments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_adjustments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `adjustment_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adjustment_date` date NOT NULL,
  `direction` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_adjustments_number` (`adjustment_number`),
  UNIQUE KEY `uk_stock_adjustments_idempotency` (`idempotency_key`),
  KEY `idx_stock_adjustments_date` (`adjustment_date`),
  CONSTRAINT `ck_stock_adjustments_direction` CHECK ((`direction` in (_utf8mb4'IN',_utf8mb4'OUT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_adjustments`
--

LOCK TABLES `stock_adjustments` WRITE;
/*!40000 ALTER TABLE `stock_adjustments` DISABLE KEYS */;
/*!40000 ALTER TABLE `stock_adjustments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_balances`
--

DROP TABLE IF EXISTS `stock_balances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_balances` (
  `item_id` bigint NOT NULL,
  `available_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `issued_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `hired_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `lost_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `scrapped_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `available_weight` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `version` bigint NOT NULL DEFAULT '0',
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `damaged_quantity` decimal(19,4) NOT NULL DEFAULT '0.0000',
  PRIMARY KEY (`item_id`),
  CONSTRAINT `fk_stock_balances_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_balances`
--

LOCK TABLES `stock_balances` WRITE;
/*!40000 ALTER TABLE `stock_balances` DISABLE KEYS */;
INSERT INTO `stock_balances` VALUES (1,1427.0000,2171.0000,0.0000,11.0000,0.0000,0.0000,36,'2026-07-28 00:29:42.869315',11.0000),(2,100.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:42.875807',0.0000),(3,250.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:42.882760',0.0000),(4,2230.0000,3824.0000,0.0000,0.0000,2.0000,0.0000,33,'2026-07-28 00:29:42.949697',0.0000),(5,205.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:42.954928',0.0000),(6,100.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:42.959884',0.0000),(7,900.0000,917.0000,0.0000,0.0000,0.0000,0.0000,39,'2026-07-28 00:29:43.050824',0.0000),(8,1020.0000,1129.0000,0.0000,0.0000,0.0000,0.0000,24,'2026-07-28 00:29:43.107144',0.0000),(9,120.0000,275.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:43.127503',0.0000),(10,1110.0000,520.0000,0.0000,0.0000,0.0000,0.0000,18,'2026-07-28 00:29:44.385469',0.0000),(11,150.0000,200.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:44.409331',0.0000),(12,150.0000,630.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:44.432437',0.0000),(13,1270.0000,965.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:44.453364',0.0000),(14,420.0000,300.0000,0.0000,0.0000,0.0000,0.0000,6,'2026-07-28 00:29:44.467173',0.0000),(15,2534.0000,660.0000,0.0000,0.0000,0.0000,0.0000,12,'2026-07-28 00:29:44.494570',0.0000),(16,300.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:44.500543',0.0000),(17,1553.0000,835.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:44.520851',0.0000),(18,2450.0000,736.0000,0.0000,0.0000,0.0000,0.0000,6,'2026-07-28 00:29:44.533973',0.0000),(19,0.0000,1384.0000,0.0000,0.0000,0.0000,0.0000,15,'2026-07-28 00:29:44.574643',0.0000),(20,0.0000,500.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:44.584154',0.0000),(21,2595.0000,1527.0000,0.0000,0.0000,0.0000,0.0000,12,'2026-07-28 00:29:44.614056',0.0000),(22,408.0000,34.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:44.638387',0.0000),(23,850.0000,312.0000,0.0000,0.0000,0.0000,0.0000,30,'2026-07-28 00:29:45.518308',0.0000),(24,0.0000,6.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:45.938863',0.0000),(25,700.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:45.945210',0.0000),(26,1150.0000,812.0000,0.0000,0.0000,0.0000,0.0000,42,'2026-07-28 00:29:46.170505',0.0000),(27,0.0000,1182.0000,0.0000,0.0000,0.0000,0.0000,30,'2026-07-28 00:29:46.265022',0.0000),(28,1400.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:46.271336',0.0000),(29,800.0000,1674.0000,0.0000,0.0000,0.0000,0.0000,27,'2026-07-28 00:29:46.349685',0.0000),(30,260.0000,6.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:47.350376',0.0000),(31,0.0000,4.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.358318',0.0000),(32,130.0000,26.0000,0.0000,0.0000,0.0000,0.0000,9,'2026-07-28 00:29:47.381973',0.0000),(33,200.0000,221.0000,0.0000,0.0000,0.0000,0.0000,12,'2026-07-28 00:29:47.415180',0.0000),(34,0.0000,2.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.424143',0.0000),(35,0.0000,20.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.520078',0.0000),(36,0.0000,25.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.528508',0.0000),(37,30.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.534826',0.0000),(38,112.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.540053',0.0000),(39,5.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.545594',0.0000),(40,23.0000,0.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.551520',0.0000),(41,0.0000,100.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.559159',0.0000),(42,0.0000,50.0000,0.0000,0.0000,0.0000,0.0000,3,'2026-07-28 00:29:47.568883',0.0000);
/*!40000 ALTER TABLE `stock_balances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_import_batches`
--

DROP TABLE IF EXISTS `stock_import_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_import_batches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `import_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_checksum` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_format` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_snapshot_date` date NOT NULL,
  `godown_snapshot_date` date NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_source_rows` int NOT NULL DEFAULT '0',
  `total_balance_rows` int NOT NULL DEFAULT '0',
  `valid_rows` int NOT NULL DEFAULT '0',
  `warning_rows` int NOT NULL DEFAULT '0',
  `error_rows` int NOT NULL DEFAULT '0',
  `expected_party_total` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `expected_godown_total` decimal(19,4) NOT NULL DEFAULT '0.0000',
  `imported_at` timestamp(6) NULL DEFAULT NULL,
  `imported_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversed_at` timestamp(6) NULL DEFAULT NULL,
  `reversed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reversal_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_import_batches_code` (`batch_code`),
  UNIQUE KEY `uk_stock_import_batches_checksum` (`file_checksum`),
  KEY `idx_stock_import_batches_status` (`status`),
  CONSTRAINT `ck_stock_import_batches_source` CHECK ((`source_format` = _utf8mb4'STEELFAB_STOCK_SNAPSHOT_V1')),
  CONSTRAINT `ck_stock_import_batches_status` CHECK ((`status` in (_utf8mb4'UPLOADED',_utf8mb4'PARSED',_utf8mb4'MAPPING_REQUIRED',_utf8mb4'VALIDATED',_utf8mb4'POSTED',_utf8mb4'PARTIALLY_POSTED',_utf8mb4'FAILED',_utf8mb4'REVERSED'))),
  CONSTRAINT `ck_stock_import_batches_type` CHECK ((`import_type` = _utf8mb4'CLIENT_OPENING_STOCK'))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_import_batches`
--

LOCK TABLES `stock_import_batches` WRITE;
/*!40000 ALTER TABLE `stock_import_batches` DISABLE KEYS */;
INSERT INTO `stock_import_batches` VALUES (1,'OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A','CLIENT_OPENING_STOCK','Stock of material as on 25-07-26.xlsx','f6e5541f-d588-404f-823b-b1a3bd080061.xlsx','stock-imports/OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A/f6e5541f-d588-404f-823b-b1a3bd080061.xlsx','c2d4c0df83dc8cd0a5e0f4fbdcb3a8dd8314c99eff089bae68611deb1c44000c','STEELFAB_STOCK_SNAPSHOT_V1','2026-07-25','2026-07-16','POSTED',42,152,152,0,0,20637.0000,25382.0000,'2026-07-28 00:08:33.322438','admin',NULL,NULL,NULL,'E2E real client workbook validation 20260728','2026-07-28 00:08:22.439969','admin','2026-07-28 00:08:33.335307','admin',53),(2,'OSI-4200A46E-DD04-460D-8BA3-500EDA699819','CLIENT_OPENING_STOCK','StockSync_Client_Opening_Stock_Import.xlsx','66555a22-9ae9-40ca-8a1e-d75b32a5bf87.xlsx','stock-imports/OSI-4200A46E-DD04-460D-8BA3-500EDA699819/66555a22-9ae9-40ca-8a1e-d75b32a5bf87.xlsx','8080f52c334412c88f06346eee373ca9fd028038db3af5436e0b82530661e4a5','STEELFAB_STOCK_SNAPSHOT_V1','2026-07-25','2026-07-16','REVERSED',42,152,152,0,0,20637.0000,25382.0000,'2026-07-28 00:29:42.645805','admin','2026-07-28 00:29:47.566853','admin','E2E prove compensating reversal and non-destructive audit trail','E2E reversal validation batch','2026-07-28 00:28:03.490236','admin','2026-07-28 00:29:47.568555','admin',26);
/*!40000 ALTER TABLE `stock_import_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_import_location_mappings`
--

DROP TABLE IF EXISTS `stock_import_location_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_import_location_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `source_excel_column` varchar(5) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_location_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mapped_party_id` bigint NOT NULL,
  `mapped_site_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_import_location_column` (`batch_id`,`source_excel_column`),
  KEY `fk_import_location_party` (`mapped_party_id`),
  KEY `fk_import_location_site` (`mapped_site_id`),
  CONSTRAINT `fk_import_location_batch` FOREIGN KEY (`batch_id`) REFERENCES `stock_import_batches` (`id`),
  CONSTRAINT `fk_import_location_party` FOREIGN KEY (`mapped_party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_import_location_site` FOREIGN KEY (`mapped_site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_import_location_mappings`
--

LOCK TABLES `stock_import_location_mappings` WRITE;
/*!40000 ALTER TABLE `stock_import_location_mappings` DISABLE KEYS */;
INSERT INTO `stock_import_location_mappings` VALUES (1,1,'C','4m Façade',1,1,'2026-07-28 00:08:29.976872','admin','2026-07-28 00:08:29.976872','admin',0),(2,1,'D','Ali Designer',2,2,'2026-07-28 00:08:30.081777','admin','2026-07-28 00:08:30.081777','admin',0),(3,1,'E','Avighnaa Kandivali',3,3,'2026-07-28 00:08:30.189981','admin','2026-07-28 00:08:30.189981','admin',0),(4,1,'F','Engarc',4,4,'2026-07-28 00:08:30.292417','admin','2026-07-28 00:08:30.292417','admin',0),(5,1,'G','Imperial',5,5,'2026-07-28 00:08:30.401124','admin','2026-07-28 00:08:30.401124','admin',0),(6,1,'H','Noble',6,6,'2026-07-28 00:08:30.510985','admin','2026-07-28 00:08:30.510985','admin',0),(7,1,'I','Zeeco Media',7,7,'2026-07-28 00:08:30.624479','admin','2026-07-28 00:08:30.624479','admin',0),(8,1,'J','Raymond GS',8,8,'2026-07-28 00:08:30.740677','admin','2026-07-28 00:08:30.740677','admin',0),(9,1,'K','Raymond Tenex',9,9,'2026-07-28 00:08:30.853072','admin','2026-07-28 00:08:30.853072','admin',0),(10,1,'L','Epilson',10,10,'2026-07-28 00:08:30.966167','admin','2026-07-28 00:08:30.966167','admin',0),(11,1,'M','SBUT',11,11,'2026-07-28 00:08:31.081739','admin','2026-07-28 00:08:31.081739','admin',0),(12,1,'N','SK Interior',12,12,'2026-07-28 00:08:31.209114','admin','2026-07-28 00:08:31.209114','admin',0),(13,1,'O','Sukoon',13,13,'2026-07-28 00:08:31.330609','admin','2026-07-28 00:08:31.330609','admin',0),(14,1,'P','ANV',14,14,'2026-07-28 00:08:31.540037','admin','2026-07-28 00:08:31.540037','admin',0),(15,1,'Q','Rocks & Logs',15,15,'2026-07-28 00:08:31.659066','admin','2026-07-28 00:08:31.659066','admin',0),(16,1,'R','Innovator Façade',16,16,'2026-07-28 00:08:31.778265','admin','2026-07-28 00:08:31.778265','admin',0),(17,2,'C','4m Façade',1,1,'2026-07-28 00:28:29.635000','admin','2026-07-28 00:28:29.635000','admin',0),(18,2,'D','Ali Designer',2,2,'2026-07-28 00:28:29.703116','admin','2026-07-28 00:28:29.703116','admin',0),(19,2,'E','Avighnaa Kandivali',3,3,'2026-07-28 00:28:29.787083','admin','2026-07-28 00:28:29.787083','admin',0),(20,2,'F','Engarc',4,4,'2026-07-28 00:28:29.867836','admin','2026-07-28 00:28:29.867836','admin',0),(21,2,'G','Imperial',5,5,'2026-07-28 00:28:30.273707','admin','2026-07-28 00:28:30.273707','admin',0),(22,2,'H','Noble',6,6,'2026-07-28 00:28:30.379133','admin','2026-07-28 00:28:30.379133','admin',0),(23,2,'I','Zeeco Media',7,7,'2026-07-28 00:28:30.455046','admin','2026-07-28 00:28:30.455046','admin',0),(24,2,'J','Raymond GS',8,8,'2026-07-28 00:28:30.574933','admin','2026-07-28 00:28:30.574933','admin',0),(25,2,'K','Raymond Tenex',9,9,'2026-07-28 00:28:30.649570','admin','2026-07-28 00:28:30.649570','admin',0),(26,2,'L','Epilson',10,10,'2026-07-28 00:28:30.722434','admin','2026-07-28 00:28:30.722434','admin',0),(27,2,'M','SBUT',11,11,'2026-07-28 00:28:30.796746','admin','2026-07-28 00:28:30.796746','admin',0),(28,2,'N','SK Interior',12,12,'2026-07-28 00:28:30.874363','admin','2026-07-28 00:28:30.874363','admin',0),(29,2,'O','Sukoon',13,13,'2026-07-28 00:28:30.955243','admin','2026-07-28 00:28:30.955243','admin',0),(30,2,'P','ANV',14,14,'2026-07-28 00:28:31.034736','admin','2026-07-28 00:28:31.034736','admin',0),(31,2,'Q','Rocks & Logs',15,15,'2026-07-28 00:28:31.113467','admin','2026-07-28 00:28:31.113467','admin',0),(32,2,'R','Innovator Façade',16,16,'2026-07-28 00:28:31.195184','admin','2026-07-28 00:28:31.195184','admin',0);
/*!40000 ALTER TABLE `stock_import_location_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_import_rows`
--

DROP TABLE IF EXISTS `stock_import_rows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_import_rows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `source_excel_row` int NOT NULL,
  `source_excel_column` varchar(5) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_sr_number` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `normalized_item_suggestion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mapped_item_id` bigint DEFAULT NULL,
  `source_location_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mapped_party_id` bigint DEFAULT NULL,
  `mapped_site_id` bigint DEFAULT NULL,
  `mapped_godown_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `snapshot_date` date NOT NULL,
  `target_stock_bucket` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `opening_transaction_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `validation_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `validation_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duplicate_confirmed` tinyint(1) NOT NULL DEFAULT '0',
  `excluded` tinyint(1) NOT NULL DEFAULT '0',
  `exclusion_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `posted_stock_transaction_id` bigint DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_import_rows_cell` (`batch_id`,`source_excel_row`,`source_excel_column`),
  KEY `fk_stock_import_rows_party` (`mapped_party_id`),
  KEY `fk_stock_import_rows_site` (`mapped_site_id`),
  KEY `fk_stock_import_rows_posted_transaction` (`posted_stock_transaction_id`),
  KEY `idx_stock_import_rows_batch_status` (`batch_id`,`validation_status`),
  KEY `idx_stock_import_rows_item` (`mapped_item_id`),
  KEY `idx_stock_import_rows_location` (`batch_id`,`source_excel_column`),
  CONSTRAINT `fk_stock_import_rows_batch` FOREIGN KEY (`batch_id`) REFERENCES `stock_import_batches` (`id`),
  CONSTRAINT `fk_stock_import_rows_item` FOREIGN KEY (`mapped_item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_stock_import_rows_party` FOREIGN KEY (`mapped_party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_stock_import_rows_posted_transaction` FOREIGN KEY (`posted_stock_transaction_id`) REFERENCES `stock_transactions` (`id`),
  CONSTRAINT `fk_stock_import_rows_site` FOREIGN KEY (`mapped_site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `ck_stock_import_rows_bucket` CHECK ((`target_stock_bucket` in (_utf8mb4'AVAILABLE',_utf8mb4'ISSUED'))),
  CONSTRAINT `ck_stock_import_rows_location` CHECK ((`location_type` in (_utf8mb4'GODOWN',_utf8mb4'PARTY_OR_SITE'))),
  CONSTRAINT `ck_stock_import_rows_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `ck_stock_import_rows_transaction` CHECK ((`opening_transaction_type` in (_utf8mb4'OPENING_GODOWN_BALANCE',_utf8mb4'OPENING_SITE_BALANCE'))),
  CONSTRAINT `ck_stock_import_rows_validation` CHECK ((`validation_status` in (_utf8mb4'READY',_utf8mb4'MAPPING_REQUIRED',_utf8mb4'WARNING',_utf8mb4'ERROR',_utf8mb4'POSTED',_utf8mb4'REVERSED')))
) ENGINE=InnoDB AUTO_INCREMENT=305 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_import_rows`
--

LOCK TABLES `stock_import_rows` WRITE;
/*!40000 ALTER TABLE `stock_import_rows` DISABLE KEYS */;
INSERT INTO `stock_import_rows` VALUES (1,1,5,'C','1','H frames','H frames',1,'4m Façade',1,1,NULL,'PARTY_OR_SITE',33.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,1,'2026-07-28 00:08:22.451884','2026-07-28 00:08:31.956417',3),(2,1,5,'D','1','H frames','H frames',1,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,2,'2026-07-28 00:08:22.460469','2026-07-28 00:08:31.963761',3),(3,1,5,'E','1','H frames','H frames',1,'Avighnaa Kandivali',3,3,NULL,'PARTY_OR_SITE',319.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,3,'2026-07-28 00:08:22.468782','2026-07-28 00:08:31.970493',3),(4,1,5,'G','1','H frames','H frames',1,'Imperial',5,5,NULL,'PARTY_OR_SITE',151.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,4,'2026-07-28 00:08:22.476261','2026-07-28 00:08:31.976985',3),(5,1,5,'H','1','H frames','H frames',1,'Noble',6,6,NULL,'PARTY_OR_SITE',190.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,5,'2026-07-28 00:08:22.483791','2026-07-28 00:08:31.985000',3),(6,1,5,'J','1','H frames','H frames',1,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',287.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,6,'2026-07-28 00:08:22.490737','2026-07-28 00:08:31.992109',3),(7,1,5,'M','1','H frames','H frames',1,'SBUT',11,11,NULL,'PARTY_OR_SITE',1031.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,7,'2026-07-28 00:08:22.497469','2026-07-28 00:08:32.001391',3),(8,1,5,'O','1','H frames','H frames',1,'Sukoon',13,13,NULL,'PARTY_OR_SITE',130.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,8,'2026-07-28 00:08:22.504482','2026-07-28 00:08:32.010502',3),(9,1,5,'U','1','H frames','H frames',1,'Godown',NULL,NULL,'MAIN','GODOWN',1457.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,9,'2026-07-28 00:08:22.513326','2026-07-28 00:08:32.010522',2),(10,1,6,'U','2','Out size H frame','Out size H frame',2,'Godown',NULL,NULL,'MAIN','GODOWN',100.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED','Out size H frame requires an explicit separate-item or variant decision. Decision explicitly confirmed.',1,0,NULL,10,'2026-07-28 00:08:22.516223','2026-07-28 00:08:32.019297',2),(11,1,7,'U','3','Damage H frame','Damage H frame',3,'Godown',NULL,NULL,'MAIN','GODOWN',250.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED','Damage H frame may be a stock condition; create a separate item or exclude pending clarification. Decision explicitly confirmed.',1,0,NULL,11,'2026-07-28 00:08:22.518207','2026-07-28 00:08:32.025013',2),(12,1,8,'C','4','7.5 ft Bracings','7.5 ft Bracings',4,'4m Façade',1,1,NULL,'PARTY_OR_SITE',66.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,12,'2026-07-28 00:08:22.525936','2026-07-28 00:08:32.037169',3),(13,1,8,'D','4','7.5 ft Bracings','7.5 ft Bracings',4,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,13,'2026-07-28 00:08:22.533306','2026-07-28 00:08:32.044921',3),(14,1,8,'E','4','7.5 ft Bracings','7.5 ft Bracings',4,'Avighnaa Kandivali',3,3,NULL,'PARTY_OR_SITE',310.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,14,'2026-07-28 00:08:22.539350','2026-07-28 00:08:32.053341',3),(15,1,8,'G','4','7.5 ft Bracings','7.5 ft Bracings',4,'Imperial',5,5,NULL,'PARTY_OR_SITE',246.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,15,'2026-07-28 00:08:22.546629','2026-07-28 00:08:32.060257',3),(16,1,8,'H','4','7.5 ft Bracings','7.5 ft Bracings',4,'Noble',6,6,NULL,'PARTY_OR_SITE',462.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,16,'2026-07-28 00:08:22.553170','2026-07-28 00:08:32.068528',3),(17,1,8,'J','4','7.5 ft Bracings','7.5 ft Bracings',4,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',431.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,17,'2026-07-28 00:08:22.559505','2026-07-28 00:08:32.075725',3),(18,1,8,'M','4','7.5 ft Bracings','7.5 ft Bracings',4,'SBUT',11,11,NULL,'PARTY_OR_SITE',1722.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,18,'2026-07-28 00:08:22.566404','2026-07-28 00:08:32.083839',3),(19,1,8,'O','4','7.5 ft Bracings','7.5 ft Bracings',4,'Sukoon',13,13,NULL,'PARTY_OR_SITE',167.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,19,'2026-07-28 00:08:22.572148','2026-07-28 00:08:32.093950',3),(20,1,8,'U','4','7.5 ft Bracings','7.5 ft Bracings',4,'Godown',NULL,NULL,'MAIN','GODOWN',2630.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,20,'2026-07-28 00:08:22.577465','2026-07-28 00:08:32.093970',2),(21,1,9,'U','5','6ft Bracing','6ft Bracing',5,'Godown',NULL,NULL,'MAIN','GODOWN',205.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,21,'2026-07-28 00:08:22.583828','2026-07-28 00:08:32.102785',2),(22,1,10,'U','6','7ft Bracing','7ft Bracing',6,'Godown',NULL,NULL,'MAIN','GODOWN',100.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,22,'2026-07-28 00:08:22.589557','2026-07-28 00:08:32.108800',2),(23,1,11,'C','7','Platforms','Platforms',7,'4m Façade',1,1,NULL,'PARTY_OR_SITE',27.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,23,'2026-07-28 00:08:22.595077','2026-07-28 00:08:32.121324',3),(24,1,11,'D','7','Platforms','Platforms',7,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',102.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,24,'2026-07-28 00:08:22.601062','2026-07-28 00:08:32.128739',3),(25,1,11,'G','7','Platforms','Platforms',7,'Imperial',5,5,NULL,'PARTY_OR_SITE',15.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,25,'2026-07-28 00:08:22.606423','2026-07-28 00:08:32.136194',3),(26,1,11,'H','7','Platforms','Platforms',7,'Noble',6,6,NULL,'PARTY_OR_SITE',85.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,26,'2026-07-28 00:08:22.611653','2026-07-28 00:08:32.143141',3),(27,1,11,'I','7','Platforms','Platforms',7,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',31.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,27,'2026-07-28 00:08:22.617760','2026-07-28 00:08:32.155478',3),(28,1,11,'J','7','Platforms','Platforms',7,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',252.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,28,'2026-07-28 00:08:22.623305','2026-07-28 00:08:32.162737',3),(29,1,11,'K','7','Platforms','Platforms',7,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',255.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,29,'2026-07-28 00:08:22.628947','2026-07-28 00:08:32.169722',3),(30,1,11,'M','7','Platforms','Platforms',7,'SBUT',11,11,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,30,'2026-07-28 00:08:22.635082','2026-07-28 00:08:32.176166',3),(31,1,11,'N','7','Platforms','Platforms',7,'SK Interior',12,12,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,31,'2026-07-28 00:08:22.640702','2026-07-28 00:08:32.183635',3),(32,1,11,'O','7','Platforms','Platforms',7,'Sukoon',13,13,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,32,'2026-07-28 00:08:22.646903','2026-07-28 00:08:32.260659',3),(33,1,11,'P','7','Platforms','Platforms',7,'ANV',14,14,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,33,'2026-07-28 00:08:22.660458','2026-07-28 00:08:32.268856',3),(34,1,11,'Q','7','Platforms','Platforms',7,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,34,'2026-07-28 00:08:22.676184','2026-07-28 00:08:32.287977',3),(35,1,11,'U','7','Platforms','Platforms',7,'Godown',NULL,NULL,'MAIN','GODOWN',900.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,35,'2026-07-28 00:08:22.685700','2026-07-28 00:08:32.287995',2),(36,1,12,'D','8','3m vertical','3m vertical',8,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',267.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,36,'2026-07-28 00:08:22.692641','2026-07-28 00:08:32.299648',3),(37,1,12,'F','8','3m vertical','3m vertical',8,'Engarc',4,4,NULL,'PARTY_OR_SITE',190.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,37,'2026-07-28 00:08:22.700962','2026-07-28 00:08:32.306530',3),(38,1,12,'I','8','3m vertical','3m vertical',8,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',140.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,38,'2026-07-28 00:08:22.707020','2026-07-28 00:08:32.313946',3),(39,1,12,'K','8','3m vertical','3m vertical',8,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',230.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,39,'2026-07-28 00:08:22.713442','2026-07-28 00:08:32.321386',3),(40,1,12,'L','8','3m vertical','3m vertical',8,'Epilson',10,10,NULL,'PARTY_OR_SITE',120.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,40,'2026-07-28 00:08:22.720439','2026-07-28 00:08:32.328322',3),(41,1,12,'Q','8','3m vertical','3m vertical',8,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,41,'2026-07-28 00:08:22.726188','2026-07-28 00:08:32.336036',3),(42,1,12,'R','8','3m vertical','3m vertical',8,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',160.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,42,'2026-07-28 00:08:22.732688','2026-07-28 00:08:32.344571',3),(43,1,12,'U','8','3m vertical','3m vertical',8,'Godown',NULL,NULL,'MAIN','GODOWN',1020.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,43,'2026-07-28 00:08:22.738521','2026-07-28 00:08:32.344589',2),(44,1,13,'F','9','2.5m vertical','2.5m vertical',9,'Engarc',4,4,NULL,'PARTY_OR_SITE',120.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,44,'2026-07-28 00:08:22.751836','2026-07-28 00:08:32.356454',3),(45,1,13,'L','9','2.5m vertical','2.5m vertical',9,'Epilson',10,10,NULL,'PARTY_OR_SITE',155.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,45,'2026-07-28 00:08:22.757604','2026-07-28 00:08:32.365441',3),(46,1,13,'U','9','2.5m vertical','2.5m vertical',9,'Godown',NULL,NULL,'MAIN','GODOWN',120.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,46,'2026-07-28 00:08:22.763712','2026-07-28 00:08:32.365461',2),(47,1,14,'D','10','2m vertical','2m vertical',10,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,47,'2026-07-28 00:08:22.769831','2026-07-28 00:08:32.377844',3),(48,1,14,'F','10','2m vertical','2m vertical',10,'Engarc',4,4,NULL,'PARTY_OR_SITE',40.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,48,'2026-07-28 00:08:22.775396','2026-07-28 00:08:32.385824',3),(49,1,14,'K','10','2m vertical','2m vertical',10,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',150.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,49,'2026-07-28 00:08:22.781612','2026-07-28 00:08:32.392649',3),(50,1,14,'N','10','2m vertical','2m vertical',10,'SK Interior',12,12,NULL,'PARTY_OR_SITE',210.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,50,'2026-07-28 00:08:22.787634','2026-07-28 00:08:32.400624',3),(51,1,14,'P','10','2m vertical','2m vertical',10,'ANV',14,14,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,51,'2026-07-28 00:08:22.797265','2026-07-28 00:08:32.409290',3),(52,1,14,'U','10','2m vertical','2m vertical',10,'Godown',NULL,NULL,'MAIN','GODOWN',1110.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,52,'2026-07-28 00:08:22.803970','2026-07-28 00:08:32.409308',2),(53,1,15,'D','11','1m vertical','1m vertical',11,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,53,'2026-07-28 00:08:22.810397','2026-07-28 00:08:32.422402',3),(54,1,15,'F','11','1m vertical','1m vertical',11,'Engarc',4,4,NULL,'PARTY_OR_SITE',180.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,54,'2026-07-28 00:08:22.817303','2026-07-28 00:08:32.431807',3),(55,1,15,'U','11','1m vertical','1m vertical',11,'Godown',NULL,NULL,'MAIN','GODOWN',150.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,55,'2026-07-28 00:08:22.822610','2026-07-28 00:08:32.431827',2),(56,1,16,'D','12','1.5m vertical','1.5m vertical',12,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',30.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,56,'2026-07-28 00:08:22.827742','2026-07-28 00:08:32.443612',3),(57,1,16,'F','12','1.5m vertical','1.5m vertical',12,'Engarc',4,4,NULL,'PARTY_OR_SITE',600.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,57,'2026-07-28 00:08:22.833786','2026-07-28 00:08:32.453155',3),(58,1,16,'U','12','1.5m vertical','1.5m vertical',12,'Godown',NULL,NULL,'MAIN','GODOWN',150.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,58,'2026-07-28 00:08:22.838675','2026-07-28 00:08:32.453174',2),(59,1,17,'I','13','2m ledger','2m ledger',13,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',385.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,59,'2026-07-28 00:08:22.844712','2026-07-28 00:08:32.465436',3),(60,1,17,'R','13','2m ledger','2m ledger',13,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',580.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,60,'2026-07-28 00:08:22.850109','2026-07-28 00:08:32.473890',3),(61,1,17,'U','13','2m ledger','2m ledger',13,'Godown',NULL,NULL,'MAIN','GODOWN',1270.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,61,'2026-07-28 00:08:22.855662','2026-07-28 00:08:32.473908',2),(62,1,18,'D','14','1850m ledger','1850m ledger',14,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',300.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,62,'2026-07-28 00:08:22.861369','2026-07-28 00:08:32.487576',3),(63,1,18,'U','14','1850m ledger','1850m ledger',14,'Godown',NULL,NULL,'MAIN','GODOWN',420.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,63,'2026-07-28 00:08:22.868901','2026-07-28 00:08:32.487596',2),(64,1,19,'I','15','1.5m ledger','1.5m ledger',15,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',300.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,64,'2026-07-28 00:08:22.874275','2026-07-28 00:08:32.499879',3),(65,1,19,'N','15','1.5m ledger','1.5m ledger',15,'SK Interior',12,12,NULL,'PARTY_OR_SITE',240.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,65,'2026-07-28 00:08:22.880555','2026-07-28 00:08:32.506928',3),(66,1,19,'Q','15','1.5m ledger','1.5m ledger',15,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',120.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,66,'2026-07-28 00:08:22.886031','2026-07-28 00:08:32.516105',3),(67,1,19,'U','15','1.5m ledger','1.5m ledger',15,'Godown',NULL,NULL,'MAIN','GODOWN',2534.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,67,'2026-07-28 00:08:22.890676','2026-07-28 00:08:32.516124',2),(68,1,20,'U','16','1480m Ledger','1480m Ledger',16,'Godown',NULL,NULL,'MAIN','GODOWN',300.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,68,'2026-07-28 00:08:22.896041','2026-07-28 00:08:32.523452',2),(69,1,21,'K','17','1450m ledger','1450m ledger',17,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',615.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,69,'2026-07-28 00:08:22.907769','2026-07-28 00:08:32.535385',3),(70,1,21,'P','17','1450m ledger','1450m ledger',17,'ANV',14,14,NULL,'PARTY_OR_SITE',220.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,70,'2026-07-28 00:08:22.913193','2026-07-28 00:08:32.543969',3),(71,1,21,'U','17','1450m ledger','1450m ledger',17,'Godown',NULL,NULL,'MAIN','GODOWN',1553.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,71,'2026-07-28 00:08:22.919416','2026-07-28 00:08:32.543987',2),(72,1,22,'F','18','1150m ledger','1150m ledger',18,'Engarc',4,4,NULL,'PARTY_OR_SITE',736.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,72,'2026-07-28 00:08:22.924042','2026-07-28 00:08:32.558070',3),(73,1,22,'U','18','1150m ledger','1150m ledger',18,'Godown',NULL,NULL,'MAIN','GODOWN',2450.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,73,'2026-07-28 00:08:22.929257','2026-07-28 00:08:32.558088',2),(74,1,23,'F','19','1m ledger','1m ledger',19,'Engarc',4,4,NULL,'PARTY_OR_SITE',545.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,74,'2026-07-28 00:08:22.936062','2026-07-28 00:08:32.570767',3),(75,1,23,'L','19','1m ledger','1m ledger',19,'Epilson',10,10,NULL,'PARTY_OR_SITE',47.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,75,'2026-07-28 00:08:22.940574','2026-07-28 00:08:32.581185',3),(76,1,23,'N','19','1m ledger','1m ledger',19,'SK Interior',12,12,NULL,'PARTY_OR_SITE',140.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,76,'2026-07-28 00:08:22.945945','2026-07-28 00:08:32.590980',3),(77,1,23,'Q','19','1m ledger','1m ledger',19,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',72.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,77,'2026-07-28 00:08:22.952494','2026-07-28 00:08:32.602553',3),(78,1,23,'R','19','1m ledger','1m ledger',19,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',580.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,78,'2026-07-28 00:08:22.957550','2026-07-28 00:08:32.616243',3),(79,1,24,'D','20','950m Ledger','950m Ledger',20,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',500.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,79,'2026-07-28 00:08:22.962530','2026-07-28 00:08:32.628418',3),(80,1,25,'D','21','980m ledger','980m ledger',21,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',500.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,80,'2026-07-28 00:08:22.968772','2026-07-28 00:08:32.636737',3),(81,1,25,'F','21','980m ledger','980m ledger',21,'Engarc',4,4,NULL,'PARTY_OR_SITE',293.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,81,'2026-07-28 00:08:22.973589','2026-07-28 00:08:32.644269',3),(82,1,25,'K','21','980m ledger','980m ledger',21,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',734.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,82,'2026-07-28 00:08:22.978548','2026-07-28 00:08:32.654551',3),(83,1,25,'U','21','980m ledger','980m ledger',21,'Godown',NULL,NULL,'MAIN','GODOWN',2595.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,83,'2026-07-28 00:08:22.985652','2026-07-28 00:08:32.654569',2),(84,1,26,'H','22','20ft pipe','20ft pipe',22,'Noble',6,6,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,84,'2026-07-28 00:08:22.990585','2026-07-28 00:08:32.667837',3),(85,1,26,'O','22','20ft pipe','20ft pipe',22,'Sukoon',13,13,NULL,'PARTY_OR_SITE',12.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,85,'2026-07-28 00:08:22.996427','2026-07-28 00:08:32.676832',3),(86,1,26,'U','22','20ft pipe','20ft pipe',22,'Godown',NULL,NULL,'MAIN','GODOWN',408.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,86,'2026-07-28 00:08:23.002750','2026-07-28 00:08:32.676853',2),(87,1,27,'C','23','10ft pipe','10ft pipe',23,'4m Façade',1,1,NULL,'PARTY_OR_SITE',4.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,87,'2026-07-28 00:08:23.007841','2026-07-28 00:08:32.690045',3),(88,1,27,'G','23','10ft pipe','10ft pipe',23,'Imperial',5,5,NULL,'PARTY_OR_SITE',18.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,88,'2026-07-28 00:08:23.012916','2026-07-28 00:08:32.697991',3),(89,1,27,'H','23','10ft pipe','10ft pipe',23,'Noble',6,6,NULL,'PARTY_OR_SITE',39.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,89,'2026-07-28 00:08:23.018945','2026-07-28 00:08:32.706402',3),(90,1,27,'I','23','10ft pipe','10ft pipe',23,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',28.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,90,'2026-07-28 00:08:23.023440','2026-07-28 00:08:32.714504',3),(91,1,27,'J','23','10ft pipe','10ft pipe',23,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',39.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,91,'2026-07-28 00:08:23.027812','2026-07-28 00:08:32.722452',3),(92,1,27,'N','23','10ft pipe','10ft pipe',23,'SK Interior',12,12,NULL,'PARTY_OR_SITE',80.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,92,'2026-07-28 00:08:23.033954','2026-07-28 00:08:32.732605',3),(93,1,27,'O','23','10ft pipe','10ft pipe',23,'Sukoon',13,13,NULL,'PARTY_OR_SITE',44.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,93,'2026-07-28 00:08:23.038881','2026-07-28 00:08:32.741188',3),(94,1,27,'P','23','10ft pipe','10ft pipe',23,'ANV',14,14,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,94,'2026-07-28 00:08:23.043262','2026-07-28 00:08:32.750725',3),(95,1,27,'Q','23','10ft pipe','10ft pipe',23,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,95,'2026-07-28 00:08:23.049608','2026-07-28 00:08:32.760153',3),(96,1,27,'U','23','10ft pipe','10ft pipe',23,'Godown',NULL,NULL,'MAIN','GODOWN',850.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,96,'2026-07-28 00:08:23.055219','2026-07-28 00:08:32.760172',2),(97,1,28,'I','24','8ft pipe','8ft pipe',24,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',6.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,97,'2026-07-28 00:08:23.059470','2026-07-28 00:08:32.779547',3),(98,1,29,'U','25','U jack','U jack',25,'Godown',NULL,NULL,'MAIN','GODOWN',700.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,98,'2026-07-28 00:08:23.068661','2026-07-28 00:08:32.779574',2),(99,1,30,'C','26','Base jack','Base jack',26,'4m Façade',1,1,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,99,'2026-07-28 00:08:23.076812','2026-07-28 00:08:32.793001',3),(100,1,30,'D','26','Base jack','Base jack',26,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',126.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,100,'2026-07-28 00:08:23.085726','2026-07-28 00:08:32.803204',3),(101,1,30,'G','26','Base jack','Base jack',26,'Imperial',5,5,NULL,'PARTY_OR_SITE',58.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,101,'2026-07-28 00:08:23.093515','2026-07-28 00:08:32.812367',3),(102,1,30,'H','26','Base jack','Base jack',26,'Noble',6,6,NULL,'PARTY_OR_SITE',70.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,102,'2026-07-28 00:08:23.104814','2026-07-28 00:08:32.821752',3),(103,1,30,'I','26','Base jack','Base jack',26,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',41.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,103,'2026-07-28 00:08:23.159516','2026-07-28 00:08:32.830206',3),(104,1,30,'J','26','Base jack','Base jack',26,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',32.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,104,'2026-07-28 00:08:23.166493','2026-07-28 00:08:32.846281',3),(105,1,30,'K','26','Base jack','Base jack',26,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',133.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,105,'2026-07-28 00:08:23.172313','2026-07-28 00:08:32.854778',3),(106,1,30,'L','26','Base jack','Base jack',26,'Epilson',10,10,NULL,'PARTY_OR_SITE',96.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,106,'2026-07-28 00:08:23.177912','2026-07-28 00:08:32.863816',3),(107,1,30,'M','26','Base jack','Base jack',26,'SBUT',11,11,NULL,'PARTY_OR_SITE',74.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,107,'2026-07-28 00:08:23.184134','2026-07-28 00:08:32.872120',3),(108,1,30,'N','26','Base jack','Base jack',26,'SK Interior',12,12,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,108,'2026-07-28 00:08:23.202589','2026-07-28 00:08:32.880664',3),(109,1,30,'O','26','Base jack','Base jack',26,'Sukoon',13,13,NULL,'PARTY_OR_SITE',38.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,109,'2026-07-28 00:08:23.210814','2026-07-28 00:08:32.889842',3),(110,1,30,'P','26','Base jack','Base jack',26,'ANV',14,14,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,110,'2026-07-28 00:08:23.219765','2026-07-28 00:08:32.898594',3),(111,1,30,'Q','26','Base jack','Base jack',26,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,111,'2026-07-28 00:08:23.227797','2026-07-28 00:08:32.908480',3),(112,1,30,'U','26','Base jack','Base jack',26,'Godown',NULL,NULL,'MAIN','GODOWN',1150.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,112,'2026-07-28 00:08:23.235463','2026-07-28 00:08:32.908508',2),(113,1,31,'C','27','Coupler','Coupler',27,'4m Façade',1,1,NULL,'PARTY_OR_SITE',8.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,113,'2026-07-28 00:08:23.241015','2026-07-28 00:08:32.922313',3),(114,1,31,'G','27','Coupler','Coupler',27,'Imperial',5,5,NULL,'PARTY_OR_SITE',60.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,114,'2026-07-28 00:08:23.247349','2026-07-28 00:08:32.931317',3),(115,1,31,'H','27','Coupler','Coupler',27,'Noble',6,6,NULL,'PARTY_OR_SITE',229.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,115,'2026-07-28 00:08:23.255380','2026-07-28 00:08:32.939436',3),(116,1,31,'I','27','Coupler','Coupler',27,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',117.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,116,'2026-07-28 00:08:23.263773','2026-07-28 00:08:32.947882',3),(117,1,31,'J','27','Coupler','Coupler',27,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',90.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,117,'2026-07-28 00:08:23.271364','2026-07-28 00:08:32.956084',3),(118,1,31,'M','27','Coupler','Coupler',27,'SBUT',11,11,NULL,'PARTY_OR_SITE',200.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,118,'2026-07-28 00:08:23.279329','2026-07-28 00:08:32.964478',3),(119,1,31,'N','27','Coupler','Coupler',27,'SK Interior',12,12,NULL,'PARTY_OR_SITE',180.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,119,'2026-07-28 00:08:23.288064','2026-07-28 00:08:32.972750',3),(120,1,31,'O','27','Coupler','Coupler',27,'Sukoon',13,13,NULL,'PARTY_OR_SITE',114.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,120,'2026-07-28 00:08:23.293943','2026-07-28 00:08:32.981393',3),(121,1,31,'P','27','Coupler','Coupler',27,'ANV',14,14,NULL,'PARTY_OR_SITE',150.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,121,'2026-07-28 00:08:23.301578','2026-07-28 00:08:32.989706',3),(122,1,31,'Q','27','Coupler','Coupler',27,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',34.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,122,'2026-07-28 00:08:23.307437','2026-07-28 00:08:33.002773',3),(123,1,32,'U','28','Spiggot pin','Spiggot pin',28,'Godown',NULL,NULL,'MAIN','GODOWN',1400.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,123,'2026-07-28 00:08:23.313678','2026-07-28 00:08:33.002793',2),(124,1,33,'F','29','Joint pins','Joint pins',29,'Engarc',4,4,NULL,'PARTY_OR_SITE',1000.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,124,'2026-07-28 00:08:23.320010','2026-07-28 00:08:33.017136',3),(125,1,33,'H','29','Joint pins','Joint pins',29,'Noble',6,6,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,125,'2026-07-28 00:08:23.326445','2026-07-28 00:08:33.025907',3),(126,1,33,'I','29','Joint pins','Joint pins',29,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',146.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,126,'2026-07-28 00:08:23.334113','2026-07-28 00:08:33.035090',3),(127,1,33,'K','29','Joint pins','Joint pins',29,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',141.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,127,'2026-07-28 00:08:23.340006','2026-07-28 00:08:33.043608',3),(128,1,33,'N','29','Joint pins','Joint pins',29,'SK Interior',12,12,NULL,'PARTY_OR_SITE',175.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,128,'2026-07-28 00:08:23.346410','2026-07-28 00:08:33.053268',3),(129,1,33,'P','29','Joint pins','Joint pins',29,'ANV',14,14,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,129,'2026-07-28 00:08:23.352533','2026-07-28 00:08:33.061400',3),(130,1,33,'Q','29','Joint pins','Joint pins',29,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,130,'2026-07-28 00:08:23.358135','2026-07-28 00:08:33.070361',3),(131,1,33,'R','29','Joint pins','Joint pins',29,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',80.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,131,'2026-07-28 00:08:23.363091','2026-07-28 00:08:33.080714',3),(132,1,33,'U','29','Joint pins','Joint pins',29,'Godown',NULL,NULL,'MAIN','GODOWN',800.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,132,'2026-07-28 00:08:23.367833','2026-07-28 00:08:33.080734',2),(133,1,34,'I','30','Props','Props',30,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',2.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,133,'2026-07-28 00:08:23.372013','2026-07-28 00:08:33.094858',3),(134,1,34,'O','30','Props','Props',30,'Sukoon',13,13,NULL,'PARTY_OR_SITE',4.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,134,'2026-07-28 00:08:23.376128','2026-07-28 00:08:33.106420',3),(135,1,34,'U','30','Props','Props',30,'Godown',NULL,NULL,'MAIN','GODOWN',260.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,135,'2026-07-28 00:08:23.381064','2026-07-28 00:08:33.106438',2),(136,1,35,'H','31','Castor wheel','Castor wheel',31,'Noble',6,6,NULL,'PARTY_OR_SITE',4.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,136,'2026-07-28 00:08:23.385679','2026-07-28 00:08:33.124453',3),(137,1,36,'J','32','7ft pipe','7ft pipe',32,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',6.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,137,'2026-07-28 00:08:23.389910','2026-07-28 00:08:33.134553',3),(138,1,36,'P','32','7ft pipe','7ft pipe',32,'ANV',14,14,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,138,'2026-07-28 00:08:23.395897','2026-07-28 00:08:33.147014',3),(139,1,36,'U','32','7ft pipe','7ft pipe',32,'Godown',NULL,NULL,'MAIN','GODOWN',130.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,139,'2026-07-28 00:08:23.400954','2026-07-28 00:08:33.147036',2),(140,1,37,'I','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,140,'2026-07-28 00:08:23.402174','2026-07-28 00:08:33.164133',3),(141,1,37,'M','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'SBUT',11,11,NULL,'PARTY_OR_SITE',195.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,141,'2026-07-28 00:08:23.403237','2026-07-28 00:08:33.174214',3),(142,1,37,'Q','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',6.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,142,'2026-07-28 00:08:23.404239','2026-07-28 00:08:33.186480',3),(143,1,37,'U','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'Godown',NULL,NULL,'MAIN','GODOWN',200.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,143,'2026-07-28 00:08:23.405294','2026-07-28 00:08:33.186501',2),(144,1,38,'G','34','10ft chaneel','10ft chaneel',34,'Imperial',5,5,NULL,'PARTY_OR_SITE',2.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,144,'2026-07-28 00:08:23.409295','2026-07-28 00:08:33.212260',3),(145,1,39,'G','35','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',35,'Imperial',5,5,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,145,'2026-07-28 00:08:23.410459','2026-07-28 00:08:33.233394',3),(146,1,40,'N','36','10f ladder','10f ladder',36,'SK Interior',12,12,NULL,'PARTY_OR_SITE',25.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,146,'2026-07-28 00:08:23.415140','2026-07-28 00:08:33.252833',3),(147,1,41,'U','37','Ladder pipe','Ladder pipe',37,'Godown',NULL,NULL,'MAIN','GODOWN',30.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,147,'2026-07-28 00:08:23.419481','2026-07-28 00:08:33.252855',2),(148,1,42,'U','38','Ladder steps','Ladder steps',38,'Godown',NULL,NULL,'MAIN','GODOWN',112.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,148,'2026-07-28 00:08:23.423567','2026-07-28 00:08:33.265519',2),(149,1,43,'U','39','20ft Aluminium Ladder','20ft Aluminium Ladder',39,'Godown',NULL,NULL,'MAIN','GODOWN',5.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,149,'2026-07-28 00:08:23.427670','2026-07-28 00:08:33.277722',2),(150,1,44,'U','40','7.5 ft Iron Ladder','7.5 ft Iron Ladder',40,'Godown',NULL,NULL,'MAIN','GODOWN',23.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','POSTED',NULL,1,0,NULL,150,'2026-07-28 00:08:23.432415','2026-07-28 00:08:33.290521',2),(151,1,45,'N','41','Ladder coupler','Ladder coupler',41,'SK Interior',12,12,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,151,'2026-07-28 00:08:23.436683','2026-07-28 00:08:33.315953',3),(152,1,46,'N','42','Toe board','Toe board',42,'SK Interior',12,12,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','POSTED',NULL,1,0,NULL,152,'2026-07-28 00:08:23.440728','2026-07-28 00:08:33.324009',3),(153,2,5,'C','1','H frames','H frames',1,'4m Façade',1,1,NULL,'PARTY_OR_SITE',33.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,195,'2026-07-28 00:28:03.499292','2026-07-28 00:29:42.792473',3),(154,2,5,'D','1','H frames','H frames',1,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,196,'2026-07-28 00:28:03.505756','2026-07-28 00:29:42.802655',3),(155,2,5,'E','1','H frames','H frames',1,'Avighnaa Kandivali',3,3,NULL,'PARTY_OR_SITE',319.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,197,'2026-07-28 00:28:03.510848','2026-07-28 00:29:42.812545',3),(156,2,5,'G','1','H frames','H frames',1,'Imperial',5,5,NULL,'PARTY_OR_SITE',151.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,198,'2026-07-28 00:28:03.515590','2026-07-28 00:29:42.822276',3),(157,2,5,'H','1','H frames','H frames',1,'Noble',6,6,NULL,'PARTY_OR_SITE',190.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,199,'2026-07-28 00:28:03.520899','2026-07-28 00:29:42.832327',3),(158,2,5,'J','1','H frames','H frames',1,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',287.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,200,'2026-07-28 00:28:03.526050','2026-07-28 00:29:42.841262',3),(159,2,5,'M','1','H frames','H frames',1,'SBUT',11,11,NULL,'PARTY_OR_SITE',1031.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,201,'2026-07-28 00:28:03.531291','2026-07-28 00:29:42.851349',3),(160,2,5,'O','1','H frames','H frames',1,'Sukoon',13,13,NULL,'PARTY_OR_SITE',130.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,202,'2026-07-28 00:28:03.537431','2026-07-28 00:29:42.860728',3),(161,2,5,'U','1','H frames','H frames',1,'Godown',NULL,NULL,'MAIN','GODOWN',1457.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,203,'2026-07-28 00:28:03.542650','2026-07-28 00:29:42.868860',2),(162,2,6,'U','2','Out size H frame','Out size H frame',2,'Godown',NULL,NULL,'MAIN','GODOWN',100.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED','Out size H frame requires an explicit separate-item or variant decision. Decision explicitly confirmed.',1,0,NULL,204,'2026-07-28 00:28:03.544955','2026-07-28 00:29:42.875526',4),(163,2,7,'U','3','Damage H frame','Damage H frame',3,'Godown',NULL,NULL,'MAIN','GODOWN',250.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED','Damage H frame may be a stock condition; create a separate item or exclude pending clarification. Decision explicitly confirmed.',1,0,NULL,205,'2026-07-28 00:28:03.546674','2026-07-28 00:29:42.882483',4),(164,2,8,'C','4','7.5 ft Bracings','7.5 ft Bracings',4,'4m Façade',1,1,NULL,'PARTY_OR_SITE',66.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,206,'2026-07-28 00:28:03.553872','2026-07-28 00:29:42.889487',3),(165,2,8,'D','4','7.5 ft Bracings','7.5 ft Bracings',4,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,207,'2026-07-28 00:28:03.561415','2026-07-28 00:29:42.897207',3),(166,2,8,'E','4','7.5 ft Bracings','7.5 ft Bracings',4,'Avighnaa Kandivali',3,3,NULL,'PARTY_OR_SITE',310.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,208,'2026-07-28 00:28:03.566804','2026-07-28 00:29:42.904414',3),(167,2,8,'G','4','7.5 ft Bracings','7.5 ft Bracings',4,'Imperial',5,5,NULL,'PARTY_OR_SITE',246.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,209,'2026-07-28 00:28:03.572091','2026-07-28 00:29:42.911206',3),(168,2,8,'H','4','7.5 ft Bracings','7.5 ft Bracings',4,'Noble',6,6,NULL,'PARTY_OR_SITE',462.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,210,'2026-07-28 00:28:03.577052','2026-07-28 00:29:42.919447',3),(169,2,8,'J','4','7.5 ft Bracings','7.5 ft Bracings',4,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',431.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,211,'2026-07-28 00:28:03.581023','2026-07-28 00:29:42.926814',3),(170,2,8,'M','4','7.5 ft Bracings','7.5 ft Bracings',4,'SBUT',11,11,NULL,'PARTY_OR_SITE',1722.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,212,'2026-07-28 00:28:03.586774','2026-07-28 00:29:42.935498',3),(171,2,8,'O','4','7.5 ft Bracings','7.5 ft Bracings',4,'Sukoon',13,13,NULL,'PARTY_OR_SITE',167.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,213,'2026-07-28 00:28:03.591093','2026-07-28 00:29:42.942752',3),(172,2,8,'U','4','7.5 ft Bracings','7.5 ft Bracings',4,'Godown',NULL,NULL,'MAIN','GODOWN',2630.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,214,'2026-07-28 00:28:03.595249','2026-07-28 00:29:42.949459',2),(173,2,9,'U','5','6ft Bracing','6ft Bracing',5,'Godown',NULL,NULL,'MAIN','GODOWN',205.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,215,'2026-07-28 00:28:03.602121','2026-07-28 00:29:42.954669',2),(174,2,10,'U','6','7ft Bracing','7ft Bracing',6,'Godown',NULL,NULL,'MAIN','GODOWN',100.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,216,'2026-07-28 00:28:03.608031','2026-07-28 00:29:42.959647',2),(175,2,11,'C','7','Platforms','Platforms',7,'4m Façade',1,1,NULL,'PARTY_OR_SITE',27.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,217,'2026-07-28 00:28:03.613670','2026-07-28 00:29:42.967764',3),(176,2,11,'D','7','Platforms','Platforms',7,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',102.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,218,'2026-07-28 00:28:03.844474','2026-07-28 00:29:42.974125',3),(177,2,11,'G','7','Platforms','Platforms',7,'Imperial',5,5,NULL,'PARTY_OR_SITE',15.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,219,'2026-07-28 00:28:03.847909','2026-07-28 00:29:42.981356',3),(178,2,11,'H','7','Platforms','Platforms',7,'Noble',6,6,NULL,'PARTY_OR_SITE',85.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,220,'2026-07-28 00:28:03.852682','2026-07-28 00:29:42.987939',3),(179,2,11,'I','7','Platforms','Platforms',7,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',31.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,221,'2026-07-28 00:28:03.856559','2026-07-28 00:29:42.994300',3),(180,2,11,'J','7','Platforms','Platforms',7,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',252.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,222,'2026-07-28 00:28:03.859894','2026-07-28 00:29:43.001756',3),(181,2,11,'K','7','Platforms','Platforms',7,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',255.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,223,'2026-07-28 00:28:03.865013','2026-07-28 00:29:43.008536',3),(182,2,11,'M','7','Platforms','Platforms',7,'SBUT',11,11,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,224,'2026-07-28 00:28:03.869869','2026-07-28 00:29:43.016582',3),(183,2,11,'N','7','Platforms','Platforms',7,'SK Interior',12,12,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,225,'2026-07-28 00:28:03.873240','2026-07-28 00:29:43.024226',3),(184,2,11,'O','7','Platforms','Platforms',7,'Sukoon',13,13,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,226,'2026-07-28 00:28:03.876589','2026-07-28 00:29:43.031703',3),(185,2,11,'P','7','Platforms','Platforms',7,'ANV',14,14,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,227,'2026-07-28 00:28:03.880710','2026-07-28 00:29:43.038351',3),(186,2,11,'Q','7','Platforms','Platforms',7,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,228,'2026-07-28 00:28:03.885671','2026-07-28 00:29:43.044772',3),(187,2,11,'U','7','Platforms','Platforms',7,'Godown',NULL,NULL,'MAIN','GODOWN',900.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,229,'2026-07-28 00:28:03.890036','2026-07-28 00:29:43.050603',2),(188,2,12,'D','8','3m vertical','3m vertical',8,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',267.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,230,'2026-07-28 00:28:03.894244','2026-07-28 00:29:43.057285',3),(189,2,12,'F','8','3m vertical','3m vertical',8,'Engarc',4,4,NULL,'PARTY_OR_SITE',190.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,231,'2026-07-28 00:28:03.897319','2026-07-28 00:29:43.064896',3),(190,2,12,'I','8','3m vertical','3m vertical',8,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',140.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,232,'2026-07-28 00:28:03.901836','2026-07-28 00:29:43.071895',3),(191,2,12,'K','8','3m vertical','3m vertical',8,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',230.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,233,'2026-07-28 00:28:03.905584','2026-07-28 00:29:43.078621',3),(192,2,12,'L','8','3m vertical','3m vertical',8,'Epilson',10,10,NULL,'PARTY_OR_SITE',120.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,234,'2026-07-28 00:28:03.908968','2026-07-28 00:29:43.086653',3),(193,2,12,'Q','8','3m vertical','3m vertical',8,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,235,'2026-07-28 00:28:03.912440','2026-07-28 00:29:43.093197',3),(194,2,12,'R','8','3m vertical','3m vertical',8,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',160.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,236,'2026-07-28 00:28:03.916062','2026-07-28 00:29:43.101329',3),(195,2,12,'U','8','3m vertical','3m vertical',8,'Godown',NULL,NULL,'MAIN','GODOWN',1020.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,237,'2026-07-28 00:28:03.920133','2026-07-28 00:29:43.106939',2),(196,2,13,'F','9','2.5m vertical','2.5m vertical',9,'Engarc',4,4,NULL,'PARTY_OR_SITE',120.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,238,'2026-07-28 00:28:03.924403','2026-07-28 00:29:43.114167',3),(197,2,13,'L','9','2.5m vertical','2.5m vertical',9,'Epilson',10,10,NULL,'PARTY_OR_SITE',155.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,239,'2026-07-28 00:28:03.927706','2026-07-28 00:29:43.121710',3),(198,2,13,'U','9','2.5m vertical','2.5m vertical',9,'Godown',NULL,NULL,'MAIN','GODOWN',120.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,240,'2026-07-28 00:28:03.931476','2026-07-28 00:29:43.127274',2),(199,2,14,'D','10','2m vertical','2m vertical',10,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,241,'2026-07-28 00:28:03.936431','2026-07-28 00:29:44.273684',3),(200,2,14,'F','10','2m vertical','2m vertical',10,'Engarc',4,4,NULL,'PARTY_OR_SITE',40.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,242,'2026-07-28 00:28:03.940009','2026-07-28 00:29:44.355364',3),(201,2,14,'K','10','2m vertical','2m vertical',10,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',150.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,243,'2026-07-28 00:28:03.943380','2026-07-28 00:29:44.363181',3),(202,2,14,'N','10','2m vertical','2m vertical',10,'SK Interior',12,12,NULL,'PARTY_OR_SITE',210.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,244,'2026-07-28 00:28:03.947098','2026-07-28 00:29:44.370715',3),(203,2,14,'P','10','2m vertical','2m vertical',10,'ANV',14,14,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,245,'2026-07-28 00:28:03.950774','2026-07-28 00:29:44.378625',3),(204,2,14,'U','10','2m vertical','2m vertical',10,'Godown',NULL,NULL,'MAIN','GODOWN',1110.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,246,'2026-07-28 00:28:03.954637','2026-07-28 00:29:44.385275',2),(205,2,15,'D','11','1m vertical','1m vertical',11,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,247,'2026-07-28 00:28:03.959029','2026-07-28 00:29:44.392819',3),(206,2,15,'F','11','1m vertical','1m vertical',11,'Engarc',4,4,NULL,'PARTY_OR_SITE',180.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,248,'2026-07-28 00:28:03.962358','2026-07-28 00:29:44.402053',3),(207,2,15,'U','11','1m vertical','1m vertical',11,'Godown',NULL,NULL,'MAIN','GODOWN',150.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,249,'2026-07-28 00:28:03.965611','2026-07-28 00:29:44.409139',2),(208,2,16,'D','12','1.5m vertical','1.5m vertical',12,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',30.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,250,'2026-07-28 00:28:03.970659','2026-07-28 00:29:44.417554',3),(209,2,16,'F','12','1.5m vertical','1.5m vertical',12,'Engarc',4,4,NULL,'PARTY_OR_SITE',600.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,251,'2026-07-28 00:28:03.974203','2026-07-28 00:29:44.424947',3),(210,2,16,'U','12','1.5m vertical','1.5m vertical',12,'Godown',NULL,NULL,'MAIN','GODOWN',150.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,252,'2026-07-28 00:28:03.977600','2026-07-28 00:29:44.432248',2),(211,2,17,'I','13','2m ledger','2m ledger',13,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',385.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,253,'2026-07-28 00:28:03.982263','2026-07-28 00:29:44.439024',3),(212,2,17,'R','13','2m ledger','2m ledger',13,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',580.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,254,'2026-07-28 00:28:03.986581','2026-07-28 00:29:44.446723',3),(213,2,17,'U','13','2m ledger','2m ledger',13,'Godown',NULL,NULL,'MAIN','GODOWN',1270.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,255,'2026-07-28 00:28:03.990113','2026-07-28 00:29:44.453172',2),(214,2,18,'D','14','1850m ledger','1850m ledger',14,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',300.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,256,'2026-07-28 00:28:03.994357','2026-07-28 00:29:44.459847',3),(215,2,18,'U','14','1850m ledger','1850m ledger',14,'Godown',NULL,NULL,'MAIN','GODOWN',420.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,257,'2026-07-28 00:28:06.923421','2026-07-28 00:29:44.466966',2),(216,2,19,'I','15','1.5m ledger','1.5m ledger',15,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',300.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,258,'2026-07-28 00:28:06.928531','2026-07-28 00:29:44.473763',3),(217,2,19,'N','15','1.5m ledger','1.5m ledger',15,'SK Interior',12,12,NULL,'PARTY_OR_SITE',240.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,259,'2026-07-28 00:28:06.932565','2026-07-28 00:29:44.481569',3),(218,2,19,'Q','15','1.5m ledger','1.5m ledger',15,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',120.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,260,'2026-07-28 00:28:06.938319','2026-07-28 00:29:44.489041',3),(219,2,19,'U','15','1.5m ledger','1.5m ledger',15,'Godown',NULL,NULL,'MAIN','GODOWN',2534.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,261,'2026-07-28 00:28:06.941827','2026-07-28 00:29:44.494357',2),(220,2,20,'U','16','1480m Ledger','1480m Ledger',16,'Godown',NULL,NULL,'MAIN','GODOWN',300.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,262,'2026-07-28 00:28:06.946162','2026-07-28 00:29:44.500323',2),(221,2,21,'K','17','1450m ledger','1450m ledger',17,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',615.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,263,'2026-07-28 00:28:06.951222','2026-07-28 00:29:44.507210',3),(222,2,21,'P','17','1450m ledger','1450m ledger',17,'ANV',14,14,NULL,'PARTY_OR_SITE',220.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,264,'2026-07-28 00:28:06.955233','2026-07-28 00:29:44.514712',3),(223,2,21,'U','17','1450m ledger','1450m ledger',17,'Godown',NULL,NULL,'MAIN','GODOWN',1553.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,265,'2026-07-28 00:28:06.958674','2026-07-28 00:29:44.520669',2),(224,2,22,'F','18','1150m ledger','1150m ledger',18,'Engarc',4,4,NULL,'PARTY_OR_SITE',736.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,266,'2026-07-28 00:28:06.963099','2026-07-28 00:29:44.527330',3),(225,2,22,'U','18','1150m ledger','1150m ledger',18,'Godown',NULL,NULL,'MAIN','GODOWN',2450.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,267,'2026-07-28 00:28:06.966701','2026-07-28 00:29:44.533799',2),(226,2,23,'F','19','1m ledger','1m ledger',19,'Engarc',4,4,NULL,'PARTY_OR_SITE',545.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,268,'2026-07-28 00:28:06.971448','2026-07-28 00:29:44.540696',3),(227,2,23,'L','19','1m ledger','1m ledger',19,'Epilson',10,10,NULL,'PARTY_OR_SITE',47.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,269,'2026-07-28 00:28:06.974777','2026-07-28 00:29:44.549889',3),(228,2,23,'N','19','1m ledger','1m ledger',19,'SK Interior',12,12,NULL,'PARTY_OR_SITE',140.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,270,'2026-07-28 00:28:06.978316','2026-07-28 00:29:44.557726',3),(229,2,23,'Q','19','1m ledger','1m ledger',19,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',72.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,271,'2026-07-28 00:28:06.982088','2026-07-28 00:29:44.566826',3),(230,2,23,'R','19','1m ledger','1m ledger',19,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',580.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,272,'2026-07-28 00:28:06.986016','2026-07-28 00:29:44.574479',3),(231,2,24,'D','20','950m Ledger','950m Ledger',20,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',500.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,273,'2026-07-28 00:28:06.990343','2026-07-28 00:29:44.583994',3),(232,2,25,'D','21','980m ledger','980m ledger',21,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',500.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,274,'2026-07-28 00:28:06.994578','2026-07-28 00:29:44.591116',3),(233,2,25,'F','21','980m ledger','980m ledger',21,'Engarc',4,4,NULL,'PARTY_OR_SITE',293.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,275,'2026-07-28 00:28:06.997952','2026-07-28 00:29:44.600071',3),(234,2,25,'K','21','980m ledger','980m ledger',21,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',734.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,276,'2026-07-28 00:28:07.002103','2026-07-28 00:29:44.607429',3),(235,2,25,'U','21','980m ledger','980m ledger',21,'Godown',NULL,NULL,'MAIN','GODOWN',2595.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,277,'2026-07-28 00:28:07.005623','2026-07-28 00:29:44.613883',2),(236,2,26,'H','22','20ft pipe','20ft pipe',22,'Noble',6,6,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,278,'2026-07-28 00:28:07.009884','2026-07-28 00:29:44.622167',3),(237,2,26,'O','22','20ft pipe','20ft pipe',22,'Sukoon',13,13,NULL,'PARTY_OR_SITE',12.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,279,'2026-07-28 00:28:07.013055','2026-07-28 00:29:44.630378',3),(238,2,26,'U','22','20ft pipe','20ft pipe',22,'Godown',NULL,NULL,'MAIN','GODOWN',408.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,280,'2026-07-28 00:28:07.016470','2026-07-28 00:29:44.638087',2),(239,2,27,'C','23','10ft pipe','10ft pipe',23,'4m Façade',1,1,NULL,'PARTY_OR_SITE',4.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,281,'2026-07-28 00:28:07.021172','2026-07-28 00:29:44.648885',3),(240,2,27,'G','23','10ft pipe','10ft pipe',23,'Imperial',5,5,NULL,'PARTY_OR_SITE',18.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,282,'2026-07-28 00:28:07.024454','2026-07-28 00:29:44.662534',3),(241,2,27,'H','23','10ft pipe','10ft pipe',23,'Noble',6,6,NULL,'PARTY_OR_SITE',39.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,283,'2026-07-28 00:28:07.027727','2026-07-28 00:29:44.674861',3),(242,2,27,'I','23','10ft pipe','10ft pipe',23,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',28.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,284,'2026-07-28 00:28:07.030966','2026-07-28 00:29:44.685780',3),(243,2,27,'J','23','10ft pipe','10ft pipe',23,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',39.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,285,'2026-07-28 00:28:07.034642','2026-07-28 00:29:44.694667',3),(244,2,27,'N','23','10ft pipe','10ft pipe',23,'SK Interior',12,12,NULL,'PARTY_OR_SITE',80.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,286,'2026-07-28 00:28:07.038388','2026-07-28 00:29:45.486422',3),(245,2,27,'O','23','10ft pipe','10ft pipe',23,'Sukoon',13,13,NULL,'PARTY_OR_SITE',44.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,287,'2026-07-28 00:28:07.041534','2026-07-28 00:29:45.493867',3),(246,2,27,'P','23','10ft pipe','10ft pipe',23,'ANV',14,14,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,288,'2026-07-28 00:28:07.044794','2026-07-28 00:29:45.503177',3),(247,2,27,'Q','23','10ft pipe','10ft pipe',23,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,289,'2026-07-28 00:28:07.048398','2026-07-28 00:29:45.511378',3),(248,2,27,'U','23','10ft pipe','10ft pipe',23,'Godown',NULL,NULL,'MAIN','GODOWN',850.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,290,'2026-07-28 00:28:07.052538','2026-07-28 00:29:45.518163',2),(249,2,28,'I','24','8ft pipe','8ft pipe',24,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',6.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,291,'2026-07-28 00:28:07.057043','2026-07-28 00:29:45.938731',3),(250,2,29,'U','25','U jack','U jack',25,'Godown',NULL,NULL,'MAIN','GODOWN',700.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,292,'2026-07-28 00:28:07.637125','2026-07-28 00:29:45.945036',2),(251,2,30,'C','26','Base jack','Base jack',26,'4m Façade',1,1,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,293,'2026-07-28 00:28:07.641711','2026-07-28 00:29:45.953501',3),(252,2,30,'D','26','Base jack','Base jack',26,'Ali Designer',2,2,NULL,'PARTY_OR_SITE',126.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,294,'2026-07-28 00:28:07.644968','2026-07-28 00:29:45.962127',3),(253,2,30,'G','26','Base jack','Base jack',26,'Imperial',5,5,NULL,'PARTY_OR_SITE',58.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,295,'2026-07-28 00:28:07.648340','2026-07-28 00:29:45.971728',3),(254,2,30,'H','26','Base jack','Base jack',26,'Noble',6,6,NULL,'PARTY_OR_SITE',70.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,296,'2026-07-28 00:28:07.652137','2026-07-28 00:29:45.981243',3),(255,2,30,'I','26','Base jack','Base jack',26,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',41.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,297,'2026-07-28 00:28:07.656161','2026-07-28 00:29:45.989406',3),(256,2,30,'J','26','Base jack','Base jack',26,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',32.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,298,'2026-07-28 00:28:07.659711','2026-07-28 00:29:45.998484',3),(257,2,30,'K','26','Base jack','Base jack',26,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',133.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,299,'2026-07-28 00:28:07.663492','2026-07-28 00:29:46.007138',3),(258,2,30,'L','26','Base jack','Base jack',26,'Epilson',10,10,NULL,'PARTY_OR_SITE',96.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,300,'2026-07-28 00:28:07.667336','2026-07-28 00:29:46.015978',3),(259,2,30,'M','26','Base jack','Base jack',26,'SBUT',11,11,NULL,'PARTY_OR_SITE',74.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,301,'2026-07-28 00:28:07.671685','2026-07-28 00:29:46.023965',3),(260,2,30,'N','26','Base jack','Base jack',26,'SK Interior',12,12,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,302,'2026-07-28 00:28:07.675365','2026-07-28 00:29:46.032583',3),(261,2,30,'O','26','Base jack','Base jack',26,'Sukoon',13,13,NULL,'PARTY_OR_SITE',38.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,303,'2026-07-28 00:28:07.678773','2026-07-28 00:29:46.040469',3),(262,2,30,'P','26','Base jack','Base jack',26,'ANV',14,14,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,304,'2026-07-28 00:28:07.682297','2026-07-28 00:29:46.049715',3),(263,2,30,'Q','26','Base jack','Base jack',26,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,305,'2026-07-28 00:28:07.686353','2026-07-28 00:29:46.163577',3),(264,2,30,'U','26','Base jack','Base jack',26,'Godown',NULL,NULL,'MAIN','GODOWN',1150.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,306,'2026-07-28 00:28:07.689905','2026-07-28 00:29:46.170393',2),(265,2,31,'C','27','Coupler','Coupler',27,'4m Façade',1,1,NULL,'PARTY_OR_SITE',8.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,307,'2026-07-28 00:28:07.694007','2026-07-28 00:29:46.180061',3),(266,2,31,'G','27','Coupler','Coupler',27,'Imperial',5,5,NULL,'PARTY_OR_SITE',60.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,308,'2026-07-28 00:28:07.697390','2026-07-28 00:29:46.191680',3),(267,2,31,'H','27','Coupler','Coupler',27,'Noble',6,6,NULL,'PARTY_OR_SITE',229.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,309,'2026-07-28 00:28:07.701016','2026-07-28 00:29:46.201199',3),(268,2,31,'I','27','Coupler','Coupler',27,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',117.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,310,'2026-07-28 00:28:07.705343','2026-07-28 00:29:46.209605',3),(269,2,31,'J','27','Coupler','Coupler',27,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',90.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,311,'2026-07-28 00:28:07.708633','2026-07-28 00:29:46.218932',3),(270,2,31,'M','27','Coupler','Coupler',27,'SBUT',11,11,NULL,'PARTY_OR_SITE',200.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,312,'2026-07-28 00:28:07.711883','2026-07-28 00:29:46.227574',3),(271,2,31,'N','27','Coupler','Coupler',27,'SK Interior',12,12,NULL,'PARTY_OR_SITE',180.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,313,'2026-07-28 00:28:07.715233','2026-07-28 00:29:46.236614',3),(272,2,31,'O','27','Coupler','Coupler',27,'Sukoon',13,13,NULL,'PARTY_OR_SITE',114.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,314,'2026-07-28 00:28:07.719135','2026-07-28 00:29:46.245409',3),(273,2,31,'P','27','Coupler','Coupler',27,'ANV',14,14,NULL,'PARTY_OR_SITE',150.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,315,'2026-07-28 00:28:07.722934','2026-07-28 00:29:46.255315',3),(274,2,31,'Q','27','Coupler','Coupler',27,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',34.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,316,'2026-07-28 00:28:07.726373','2026-07-28 00:29:46.264898',3),(275,2,32,'U','28','Spiggot pin','Spiggot pin',28,'Godown',NULL,NULL,'MAIN','GODOWN',1400.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,317,'2026-07-28 00:28:07.730542','2026-07-28 00:29:46.271239',2),(276,2,33,'F','29','Joint pins','Joint pins',29,'Engarc',4,4,NULL,'PARTY_OR_SITE',1000.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,318,'2026-07-28 00:28:07.735618','2026-07-28 00:29:46.278958',3),(277,2,33,'H','29','Joint pins','Joint pins',29,'Noble',6,6,NULL,'PARTY_OR_SITE',10.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,319,'2026-07-28 00:28:07.739161','2026-07-28 00:29:46.287791',3),(278,2,33,'I','29','Joint pins','Joint pins',29,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',146.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,320,'2026-07-28 00:28:07.742786','2026-07-28 00:29:46.296381',3),(279,2,33,'K','29','Joint pins','Joint pins',29,'Raymond Tenex',9,9,NULL,'PARTY_OR_SITE',141.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,321,'2026-07-28 00:28:07.746565','2026-07-28 00:29:46.306796',3),(280,2,33,'N','29','Joint pins','Joint pins',29,'SK Interior',12,12,NULL,'PARTY_OR_SITE',175.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,322,'2026-07-28 00:28:07.761363','2026-07-28 00:29:46.316254',3),(281,2,33,'P','29','Joint pins','Joint pins',29,'ANV',14,14,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,323,'2026-07-28 00:28:07.765699','2026-07-28 00:29:46.327003',3),(282,2,33,'Q','29','Joint pins','Joint pins',29,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',22.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,324,'2026-07-28 00:28:07.770010','2026-07-28 00:29:46.335253',3),(283,2,33,'R','29','Joint pins','Joint pins',29,'Innovator Façade',16,16,NULL,'PARTY_OR_SITE',80.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,325,'2026-07-28 00:28:07.773670','2026-07-28 00:29:46.342843',3),(284,2,33,'U','29','Joint pins','Joint pins',29,'Godown',NULL,NULL,'MAIN','GODOWN',800.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,326,'2026-07-28 00:28:07.777210','2026-07-28 00:29:46.349610',2),(285,2,34,'I','30','Props','Props',30,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',2.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,327,'2026-07-28 00:28:07.781770','2026-07-28 00:29:47.106862',3),(286,2,34,'O','30','Props','Props',30,'Sukoon',13,13,NULL,'PARTY_OR_SITE',4.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,328,'2026-07-28 00:28:07.785683','2026-07-28 00:29:47.343551',3),(287,2,34,'U','30','Props','Props',30,'Godown',NULL,NULL,'MAIN','GODOWN',260.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,329,'2026-07-28 00:28:07.801375','2026-07-28 00:29:47.350302',2),(288,2,35,'H','31','Castor wheel','Castor wheel',31,'Noble',6,6,NULL,'PARTY_OR_SITE',4.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,330,'2026-07-28 00:28:07.807971','2026-07-28 00:29:47.358233',3),(289,2,36,'J','32','7ft pipe','7ft pipe',32,'Raymond GS',8,8,NULL,'PARTY_OR_SITE',6.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,331,'2026-07-28 00:28:07.813990','2026-07-28 00:29:47.366965',3),(290,2,36,'P','32','7ft pipe','7ft pipe',32,'ANV',14,14,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,332,'2026-07-28 00:28:07.819043','2026-07-28 00:29:47.374911',3),(291,2,36,'U','32','7ft pipe','7ft pipe',32,'Godown',NULL,NULL,'MAIN','GODOWN',130.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,333,'2026-07-28 00:28:07.824253','2026-07-28 00:29:47.381903',2),(292,2,37,'I','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'Zeeco Media',7,7,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,334,'2026-07-28 00:28:07.826261','2026-07-28 00:29:47.389886',4),(293,2,37,'M','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'SBUT',11,11,NULL,'PARTY_OR_SITE',195.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,335,'2026-07-28 00:28:07.827710','2026-07-28 00:29:47.398359',4),(294,2,37,'Q','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'Rocks & Logs',15,15,NULL,'PARTY_OR_SITE',6.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,336,'2026-07-28 00:28:07.829421','2026-07-28 00:29:47.407513',4),(295,2,37,'U','33','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',33,'Godown',NULL,NULL,'MAIN','GODOWN',200.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,337,'2026-07-28 00:28:07.830923','2026-07-28 00:29:47.415113',3),(296,2,38,'G','34','10ft chaneel','10ft chaneel',34,'Imperial',5,5,NULL,'PARTY_OR_SITE',2.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,338,'2026-07-28 00:28:07.835666','2026-07-28 00:29:47.424081',3),(297,2,39,'G','35','8ft & 10ft  plate pipe','8ft & 10ft plate pipe',35,'Imperial',5,5,NULL,'PARTY_OR_SITE',20.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED','Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically. Decision explicitly confirmed.',1,0,NULL,339,'2026-07-28 00:28:07.837347','2026-07-28 00:29:47.520015',4),(298,2,40,'N','36','10f ladder','10f ladder',36,'SK Interior',12,12,NULL,'PARTY_OR_SITE',25.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,340,'2026-07-28 00:28:07.841624','2026-07-28 00:29:47.528415',3),(299,2,41,'U','37','Ladder pipe','Ladder pipe',37,'Godown',NULL,NULL,'MAIN','GODOWN',30.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,341,'2026-07-28 00:28:07.845741','2026-07-28 00:29:47.534766',2),(300,2,42,'U','38','Ladder steps','Ladder steps',38,'Godown',NULL,NULL,'MAIN','GODOWN',112.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,342,'2026-07-28 00:28:07.850004','2026-07-28 00:29:47.539996',2),(301,2,43,'U','39','20ft Aluminium Ladder','20ft Aluminium Ladder',39,'Godown',NULL,NULL,'MAIN','GODOWN',5.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,343,'2026-07-28 00:28:07.854525','2026-07-28 00:29:47.545522',2),(302,2,44,'U','40','7.5 ft Iron Ladder','7.5 ft Iron Ladder',40,'Godown',NULL,NULL,'MAIN','GODOWN',23.0000,'2026-07-16','AVAILABLE','OPENING_GODOWN_BALANCE','REVERSED',NULL,0,0,NULL,344,'2026-07-28 00:28:07.858838','2026-07-28 00:29:47.551461',2),(303,2,45,'N','41','Ladder coupler','Ladder coupler',41,'SK Interior',12,12,NULL,'PARTY_OR_SITE',100.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,345,'2026-07-28 00:28:07.862877','2026-07-28 00:29:47.559104',3),(304,2,46,'N','42','Toe board','Toe board',42,'SK Interior',12,12,NULL,'PARTY_OR_SITE',50.0000,'2026-07-25','ISSUED','OPENING_SITE_BALANCE','REVERSED',NULL,0,0,NULL,346,'2026-07-28 00:28:07.867156','2026-07-28 00:29:47.568824',3);
/*!40000 ALTER TABLE `stock_import_rows` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_transactions`
--

DROP TABLE IF EXISTS `stock_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint NOT NULL,
  `transaction_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_date` date NOT NULL,
  `quantity` decimal(19,4) NOT NULL,
  `weight` decimal(19,4) DEFAULT NULL,
  `direction` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stock_bucket` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint NOT NULL,
  `import_batch_id` bigint DEFAULT NULL,
  `import_row_id` bigint DEFAULT NULL,
  `site_id` bigint DEFAULT NULL,
  `party_id` bigint DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `reversal_of_transaction_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_stock_transactions_reversal` (`reversal_of_transaction_id`),
  KEY `idx_stock_transactions_item_date` (`item_id`,`transaction_date`),
  KEY `idx_stock_transactions_type` (`transaction_type`),
  KEY `idx_stock_transactions_source` (`source_type`,`source_id`),
  KEY `fk_stock_transactions_import_row` (`import_row_id`),
  KEY `idx_stock_transactions_import` (`import_batch_id`,`import_row_id`),
  KEY `idx_stock_transactions_site_date` (`site_id`,`transaction_date`),
  KEY `idx_stock_transactions_party_date` (`party_id`,`transaction_date`),
  CONSTRAINT `fk_stock_transactions_import_batch` FOREIGN KEY (`import_batch_id`) REFERENCES `stock_import_batches` (`id`),
  CONSTRAINT `fk_stock_transactions_import_row` FOREIGN KEY (`import_row_id`) REFERENCES `stock_import_rows` (`id`),
  CONSTRAINT `fk_stock_transactions_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_stock_transactions_party` FOREIGN KEY (`party_id`) REFERENCES `parties` (`id`),
  CONSTRAINT `fk_stock_transactions_reversal` FOREIGN KEY (`reversal_of_transaction_id`) REFERENCES `stock_transactions` (`id`),
  CONSTRAINT `fk_stock_transactions_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `ck_stock_transactions_direction` CHECK ((`direction` in (_utf8mb4'IN',_utf8mb4'OUT'))),
  CONSTRAINT `ck_stock_transactions_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=499 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_transactions`
--

LOCK TABLES `stock_transactions` WRITE;
/*!40000 ALTER TABLE `stock_transactions` DISABLE KEYS */;
INSERT INTO `stock_transactions` VALUES (1,1,'OPENING_SITE_BALANCE','2026-07-25',33.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',1,1,1,1,1,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell C5','admin','2026-07-28 00:08:31.952985',NULL),(2,1,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',2,1,2,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D5','admin','2026-07-28 00:08:31.960407',NULL),(3,1,'OPENING_SITE_BALANCE','2026-07-25',319.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',3,1,3,3,3,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell E5','admin','2026-07-28 00:08:31.967811',NULL),(4,1,'OPENING_SITE_BALANCE','2026-07-25',151.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',4,1,4,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G5','admin','2026-07-28 00:08:31.974195',NULL),(5,1,'OPENING_SITE_BALANCE','2026-07-25',190.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',5,1,5,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H5','admin','2026-07-28 00:08:31.981881',NULL),(6,1,'OPENING_SITE_BALANCE','2026-07-25',287.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',6,1,6,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J5','admin','2026-07-28 00:08:31.988872',NULL),(7,1,'OPENING_SITE_BALANCE','2026-07-25',1031.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',7,1,7,11,11,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell M5','admin','2026-07-28 00:08:31.997738',NULL),(8,1,'OPENING_SITE_BALANCE','2026-07-25',130.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',8,1,8,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O5','admin','2026-07-28 00:08:32.005806',NULL),(9,1,'OPENING_GODOWN_BALANCE','2026-07-16',1457.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',9,1,9,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U5','admin','2026-07-28 00:08:32.008298',NULL),(10,2,'OPENING_GODOWN_BALANCE','2026-07-16',100.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',10,1,10,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U6','admin','2026-07-28 00:08:32.017158',NULL),(11,3,'OPENING_GODOWN_BALANCE','2026-07-16',250.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',11,1,11,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U7','admin','2026-07-28 00:08:32.023035',NULL),(12,4,'OPENING_SITE_BALANCE','2026-07-25',66.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',12,1,12,1,1,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell C8','admin','2026-07-28 00:08:32.033632',NULL),(13,4,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',13,1,13,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D8','admin','2026-07-28 00:08:32.041716',NULL),(14,4,'OPENING_SITE_BALANCE','2026-07-25',310.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',14,1,14,3,3,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell E8','admin','2026-07-28 00:08:32.049815',NULL),(15,4,'OPENING_SITE_BALANCE','2026-07-25',246.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',15,1,15,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G8','admin','2026-07-28 00:08:32.057355',NULL),(16,4,'OPENING_SITE_BALANCE','2026-07-25',462.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',16,1,16,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H8','admin','2026-07-28 00:08:32.065241',NULL),(17,4,'OPENING_SITE_BALANCE','2026-07-25',431.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',17,1,17,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J8','admin','2026-07-28 00:08:32.072605',NULL),(18,4,'OPENING_SITE_BALANCE','2026-07-25',1722.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',18,1,18,11,11,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell M8','admin','2026-07-28 00:08:32.080512',NULL),(19,4,'OPENING_SITE_BALANCE','2026-07-25',167.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',19,1,19,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O8','admin','2026-07-28 00:08:32.088156',NULL),(20,4,'OPENING_GODOWN_BALANCE','2026-07-16',2630.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',20,1,20,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U8','admin','2026-07-28 00:08:32.090781',NULL),(21,5,'OPENING_GODOWN_BALANCE','2026-07-16',205.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',21,1,21,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U9','admin','2026-07-28 00:08:32.100342',NULL),(22,6,'OPENING_GODOWN_BALANCE','2026-07-16',100.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',22,1,22,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U10','admin','2026-07-28 00:08:32.106690',NULL),(23,7,'OPENING_SITE_BALANCE','2026-07-25',27.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',23,1,23,1,1,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell C11','admin','2026-07-28 00:08:32.118217',NULL),(24,7,'OPENING_SITE_BALANCE','2026-07-25',102.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',24,1,24,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D11','admin','2026-07-28 00:08:32.125261',NULL),(25,7,'OPENING_SITE_BALANCE','2026-07-25',15.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',25,1,25,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G11','admin','2026-07-28 00:08:32.133111',NULL),(26,7,'OPENING_SITE_BALANCE','2026-07-25',85.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',26,1,26,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H11','admin','2026-07-28 00:08:32.140067',NULL),(27,7,'OPENING_SITE_BALANCE','2026-07-25',31.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',27,1,27,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I11','admin','2026-07-28 00:08:32.152251',NULL),(28,7,'OPENING_SITE_BALANCE','2026-07-25',252.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',28,1,28,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J11','admin','2026-07-28 00:08:32.159215',NULL),(29,7,'OPENING_SITE_BALANCE','2026-07-25',255.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',29,1,29,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K11','admin','2026-07-28 00:08:32.166906',NULL),(30,7,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',30,1,30,11,11,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell M11','admin','2026-07-28 00:08:32.173407',NULL),(31,7,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',31,1,31,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N11','admin','2026-07-28 00:08:32.180442',NULL),(32,7,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',32,1,32,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O11','admin','2026-07-28 00:08:32.257593',NULL),(33,7,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',33,1,33,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P11','admin','2026-07-28 00:08:32.265523',NULL),(34,7,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',34,1,34,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q11','admin','2026-07-28 00:08:32.272831',NULL),(35,7,'OPENING_GODOWN_BALANCE','2026-07-16',900.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',35,1,35,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U11','admin','2026-07-28 00:08:32.285723',NULL),(36,8,'OPENING_SITE_BALANCE','2026-07-25',267.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',36,1,36,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D12','admin','2026-07-28 00:08:32.296300',NULL),(37,8,'OPENING_SITE_BALANCE','2026-07-25',190.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',37,1,37,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F12','admin','2026-07-28 00:08:32.303615',NULL),(38,8,'OPENING_SITE_BALANCE','2026-07-25',140.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',38,1,38,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I12','admin','2026-07-28 00:08:32.310447',NULL),(39,8,'OPENING_SITE_BALANCE','2026-07-25',230.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',39,1,39,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K12','admin','2026-07-28 00:08:32.318127',NULL),(40,8,'OPENING_SITE_BALANCE','2026-07-25',120.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',40,1,40,10,10,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell L12','admin','2026-07-28 00:08:32.325186',NULL),(41,8,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',41,1,41,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q12','admin','2026-07-28 00:08:32.332884',NULL),(42,8,'OPENING_SITE_BALANCE','2026-07-25',160.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',42,1,42,16,16,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell R12','admin','2026-07-28 00:08:32.339826',NULL),(43,8,'OPENING_GODOWN_BALANCE','2026-07-16',1020.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',43,1,43,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U12','admin','2026-07-28 00:08:32.342377',NULL),(44,9,'OPENING_SITE_BALANCE','2026-07-25',120.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',44,1,44,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F13','admin','2026-07-28 00:08:32.353503',NULL),(45,9,'OPENING_SITE_BALANCE','2026-07-25',155.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',45,1,45,10,10,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell L13','admin','2026-07-28 00:08:32.360249',NULL),(46,9,'OPENING_GODOWN_BALANCE','2026-07-16',120.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',46,1,46,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U13','admin','2026-07-28 00:08:32.363087',NULL),(47,10,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',47,1,47,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D14','admin','2026-07-28 00:08:32.374320',NULL),(48,10,'OPENING_SITE_BALANCE','2026-07-25',40.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',48,1,48,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F14','admin','2026-07-28 00:08:32.382295',NULL),(49,10,'OPENING_SITE_BALANCE','2026-07-25',150.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',49,1,49,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K14','admin','2026-07-28 00:08:32.389666',NULL),(50,10,'OPENING_SITE_BALANCE','2026-07-25',210.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',50,1,50,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N14','admin','2026-07-28 00:08:32.397164',NULL),(51,10,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',51,1,51,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P14','admin','2026-07-28 00:08:32.404556',NULL),(52,10,'OPENING_GODOWN_BALANCE','2026-07-16',1110.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',52,1,52,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U14','admin','2026-07-28 00:08:32.407005',NULL),(53,11,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',53,1,53,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D15','admin','2026-07-28 00:08:32.418480',NULL),(54,11,'OPENING_SITE_BALANCE','2026-07-25',180.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',54,1,54,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F15','admin','2026-07-28 00:08:32.426663',NULL),(55,11,'OPENING_GODOWN_BALANCE','2026-07-16',150.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',55,1,55,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U15','admin','2026-07-28 00:08:32.429481',NULL),(56,12,'OPENING_SITE_BALANCE','2026-07-25',30.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',56,1,56,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D16','admin','2026-07-28 00:08:32.440480',NULL),(57,12,'OPENING_SITE_BALANCE','2026-07-25',600.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',57,1,57,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F16','admin','2026-07-28 00:08:32.448020',NULL),(58,12,'OPENING_GODOWN_BALANCE','2026-07-16',150.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',58,1,58,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U16','admin','2026-07-28 00:08:32.450861',NULL),(59,13,'OPENING_SITE_BALANCE','2026-07-25',385.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',59,1,59,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I17','admin','2026-07-28 00:08:32.461857',NULL),(60,13,'OPENING_SITE_BALANCE','2026-07-25',580.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',60,1,60,16,16,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell R17','admin','2026-07-28 00:08:32.469377',NULL),(61,13,'OPENING_GODOWN_BALANCE','2026-07-16',1270.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',61,1,61,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U17','admin','2026-07-28 00:08:32.471824',NULL),(62,14,'OPENING_SITE_BALANCE','2026-07-25',300.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',62,1,62,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D18','admin','2026-07-28 00:08:32.482910',NULL),(63,14,'OPENING_GODOWN_BALANCE','2026-07-16',420.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',63,1,63,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U18','admin','2026-07-28 00:08:32.485400',NULL),(64,15,'OPENING_SITE_BALANCE','2026-07-25',300.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',64,1,64,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I19','admin','2026-07-28 00:08:32.496193',NULL),(65,15,'OPENING_SITE_BALANCE','2026-07-25',240.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',65,1,65,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N19','admin','2026-07-28 00:08:32.503824',NULL),(66,15,'OPENING_SITE_BALANCE','2026-07-25',120.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',66,1,66,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q19','admin','2026-07-28 00:08:32.510791',NULL),(67,15,'OPENING_GODOWN_BALANCE','2026-07-16',2534.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',67,1,67,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U19','admin','2026-07-28 00:08:32.513722',NULL),(68,16,'OPENING_GODOWN_BALANCE','2026-07-16',300.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',68,1,68,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U20','admin','2026-07-28 00:08:32.521246',NULL),(69,17,'OPENING_SITE_BALANCE','2026-07-25',615.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',69,1,69,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K21','admin','2026-07-28 00:08:32.532104',NULL),(70,17,'OPENING_SITE_BALANCE','2026-07-25',220.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',70,1,70,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P21','admin','2026-07-28 00:08:32.539262',NULL),(71,17,'OPENING_GODOWN_BALANCE','2026-07-16',1553.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',71,1,71,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U21','admin','2026-07-28 00:08:32.541850',NULL),(72,18,'OPENING_SITE_BALANCE','2026-07-25',736.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',72,1,72,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F22','admin','2026-07-28 00:08:32.553436',NULL),(73,18,'OPENING_GODOWN_BALANCE','2026-07-16',2450.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',73,1,73,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U22','admin','2026-07-28 00:08:32.555959',NULL),(74,19,'OPENING_SITE_BALANCE','2026-07-25',545.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',74,1,74,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F23','admin','2026-07-28 00:08:32.567398',NULL),(75,19,'OPENING_SITE_BALANCE','2026-07-25',47.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',75,1,75,10,10,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell L23','admin','2026-07-28 00:08:32.576059',NULL),(76,19,'OPENING_SITE_BALANCE','2026-07-25',140.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',76,1,76,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N23','admin','2026-07-28 00:08:32.586722',NULL),(77,19,'OPENING_SITE_BALANCE','2026-07-25',72.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',77,1,77,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q23','admin','2026-07-28 00:08:32.596868',NULL),(78,19,'OPENING_SITE_BALANCE','2026-07-25',580.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',78,1,78,16,16,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell R23','admin','2026-07-28 00:08:32.607930',NULL),(79,20,'OPENING_SITE_BALANCE','2026-07-25',500.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',79,1,79,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D24','admin','2026-07-28 00:08:32.622354',NULL),(80,21,'OPENING_SITE_BALANCE','2026-07-25',500.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',80,1,80,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D25','admin','2026-07-28 00:08:32.633260',NULL),(81,21,'OPENING_SITE_BALANCE','2026-07-25',293.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',81,1,81,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F25','admin','2026-07-28 00:08:32.640777',NULL),(82,21,'OPENING_SITE_BALANCE','2026-07-25',734.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',82,1,82,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K25','admin','2026-07-28 00:08:32.649194',NULL),(83,21,'OPENING_GODOWN_BALANCE','2026-07-16',2595.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',83,1,83,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U25','admin','2026-07-28 00:08:32.652235',NULL),(84,22,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',84,1,84,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H26','admin','2026-07-28 00:08:32.664167',NULL),(85,22,'OPENING_SITE_BALANCE','2026-07-25',12.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',85,1,85,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O26','admin','2026-07-28 00:08:32.671875',NULL),(86,22,'OPENING_GODOWN_BALANCE','2026-07-16',408.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',86,1,86,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U26','admin','2026-07-28 00:08:32.674496',NULL),(87,23,'OPENING_SITE_BALANCE','2026-07-25',4.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',87,1,87,1,1,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell C27','admin','2026-07-28 00:08:32.686665',NULL),(88,23,'OPENING_SITE_BALANCE','2026-07-25',18.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',88,1,88,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G27','admin','2026-07-28 00:08:32.694030',NULL),(89,23,'OPENING_SITE_BALANCE','2026-07-25',39.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',89,1,89,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H27','admin','2026-07-28 00:08:32.702997',NULL),(90,23,'OPENING_SITE_BALANCE','2026-07-25',28.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',90,1,90,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I27','admin','2026-07-28 00:08:32.710418',NULL),(91,23,'OPENING_SITE_BALANCE','2026-07-25',39.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',91,1,91,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J27','admin','2026-07-28 00:08:32.718880',NULL),(92,23,'OPENING_SITE_BALANCE','2026-07-25',80.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',92,1,92,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N27','admin','2026-07-28 00:08:32.726713',NULL),(93,23,'OPENING_SITE_BALANCE','2026-07-25',44.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',93,1,93,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O27','admin','2026-07-28 00:08:32.737582',NULL),(94,23,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',94,1,94,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P27','admin','2026-07-28 00:08:32.745242',NULL),(95,23,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',95,1,95,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q27','admin','2026-07-28 00:08:32.755169',NULL),(96,23,'OPENING_GODOWN_BALANCE','2026-07-16',850.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',96,1,96,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U27','admin','2026-07-28 00:08:32.757863',NULL),(97,24,'OPENING_SITE_BALANCE','2026-07-25',6.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',97,1,97,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I28','admin','2026-07-28 00:08:32.771405',NULL),(98,25,'OPENING_GODOWN_BALANCE','2026-07-16',700.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',98,1,98,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U29','admin','2026-07-28 00:08:32.776654',NULL),(99,26,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',99,1,99,1,1,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell C30','admin','2026-07-28 00:08:32.789707',NULL),(100,26,'OPENING_SITE_BALANCE','2026-07-25',126.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',100,1,100,2,2,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell D30','admin','2026-07-28 00:08:32.798541',NULL),(101,26,'OPENING_SITE_BALANCE','2026-07-25',58.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',101,1,101,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G30','admin','2026-07-28 00:08:32.808095',NULL),(102,26,'OPENING_SITE_BALANCE','2026-07-25',70.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',102,1,102,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H30','admin','2026-07-28 00:08:32.817198',NULL),(103,26,'OPENING_SITE_BALANCE','2026-07-25',41.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',103,1,103,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I30','admin','2026-07-28 00:08:32.826126',NULL),(104,26,'OPENING_SITE_BALANCE','2026-07-25',32.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',104,1,104,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J30','admin','2026-07-28 00:08:32.835284',NULL),(105,26,'OPENING_SITE_BALANCE','2026-07-25',133.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',105,1,105,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K30','admin','2026-07-28 00:08:32.851091',NULL),(106,26,'OPENING_SITE_BALANCE','2026-07-25',96.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',106,1,106,10,10,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell L30','admin','2026-07-28 00:08:32.859340',NULL),(107,26,'OPENING_SITE_BALANCE','2026-07-25',74.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',107,1,107,11,11,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell M30','admin','2026-07-28 00:08:32.868414',NULL),(108,26,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',108,1,108,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N30','admin','2026-07-28 00:08:32.876147',NULL),(109,26,'OPENING_SITE_BALANCE','2026-07-25',38.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',109,1,109,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O30','admin','2026-07-28 00:08:32.886180',NULL),(110,26,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',110,1,110,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P30','admin','2026-07-28 00:08:32.894295',NULL),(111,26,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',111,1,111,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q30','admin','2026-07-28 00:08:32.902621',NULL),(112,26,'OPENING_GODOWN_BALANCE','2026-07-16',1150.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',112,1,112,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U30','admin','2026-07-28 00:08:32.905947',NULL),(113,27,'OPENING_SITE_BALANCE','2026-07-25',8.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',113,1,113,1,1,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell C31','admin','2026-07-28 00:08:32.918693',NULL),(114,27,'OPENING_SITE_BALANCE','2026-07-25',60.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',114,1,114,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G31','admin','2026-07-28 00:08:32.926654',NULL),(115,27,'OPENING_SITE_BALANCE','2026-07-25',229.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',115,1,115,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H31','admin','2026-07-28 00:08:32.935822',NULL),(116,27,'OPENING_SITE_BALANCE','2026-07-25',117.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',116,1,116,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I31','admin','2026-07-28 00:08:32.943659',NULL),(117,27,'OPENING_SITE_BALANCE','2026-07-25',90.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',117,1,117,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J31','admin','2026-07-28 00:08:32.952432',NULL),(118,27,'OPENING_SITE_BALANCE','2026-07-25',200.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',118,1,118,11,11,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell M31','admin','2026-07-28 00:08:32.960304',NULL),(119,27,'OPENING_SITE_BALANCE','2026-07-25',180.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',119,1,119,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N31','admin','2026-07-28 00:08:32.969053',NULL),(120,27,'OPENING_SITE_BALANCE','2026-07-25',114.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',120,1,120,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O31','admin','2026-07-28 00:08:32.977004',NULL),(121,27,'OPENING_SITE_BALANCE','2026-07-25',150.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',121,1,121,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P31','admin','2026-07-28 00:08:32.986030',NULL),(122,27,'OPENING_SITE_BALANCE','2026-07-25',34.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',122,1,122,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q31','admin','2026-07-28 00:08:32.993747',NULL),(123,28,'OPENING_GODOWN_BALANCE','2026-07-16',1400.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',123,1,123,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U32','admin','2026-07-28 00:08:33.000151',NULL),(124,29,'OPENING_SITE_BALANCE','2026-07-25',1000.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',124,1,124,4,4,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell F33','admin','2026-07-28 00:08:33.012689',NULL),(125,29,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',125,1,125,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H33','admin','2026-07-28 00:08:33.022112',NULL),(126,29,'OPENING_SITE_BALANCE','2026-07-25',146.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',126,1,126,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I33','admin','2026-07-28 00:08:33.030765',NULL),(127,29,'OPENING_SITE_BALANCE','2026-07-25',141.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',127,1,127,9,9,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell K33','admin','2026-07-28 00:08:33.039415',NULL),(128,29,'OPENING_SITE_BALANCE','2026-07-25',175.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',128,1,128,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N33','admin','2026-07-28 00:08:33.048982',NULL),(129,29,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',129,1,129,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P33','admin','2026-07-28 00:08:33.057615',NULL),(130,29,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',130,1,130,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q33','admin','2026-07-28 00:08:33.066402',NULL),(131,29,'OPENING_SITE_BALANCE','2026-07-25',80.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',131,1,131,16,16,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell R33','admin','2026-07-28 00:08:33.074791',NULL),(132,29,'OPENING_GODOWN_BALANCE','2026-07-16',800.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',132,1,132,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U33','admin','2026-07-28 00:08:33.077783',NULL),(133,30,'OPENING_SITE_BALANCE','2026-07-25',2.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',133,1,133,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I34','admin','2026-07-28 00:08:33.090748',NULL),(134,30,'OPENING_SITE_BALANCE','2026-07-25',4.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',134,1,134,13,13,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell O34','admin','2026-07-28 00:08:33.100896',NULL),(135,30,'OPENING_GODOWN_BALANCE','2026-07-16',260.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',135,1,135,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U34','admin','2026-07-28 00:08:33.103971',NULL),(136,31,'OPENING_SITE_BALANCE','2026-07-25',4.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',136,1,136,6,6,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell H35','admin','2026-07-28 00:08:33.116905',NULL),(137,32,'OPENING_SITE_BALANCE','2026-07-25',6.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',137,1,137,8,8,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell J36','admin','2026-07-28 00:08:33.129858',NULL),(138,32,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',138,1,138,14,14,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell P36','admin','2026-07-28 00:08:33.139713',NULL),(139,32,'OPENING_GODOWN_BALANCE','2026-07-16',130.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',139,1,139,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U36','admin','2026-07-28 00:08:33.143216',NULL),(140,33,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',140,1,140,7,7,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell I37','admin','2026-07-28 00:08:33.158942',NULL),(141,33,'OPENING_SITE_BALANCE','2026-07-25',195.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',141,1,141,11,11,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell M37','admin','2026-07-28 00:08:33.169724',NULL),(142,33,'OPENING_SITE_BALANCE','2026-07-25',6.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',142,1,142,15,15,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell Q37','admin','2026-07-28 00:08:33.179626',NULL),(143,33,'OPENING_GODOWN_BALANCE','2026-07-16',200.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',143,1,143,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U37','admin','2026-07-28 00:08:33.183494',NULL),(144,34,'OPENING_SITE_BALANCE','2026-07-25',2.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',144,1,144,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G38','admin','2026-07-28 00:08:33.201514',NULL),(145,35,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',145,1,145,5,5,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell G39','admin','2026-07-28 00:08:33.220511',NULL),(146,36,'OPENING_SITE_BALANCE','2026-07-25',25.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',146,1,146,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N40','admin','2026-07-28 00:08:33.238715',NULL),(147,37,'OPENING_GODOWN_BALANCE','2026-07-16',30.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',147,1,147,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U41','admin','2026-07-28 00:08:33.248968',NULL),(148,38,'OPENING_GODOWN_BALANCE','2026-07-16',112.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',148,1,148,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U42','admin','2026-07-28 00:08:33.260884',NULL),(149,39,'OPENING_GODOWN_BALANCE','2026-07-16',5.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',149,1,149,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U43','admin','2026-07-28 00:08:33.273422',NULL),(150,40,'OPENING_GODOWN_BALANCE','2026-07-16',23.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',150,1,150,NULL,NULL,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell U44','admin','2026-07-28 00:08:33.286140',NULL),(151,41,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',151,1,151,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N45','admin','2026-07-28 00:08:33.306576',NULL),(152,42,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',152,1,152,12,12,'Opening stock from OSI-F1F4AFA5-59E6-43EF-B9EF-33AE1D464F3A cell N46','admin','2026-07-28 00:08:33.320915',NULL),(153,1,'ISSUE','2026-08-01',60.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',1,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:11:54.211420',NULL),(154,1,'ISSUE','2026-08-01',60.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:11:54.213296',NULL),(155,4,'ISSUE','2026-08-01',120.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',1,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:11:54.220273',NULL),(156,4,'ISSUE','2026-08-01',120.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:11:54.221094',NULL),(157,1,'ISSUE','2026-08-02',40.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',2,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:11:54.402497',NULL),(158,1,'ISSUE','2026-08-02',40.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',2,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:11:54.403336',NULL),(159,4,'ISSUE','2026-08-02',80.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',2,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:11:54.408918',NULL),(160,4,'ISSUE','2026-08-02',80.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',2,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:11:54.409698',NULL),(161,1,'RECEIVE','2026-08-10',80.0000,0.0000,'IN','AVAILABLE','RECEIVING_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:13:30.221421',NULL),(162,1,'RECEIVE','2026-08-10',80.0000,0.0000,'OUT','PENDING_SITE','RECEIVING_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:13:30.223692',NULL),(163,1,'RECEIVE','2026-08-10',10.0000,0.0000,'IN','DAMAGED','RECEIVING_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:13:30.225388',NULL),(164,1,'RECEIVE','2026-08-10',10.0000,0.0000,'OUT','PENDING_SITE','RECEIVING_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:13:30.227095',NULL),(165,1,'RECEIVE','2026-08-10',5.0000,0.0000,'IN','LOST','RECEIVING_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:13:30.228793',NULL),(166,1,'RECEIVE','2026-08-10',5.0000,0.0000,'OUT','PENDING_SITE','RECEIVING_CHALLAN',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:13:30.230378',NULL),(167,1,'REPAIR','2026-07-28',10.0000,0.0000,'OUT','DAMAGED','STOCK_DAMAGE',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:03.956705',NULL),(168,1,'REPAIR','2026-07-28',10.0000,0.0000,'IN','AVAILABLE','STOCK_DAMAGE',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:03.959001',NULL),(169,4,'DAMAGE','2026-08-11',2.0000,0.0000,'OUT','PENDING_SITE','STOCK_DAMAGE',2,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:34.849910',NULL),(170,4,'DAMAGE','2026-08-11',2.0000,0.0000,'IN','DAMAGED','STOCK_DAMAGE',2,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:34.851726',NULL),(171,4,'SCRAP','2026-07-28',2.0000,0.0000,'OUT','DAMAGED','STOCK_DAMAGE',2,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:34.882167',NULL),(172,4,'SCRAP','2026-07-28',2.0000,0.0000,'IN','SCRAPPED','STOCK_DAMAGE',2,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:34.883835',NULL),(173,1,'SITE_TRANSFER_OUT','2026-08-12',3.0000,0.0000,'OUT','PENDING_SITE','SITE_TRANSFER',1,NULL,NULL,17,17,NULL,'admin','2026-07-28 00:15:59.829383',NULL),(174,1,'SITE_TRANSFER_IN','2026-08-12',3.0000,0.0000,'IN','PENDING_SITE','SITE_TRANSFER',1,NULL,NULL,18,17,NULL,'admin','2026-07-28 00:15:59.839180',NULL),(175,1,'ISSUE','2026-07-01',60.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',3,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:17:54.415801',NULL),(176,1,'ISSUE','2026-07-01',60.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',3,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.416982',NULL),(177,4,'ISSUE','2026-07-01',120.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',3,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:17:54.426791',NULL),(178,4,'ISSUE','2026-07-01',120.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',3,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.427979',NULL),(179,1,'ISSUE','2026-07-02',40.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',4,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:17:54.483021',NULL),(180,1,'ISSUE','2026-07-02',40.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',4,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.484171',NULL),(181,4,'ISSUE','2026-07-02',80.0000,0.0000,'OUT','AVAILABLE','ISSUED_CHALLAN',4,NULL,NULL,NULL,NULL,NULL,'admin','2026-07-28 00:17:54.490694',NULL),(182,4,'ISSUE','2026-07-02',80.0000,0.0000,'IN','PENDING_SITE','ISSUED_CHALLAN',4,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.492067',NULL),(183,1,'RECEIVE','2026-07-10',80.0000,0.0000,'IN','AVAILABLE','RECEIVING_CHALLAN',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.589970',NULL),(184,1,'RECEIVE','2026-07-10',80.0000,0.0000,'OUT','PENDING_SITE','RECEIVING_CHALLAN',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.591248',NULL),(185,1,'RECEIVE','2026-07-10',10.0000,0.0000,'IN','DAMAGED','RECEIVING_CHALLAN',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.592312',NULL),(186,1,'RECEIVE','2026-07-10',10.0000,0.0000,'OUT','PENDING_SITE','RECEIVING_CHALLAN',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.593446',NULL),(187,1,'RECEIVE','2026-07-10',5.0000,0.0000,'IN','LOST','RECEIVING_CHALLAN',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:54.594520',NULL),(188,1,'RECEIVE','2026-07-10',5.0000,0.0000,'OUT','PENDING_SITE','RECEIVING_CHALLAN',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:55.794810',NULL),(189,1,'SITE_TRANSFER_OUT','2026-07-12',3.0000,0.0000,'OUT','PENDING_SITE','SITE_TRANSFER',2,NULL,NULL,19,17,NULL,'admin','2026-07-28 00:17:56.285425',NULL),(190,1,'SITE_TRANSFER_IN','2026-07-12',3.0000,0.0000,'IN','PENDING_SITE','SITE_TRANSFER',2,NULL,NULL,20,17,NULL,'admin','2026-07-28 00:17:56.286520',NULL),(191,1,'LOSS','2026-07-15',1.0000,0.0000,'OUT','PENDING_SITE','STOCK_LOSS',3,NULL,NULL,20,17,NULL,'admin','2026-07-28 00:20:20.296750',NULL),(192,1,'LOSS','2026-07-15',1.0000,0.0000,'IN','LOST','STOCK_LOSS',3,NULL,NULL,20,17,NULL,'admin','2026-07-28 00:20:20.299173',NULL),(193,1,'DAMAGE','2026-07-16',1.0000,0.0000,'OUT','PENDING_SITE','STOCK_DAMAGE',4,NULL,NULL,20,17,NULL,'admin','2026-07-28 00:20:20.429119',NULL),(194,1,'DAMAGE','2026-07-16',1.0000,0.0000,'IN','DAMAGED','STOCK_DAMAGE',4,NULL,NULL,20,17,NULL,'admin','2026-07-28 00:20:20.431048',NULL),(195,1,'OPENING_SITE_BALANCE','2026-07-25',33.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',153,2,153,1,1,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell C5','admin','2026-07-28 00:29:39.424281',NULL),(196,1,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',154,2,154,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D5','admin','2026-07-28 00:29:39.435168',NULL),(197,1,'OPENING_SITE_BALANCE','2026-07-25',319.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',155,2,155,3,3,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell E5','admin','2026-07-28 00:29:39.441779',NULL),(198,1,'OPENING_SITE_BALANCE','2026-07-25',151.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',156,2,156,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G5','admin','2026-07-28 00:29:39.448767',NULL),(199,1,'OPENING_SITE_BALANCE','2026-07-25',190.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',157,2,157,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H5','admin','2026-07-28 00:29:39.455108',NULL),(200,1,'OPENING_SITE_BALANCE','2026-07-25',287.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',158,2,158,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J5','admin','2026-07-28 00:29:39.462317',NULL),(201,1,'OPENING_SITE_BALANCE','2026-07-25',1031.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',159,2,159,11,11,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell M5','admin','2026-07-28 00:29:39.469088',NULL),(202,1,'OPENING_SITE_BALANCE','2026-07-25',130.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',160,2,160,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O5','admin','2026-07-28 00:29:39.475614',NULL),(203,1,'OPENING_GODOWN_BALANCE','2026-07-16',1457.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',161,2,161,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U5','admin','2026-07-28 00:29:39.482356',NULL),(204,2,'OPENING_GODOWN_BALANCE','2026-07-16',100.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',162,2,162,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U6','admin','2026-07-28 00:29:39.487595',NULL),(205,3,'OPENING_GODOWN_BALANCE','2026-07-16',250.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',163,2,163,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U7','admin','2026-07-28 00:29:39.492963',NULL),(206,4,'OPENING_SITE_BALANCE','2026-07-25',66.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',164,2,164,1,1,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell C8','admin','2026-07-28 00:29:39.501095',NULL),(207,4,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',165,2,165,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D8','admin','2026-07-28 00:29:39.507475',NULL),(208,4,'OPENING_SITE_BALANCE','2026-07-25',310.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',166,2,166,3,3,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell E8','admin','2026-07-28 00:29:39.514737',NULL),(209,4,'OPENING_SITE_BALANCE','2026-07-25',246.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',167,2,167,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G8','admin','2026-07-28 00:29:39.521919',NULL),(210,4,'OPENING_SITE_BALANCE','2026-07-25',462.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',168,2,168,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H8','admin','2026-07-28 00:29:39.528888',NULL),(211,4,'OPENING_SITE_BALANCE','2026-07-25',431.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',169,2,169,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J8','admin','2026-07-28 00:29:39.536269',NULL),(212,4,'OPENING_SITE_BALANCE','2026-07-25',1722.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',170,2,170,11,11,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell M8','admin','2026-07-28 00:29:39.543598',NULL),(213,4,'OPENING_SITE_BALANCE','2026-07-25',167.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',171,2,171,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O8','admin','2026-07-28 00:29:39.550675',NULL),(214,4,'OPENING_GODOWN_BALANCE','2026-07-16',2630.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',172,2,172,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U8','admin','2026-07-28 00:29:39.555122',NULL),(215,5,'OPENING_GODOWN_BALANCE','2026-07-16',205.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',173,2,173,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U9','admin','2026-07-28 00:29:39.560001',NULL),(216,6,'OPENING_GODOWN_BALANCE','2026-07-16',100.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',174,2,174,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U10','admin','2026-07-28 00:29:39.565778',NULL),(217,7,'OPENING_SITE_BALANCE','2026-07-25',27.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',175,2,175,1,1,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell C11','admin','2026-07-28 00:29:39.573141',NULL),(218,7,'OPENING_SITE_BALANCE','2026-07-25',102.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',176,2,176,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D11','admin','2026-07-28 00:29:39.580281',NULL),(219,7,'OPENING_SITE_BALANCE','2026-07-25',15.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',177,2,177,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G11','admin','2026-07-28 00:29:39.587041',NULL),(220,7,'OPENING_SITE_BALANCE','2026-07-25',85.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',178,2,178,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H11','admin','2026-07-28 00:29:39.593586',NULL),(221,7,'OPENING_SITE_BALANCE','2026-07-25',31.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',179,2,179,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I11','admin','2026-07-28 00:29:39.601495',NULL),(222,7,'OPENING_SITE_BALANCE','2026-07-25',252.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',180,2,180,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J11','admin','2026-07-28 00:29:39.608234',NULL),(223,7,'OPENING_SITE_BALANCE','2026-07-25',255.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',181,2,181,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K11','admin','2026-07-28 00:29:39.616740',NULL),(224,7,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',182,2,182,11,11,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell M11','admin','2026-07-28 00:29:39.625352',NULL),(225,7,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',183,2,183,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N11','admin','2026-07-28 00:29:39.637997',NULL),(226,7,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',184,2,184,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O11','admin','2026-07-28 00:29:39.648908',NULL),(227,7,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',185,2,185,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P11','admin','2026-07-28 00:29:39.659981',NULL),(228,7,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',186,2,186,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q11','admin','2026-07-28 00:29:39.668401',NULL),(229,7,'OPENING_GODOWN_BALANCE','2026-07-16',900.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',187,2,187,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U11','admin','2026-07-28 00:29:39.675192',NULL),(230,8,'OPENING_SITE_BALANCE','2026-07-25',267.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',188,2,188,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D12','admin','2026-07-28 00:29:39.683790',NULL),(231,8,'OPENING_SITE_BALANCE','2026-07-25',190.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',189,2,189,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F12','admin','2026-07-28 00:29:39.691515',NULL),(232,8,'OPENING_SITE_BALANCE','2026-07-25',140.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',190,2,190,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I12','admin','2026-07-28 00:29:39.699505',NULL),(233,8,'OPENING_SITE_BALANCE','2026-07-25',230.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',191,2,191,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K12','admin','2026-07-28 00:29:39.910825',NULL),(234,8,'OPENING_SITE_BALANCE','2026-07-25',120.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',192,2,192,10,10,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell L12','admin','2026-07-28 00:29:40.596650',NULL),(235,8,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',193,2,193,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q12','admin','2026-07-28 00:29:40.603093',NULL),(236,8,'OPENING_SITE_BALANCE','2026-07-25',160.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',194,2,194,16,16,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell R12','admin','2026-07-28 00:29:40.608997',NULL),(237,8,'OPENING_GODOWN_BALANCE','2026-07-16',1020.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',195,2,195,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U12','admin','2026-07-28 00:29:40.613773',NULL),(238,9,'OPENING_SITE_BALANCE','2026-07-25',120.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',196,2,196,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F13','admin','2026-07-28 00:29:40.621515',NULL),(239,9,'OPENING_SITE_BALANCE','2026-07-25',155.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',197,2,197,10,10,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell L13','admin','2026-07-28 00:29:40.629572',NULL),(240,9,'OPENING_GODOWN_BALANCE','2026-07-16',120.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',198,2,198,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U13','admin','2026-07-28 00:29:40.634702',NULL),(241,10,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',199,2,199,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D14','admin','2026-07-28 00:29:40.640919',NULL),(242,10,'OPENING_SITE_BALANCE','2026-07-25',40.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',200,2,200,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F14','admin','2026-07-28 00:29:40.648324',NULL),(243,10,'OPENING_SITE_BALANCE','2026-07-25',150.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',201,2,201,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K14','admin','2026-07-28 00:29:40.654802',NULL),(244,10,'OPENING_SITE_BALANCE','2026-07-25',210.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',202,2,202,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N14','admin','2026-07-28 00:29:40.660780',NULL),(245,10,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',203,2,203,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P14','admin','2026-07-28 00:29:40.668303',NULL),(246,10,'OPENING_GODOWN_BALANCE','2026-07-16',1110.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',204,2,204,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U14','admin','2026-07-28 00:29:40.673167',NULL),(247,11,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',205,2,205,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D15','admin','2026-07-28 00:29:40.680362',NULL),(248,11,'OPENING_SITE_BALANCE','2026-07-25',180.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',206,2,206,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F15','admin','2026-07-28 00:29:41.637826',NULL),(249,11,'OPENING_GODOWN_BALANCE','2026-07-16',150.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',207,2,207,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U15','admin','2026-07-28 00:29:41.642712',NULL),(250,12,'OPENING_SITE_BALANCE','2026-07-25',30.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',208,2,208,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D16','admin','2026-07-28 00:29:41.650692',NULL),(251,12,'OPENING_SITE_BALANCE','2026-07-25',600.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',209,2,209,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F16','admin','2026-07-28 00:29:41.657965',NULL),(252,12,'OPENING_GODOWN_BALANCE','2026-07-16',150.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',210,2,210,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U16','admin','2026-07-28 00:29:41.663461',NULL),(253,13,'OPENING_SITE_BALANCE','2026-07-25',385.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',211,2,211,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I17','admin','2026-07-28 00:29:41.672044',NULL),(254,13,'OPENING_SITE_BALANCE','2026-07-25',580.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',212,2,212,16,16,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell R17','admin','2026-07-28 00:29:41.681109',NULL),(255,13,'OPENING_GODOWN_BALANCE','2026-07-16',1270.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',213,2,213,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U17','admin','2026-07-28 00:29:41.687369',NULL),(256,14,'OPENING_SITE_BALANCE','2026-07-25',300.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',214,2,214,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D18','admin','2026-07-28 00:29:41.696889',NULL),(257,14,'OPENING_GODOWN_BALANCE','2026-07-16',420.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',215,2,215,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U18','admin','2026-07-28 00:29:41.703254',NULL),(258,15,'OPENING_SITE_BALANCE','2026-07-25',300.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',216,2,216,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I19','admin','2026-07-28 00:29:41.710722',NULL),(259,15,'OPENING_SITE_BALANCE','2026-07-25',240.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',217,2,217,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N19','admin','2026-07-28 00:29:41.718284',NULL),(260,15,'OPENING_SITE_BALANCE','2026-07-25',120.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',218,2,218,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q19','admin','2026-07-28 00:29:41.803774',NULL),(261,15,'OPENING_GODOWN_BALANCE','2026-07-16',2534.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',219,2,219,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U19','admin','2026-07-28 00:29:41.808696',NULL),(262,16,'OPENING_GODOWN_BALANCE','2026-07-16',300.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',220,2,220,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U20','admin','2026-07-28 00:29:41.814004',NULL),(263,17,'OPENING_SITE_BALANCE','2026-07-25',615.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',221,2,221,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K21','admin','2026-07-28 00:29:41.821780',NULL),(264,17,'OPENING_SITE_BALANCE','2026-07-25',220.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',222,2,222,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P21','admin','2026-07-28 00:29:41.829183',NULL),(265,17,'OPENING_GODOWN_BALANCE','2026-07-16',1553.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',223,2,223,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U21','admin','2026-07-28 00:29:41.834640',NULL),(266,18,'OPENING_SITE_BALANCE','2026-07-25',736.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',224,2,224,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F22','admin','2026-07-28 00:29:41.848987',NULL),(267,18,'OPENING_GODOWN_BALANCE','2026-07-16',2450.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',225,2,225,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U22','admin','2026-07-28 00:29:41.857371',NULL),(268,19,'OPENING_SITE_BALANCE','2026-07-25',545.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',226,2,226,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F23','admin','2026-07-28 00:29:41.870236',NULL),(269,19,'OPENING_SITE_BALANCE','2026-07-25',47.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',227,2,227,10,10,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell L23','admin','2026-07-28 00:29:41.880837',NULL),(270,19,'OPENING_SITE_BALANCE','2026-07-25',140.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',228,2,228,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N23','admin','2026-07-28 00:29:41.900533',NULL),(271,19,'OPENING_SITE_BALANCE','2026-07-25',72.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',229,2,229,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q23','admin','2026-07-28 00:29:41.920206',NULL),(272,19,'OPENING_SITE_BALANCE','2026-07-25',580.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',230,2,230,16,16,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell R23','admin','2026-07-28 00:29:41.934567',NULL),(273,20,'OPENING_SITE_BALANCE','2026-07-25',500.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',231,2,231,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D24','admin','2026-07-28 00:29:41.950694',NULL),(274,21,'OPENING_SITE_BALANCE','2026-07-25',500.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',232,2,232,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D25','admin','2026-07-28 00:29:41.968305',NULL),(275,21,'OPENING_SITE_BALANCE','2026-07-25',293.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',233,2,233,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F25','admin','2026-07-28 00:29:41.981245',NULL),(276,21,'OPENING_SITE_BALANCE','2026-07-25',734.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',234,2,234,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K25','admin','2026-07-28 00:29:41.992134',NULL),(277,21,'OPENING_GODOWN_BALANCE','2026-07-16',2595.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',235,2,235,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U25','admin','2026-07-28 00:29:42.000772',NULL),(278,22,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',236,2,236,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H26','admin','2026-07-28 00:29:42.012914',NULL),(279,22,'OPENING_SITE_BALANCE','2026-07-25',12.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',237,2,237,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O26','admin','2026-07-28 00:29:42.021353',NULL),(280,22,'OPENING_GODOWN_BALANCE','2026-07-16',408.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',238,2,238,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U26','admin','2026-07-28 00:29:42.026599',NULL),(281,23,'OPENING_SITE_BALANCE','2026-07-25',4.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',239,2,239,1,1,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell C27','admin','2026-07-28 00:29:42.035427',NULL),(282,23,'OPENING_SITE_BALANCE','2026-07-25',18.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',240,2,240,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G27','admin','2026-07-28 00:29:42.043001',NULL),(283,23,'OPENING_SITE_BALANCE','2026-07-25',39.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',241,2,241,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H27','admin','2026-07-28 00:29:42.051753',NULL),(284,23,'OPENING_SITE_BALANCE','2026-07-25',28.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',242,2,242,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I27','admin','2026-07-28 00:29:42.059932',NULL),(285,23,'OPENING_SITE_BALANCE','2026-07-25',39.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',243,2,243,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J27','admin','2026-07-28 00:29:42.067733',NULL),(286,23,'OPENING_SITE_BALANCE','2026-07-25',80.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',244,2,244,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N27','admin','2026-07-28 00:29:42.075009',NULL),(287,23,'OPENING_SITE_BALANCE','2026-07-25',44.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',245,2,245,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O27','admin','2026-07-28 00:29:42.082321',NULL),(288,23,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',246,2,246,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P27','admin','2026-07-28 00:29:42.089924',NULL),(289,23,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',247,2,247,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q27','admin','2026-07-28 00:29:42.097773',NULL),(290,23,'OPENING_GODOWN_BALANCE','2026-07-16',850.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',248,2,248,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U27','admin','2026-07-28 00:29:42.103126',NULL),(291,24,'OPENING_SITE_BALANCE','2026-07-25',6.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',249,2,249,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I28','admin','2026-07-28 00:29:42.206265',NULL),(292,25,'OPENING_GODOWN_BALANCE','2026-07-16',700.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',250,2,250,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U29','admin','2026-07-28 00:29:42.211621',NULL),(293,26,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',251,2,251,1,1,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell C30','admin','2026-07-28 00:29:42.219806',NULL),(294,26,'OPENING_SITE_BALANCE','2026-07-25',126.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',252,2,252,2,2,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell D30','admin','2026-07-28 00:29:42.226405',NULL),(295,26,'OPENING_SITE_BALANCE','2026-07-25',58.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',253,2,253,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G30','admin','2026-07-28 00:29:42.234578',NULL),(296,26,'OPENING_SITE_BALANCE','2026-07-25',70.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',254,2,254,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H30','admin','2026-07-28 00:29:42.241561',NULL),(297,26,'OPENING_SITE_BALANCE','2026-07-25',41.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',255,2,255,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I30','admin','2026-07-28 00:29:42.249178',NULL),(298,26,'OPENING_SITE_BALANCE','2026-07-25',32.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',256,2,256,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J30','admin','2026-07-28 00:29:42.256255',NULL),(299,26,'OPENING_SITE_BALANCE','2026-07-25',133.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',257,2,257,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K30','admin','2026-07-28 00:29:42.263252',NULL),(300,26,'OPENING_SITE_BALANCE','2026-07-25',96.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',258,2,258,10,10,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell L30','admin','2026-07-28 00:29:42.270947',NULL),(301,26,'OPENING_SITE_BALANCE','2026-07-25',74.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',259,2,259,11,11,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell M30','admin','2026-07-28 00:29:42.278082',NULL),(302,26,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',260,2,260,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N30','admin','2026-07-28 00:29:42.286413',NULL),(303,26,'OPENING_SITE_BALANCE','2026-07-25',38.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',261,2,261,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O30','admin','2026-07-28 00:29:42.293236',NULL),(304,26,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',262,2,262,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P30','admin','2026-07-28 00:29:42.301216',NULL),(305,26,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',263,2,263,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q30','admin','2026-07-28 00:29:42.308067',NULL),(306,26,'OPENING_GODOWN_BALANCE','2026-07-16',1150.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',264,2,264,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U30','admin','2026-07-28 00:29:42.314087',NULL),(307,27,'OPENING_SITE_BALANCE','2026-07-25',8.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',265,2,265,1,1,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell C31','admin','2026-07-28 00:29:42.322851',NULL),(308,27,'OPENING_SITE_BALANCE','2026-07-25',60.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',266,2,266,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G31','admin','2026-07-28 00:29:42.330816',NULL),(309,27,'OPENING_SITE_BALANCE','2026-07-25',229.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',267,2,267,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H31','admin','2026-07-28 00:29:42.338217',NULL),(310,27,'OPENING_SITE_BALANCE','2026-07-25',117.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',268,2,268,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I31','admin','2026-07-28 00:29:42.345188',NULL),(311,27,'OPENING_SITE_BALANCE','2026-07-25',90.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',269,2,269,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J31','admin','2026-07-28 00:29:42.352920',NULL),(312,27,'OPENING_SITE_BALANCE','2026-07-25',200.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',270,2,270,11,11,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell M31','admin','2026-07-28 00:29:42.359718',NULL),(313,27,'OPENING_SITE_BALANCE','2026-07-25',180.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',271,2,271,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N31','admin','2026-07-28 00:29:42.367697',NULL),(314,27,'OPENING_SITE_BALANCE','2026-07-25',114.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',272,2,272,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O31','admin','2026-07-28 00:29:42.374595',NULL),(315,27,'OPENING_SITE_BALANCE','2026-07-25',150.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',273,2,273,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P31','admin','2026-07-28 00:29:42.382636',NULL),(316,27,'OPENING_SITE_BALANCE','2026-07-25',34.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',274,2,274,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q31','admin','2026-07-28 00:29:42.389589',NULL),(317,28,'OPENING_GODOWN_BALANCE','2026-07-16',1400.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',275,2,275,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U32','admin','2026-07-28 00:29:42.395105',NULL),(318,29,'OPENING_SITE_BALANCE','2026-07-25',1000.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',276,2,276,4,4,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell F33','admin','2026-07-28 00:29:42.404323',NULL),(319,29,'OPENING_SITE_BALANCE','2026-07-25',10.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',277,2,277,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H33','admin','2026-07-28 00:29:42.411749',NULL),(320,29,'OPENING_SITE_BALANCE','2026-07-25',146.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',278,2,278,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I33','admin','2026-07-28 00:29:42.419798',NULL),(321,29,'OPENING_SITE_BALANCE','2026-07-25',141.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',279,2,279,9,9,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell K33','admin','2026-07-28 00:29:42.427145',NULL),(322,29,'OPENING_SITE_BALANCE','2026-07-25',175.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',280,2,280,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N33','admin','2026-07-28 00:29:42.447097',NULL),(323,29,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',281,2,281,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P33','admin','2026-07-28 00:29:42.457598',NULL),(324,29,'OPENING_SITE_BALANCE','2026-07-25',22.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',282,2,282,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q33','admin','2026-07-28 00:29:42.469197',NULL),(325,29,'OPENING_SITE_BALANCE','2026-07-25',80.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',283,2,283,16,16,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell R33','admin','2026-07-28 00:29:42.477050',NULL),(326,29,'OPENING_GODOWN_BALANCE','2026-07-16',800.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',284,2,284,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U33','admin','2026-07-28 00:29:42.488170',NULL),(327,30,'OPENING_SITE_BALANCE','2026-07-25',2.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',285,2,285,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I34','admin','2026-07-28 00:29:42.498192',NULL),(328,30,'OPENING_SITE_BALANCE','2026-07-25',4.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',286,2,286,13,13,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell O34','admin','2026-07-28 00:29:42.513953',NULL),(329,30,'OPENING_GODOWN_BALANCE','2026-07-16',260.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',287,2,287,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U34','admin','2026-07-28 00:29:42.520012',NULL),(330,31,'OPENING_SITE_BALANCE','2026-07-25',4.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',288,2,288,6,6,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell H35','admin','2026-07-28 00:29:42.528611',NULL),(331,32,'OPENING_SITE_BALANCE','2026-07-25',6.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',289,2,289,8,8,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell J36','admin','2026-07-28 00:29:42.537555',NULL),(332,32,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',290,2,290,14,14,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell P36','admin','2026-07-28 00:29:42.545142',NULL),(333,32,'OPENING_GODOWN_BALANCE','2026-07-16',130.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',291,2,291,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U36','admin','2026-07-28 00:29:42.551078',NULL),(334,33,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',292,2,292,7,7,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell I37','admin','2026-07-28 00:29:42.558586',NULL),(335,33,'OPENING_SITE_BALANCE','2026-07-25',195.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',293,2,293,11,11,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell M37','admin','2026-07-28 00:29:42.566561',NULL),(336,33,'OPENING_SITE_BALANCE','2026-07-25',6.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',294,2,294,15,15,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell Q37','admin','2026-07-28 00:29:42.574139',NULL),(337,33,'OPENING_GODOWN_BALANCE','2026-07-16',200.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',295,2,295,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U37','admin','2026-07-28 00:29:42.579830',NULL),(338,34,'OPENING_SITE_BALANCE','2026-07-25',2.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',296,2,296,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G38','admin','2026-07-28 00:29:42.588363',NULL),(339,35,'OPENING_SITE_BALANCE','2026-07-25',20.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',297,2,297,5,5,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell G39','admin','2026-07-28 00:29:42.596949',NULL),(340,36,'OPENING_SITE_BALANCE','2026-07-25',25.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',298,2,298,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N40','admin','2026-07-28 00:29:42.605470',NULL),(341,37,'OPENING_GODOWN_BALANCE','2026-07-16',30.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',299,2,299,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U41','admin','2026-07-28 00:29:42.611110',NULL),(342,38,'OPENING_GODOWN_BALANCE','2026-07-16',112.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',300,2,300,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U42','admin','2026-07-28 00:29:42.616812',NULL),(343,39,'OPENING_GODOWN_BALANCE','2026-07-16',5.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',301,2,301,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U43','admin','2026-07-28 00:29:42.622061',NULL),(344,40,'OPENING_GODOWN_BALANCE','2026-07-16',23.0000,0.0000,'IN','AVAILABLE','STOCK_IMPORT',302,2,302,NULL,NULL,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell U44','admin','2026-07-28 00:29:42.627268',NULL),(345,41,'OPENING_SITE_BALANCE','2026-07-25',100.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',303,2,303,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N45','admin','2026-07-28 00:29:42.635618',NULL),(346,42,'OPENING_SITE_BALANCE','2026-07-25',50.0000,0.0000,'IN','ISSUED','STOCK_IMPORT',304,2,304,12,12,'Opening stock from OSI-4200A46E-DD04-460D-8BA3-500EDA699819 cell N46','admin','2026-07-28 00:29:42.643842',NULL),(347,1,'OPENING_SITE_REVERSAL','2026-07-28',33.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',153,2,153,1,1,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.789473',195),(348,1,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',154,2,154,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.799065',196),(349,1,'OPENING_SITE_REVERSAL','2026-07-28',319.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',155,2,155,3,3,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.808801',197),(350,1,'OPENING_SITE_REVERSAL','2026-07-28',151.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',156,2,156,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.818881',198),(351,1,'OPENING_SITE_REVERSAL','2026-07-28',190.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',157,2,157,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.828523',199),(352,1,'OPENING_SITE_REVERSAL','2026-07-28',287.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',158,2,158,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.838232',200),(353,1,'OPENING_SITE_REVERSAL','2026-07-28',1031.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',159,2,159,11,11,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.847956',201),(354,1,'OPENING_SITE_REVERSAL','2026-07-28',130.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',160,2,160,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.857479',202),(355,1,'OPENING_GODOWN_REVERSAL','2026-07-28',1457.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',161,2,161,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.864805',203),(356,2,'OPENING_GODOWN_REVERSAL','2026-07-28',100.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',162,2,162,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.871665',204),(357,3,'OPENING_GODOWN_REVERSAL','2026-07-28',250.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',163,2,163,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.878363',205),(358,4,'OPENING_SITE_REVERSAL','2026-07-28',66.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',164,2,164,1,1,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.886795',206),(359,4,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',165,2,165,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.894324',207),(360,4,'OPENING_SITE_REVERSAL','2026-07-28',310.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',166,2,166,3,3,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.901942',208),(361,4,'OPENING_SITE_REVERSAL','2026-07-28',246.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',167,2,167,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.908537',209),(362,4,'OPENING_SITE_REVERSAL','2026-07-28',462.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',168,2,168,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.916628',210),(363,4,'OPENING_SITE_REVERSAL','2026-07-28',431.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',169,2,169,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.924116',211),(364,4,'OPENING_SITE_REVERSAL','2026-07-28',1722.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',170,2,170,11,11,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.932614',212),(365,4,'OPENING_SITE_REVERSAL','2026-07-28',167.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',171,2,171,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.940403',213),(366,4,'OPENING_GODOWN_REVERSAL','2026-07-28',2630.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',172,2,172,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.946293',214),(367,5,'OPENING_GODOWN_REVERSAL','2026-07-28',205.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',173,2,173,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.951744',215),(368,6,'OPENING_GODOWN_REVERSAL','2026-07-28',100.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',174,2,174,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.956792',216),(369,7,'OPENING_SITE_REVERSAL','2026-07-28',27.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',175,2,175,1,1,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.965165',217),(370,7,'OPENING_SITE_REVERSAL','2026-07-28',102.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',176,2,176,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.971961',218),(371,7,'OPENING_SITE_REVERSAL','2026-07-28',15.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',177,2,177,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.978440',219),(372,7,'OPENING_SITE_REVERSAL','2026-07-28',85.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',178,2,178,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.985683',220),(373,7,'OPENING_SITE_REVERSAL','2026-07-28',31.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',179,2,179,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.991861',221),(374,7,'OPENING_SITE_REVERSAL','2026-07-28',252.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',180,2,180,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:42.999066',222),(375,7,'OPENING_SITE_REVERSAL','2026-07-28',255.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',181,2,181,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.006164',223),(376,7,'OPENING_SITE_REVERSAL','2026-07-28',10.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',182,2,182,11,11,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.013510',224),(377,7,'OPENING_SITE_REVERSAL','2026-07-28',100.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',183,2,183,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.021441',225),(378,7,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',184,2,184,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.029112',226),(379,7,'OPENING_SITE_REVERSAL','2026-07-28',10.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',185,2,185,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.036048',227),(380,7,'OPENING_SITE_REVERSAL','2026-07-28',10.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',186,2,186,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.042368',228),(381,7,'OPENING_GODOWN_REVERSAL','2026-07-28',900.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',187,2,187,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.047640',229),(382,8,'OPENING_SITE_REVERSAL','2026-07-28',267.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',188,2,188,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.054790',230),(383,8,'OPENING_SITE_REVERSAL','2026-07-28',190.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',189,2,189,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.062070',231),(384,8,'OPENING_SITE_REVERSAL','2026-07-28',140.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',190,2,190,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.069486',232),(385,8,'OPENING_SITE_REVERSAL','2026-07-28',230.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',191,2,191,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.075962',233),(386,8,'OPENING_SITE_REVERSAL','2026-07-28',120.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',192,2,192,10,10,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.084046',234),(387,8,'OPENING_SITE_REVERSAL','2026-07-28',22.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',193,2,193,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.090981',235),(388,8,'OPENING_SITE_REVERSAL','2026-07-28',160.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',194,2,194,16,16,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.098705',236),(389,8,'OPENING_GODOWN_REVERSAL','2026-07-28',1020.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',195,2,195,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.104020',237),(390,9,'OPENING_SITE_REVERSAL','2026-07-28',120.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',196,2,196,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.111001',238),(391,9,'OPENING_SITE_REVERSAL','2026-07-28',155.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',197,2,197,10,10,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.119146',239),(392,9,'OPENING_GODOWN_REVERSAL','2026-07-28',120.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',198,2,198,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.124234',240),(393,10,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',199,2,199,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:43.132186',241),(394,10,'OPENING_SITE_REVERSAL','2026-07-28',40.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',200,2,200,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.352773',242),(395,10,'OPENING_SITE_REVERSAL','2026-07-28',150.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',201,2,201,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.360176',243),(396,10,'OPENING_SITE_REVERSAL','2026-07-28',210.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',202,2,202,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.368072',244),(397,10,'OPENING_SITE_REVERSAL','2026-07-28',100.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',203,2,203,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.375633',245),(398,10,'OPENING_GODOWN_REVERSAL','2026-07-28',1110.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',204,2,204,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.381935',246),(399,11,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',205,2,205,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.390012',247),(400,11,'OPENING_SITE_REVERSAL','2026-07-28',180.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',206,2,206,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.399152',248),(401,11,'OPENING_GODOWN_REVERSAL','2026-07-28',150.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',207,2,207,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.405826',249),(402,12,'OPENING_SITE_REVERSAL','2026-07-28',30.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',208,2,208,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.414882',250),(403,12,'OPENING_SITE_REVERSAL','2026-07-28',600.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',209,2,209,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.422469',251),(404,12,'OPENING_GODOWN_REVERSAL','2026-07-28',150.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',210,2,210,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.427801',252),(405,13,'OPENING_SITE_REVERSAL','2026-07-28',385.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',211,2,211,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.436515',253),(406,13,'OPENING_SITE_REVERSAL','2026-07-28',580.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',212,2,212,16,16,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.443318',254),(407,13,'OPENING_GODOWN_REVERSAL','2026-07-28',1270.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',213,2,213,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.449666',255),(408,14,'OPENING_SITE_REVERSAL','2026-07-28',300.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',214,2,214,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.457235',256),(409,14,'OPENING_GODOWN_REVERSAL','2026-07-28',420.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',215,2,215,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.463369',257),(410,15,'OPENING_SITE_REVERSAL','2026-07-28',300.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',216,2,216,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.471296',258),(411,15,'OPENING_SITE_REVERSAL','2026-07-28',240.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',217,2,217,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.478548',259),(412,15,'OPENING_SITE_REVERSAL','2026-07-28',120.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',218,2,218,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.486602',260),(413,15,'OPENING_GODOWN_REVERSAL','2026-07-28',2534.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',219,2,219,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.491396',261),(414,16,'OPENING_GODOWN_REVERSAL','2026-07-28',300.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',220,2,220,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.496943',262),(415,17,'OPENING_SITE_REVERSAL','2026-07-28',615.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',221,2,221,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.504767',263),(416,17,'OPENING_SITE_REVERSAL','2026-07-28',220.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',222,2,222,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.511766',264),(417,17,'OPENING_GODOWN_REVERSAL','2026-07-28',1553.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',223,2,223,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.517563',265),(418,18,'OPENING_SITE_REVERSAL','2026-07-28',736.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',224,2,224,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.524813',266),(419,18,'OPENING_GODOWN_REVERSAL','2026-07-28',2450.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',225,2,225,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.530354',267),(420,19,'OPENING_SITE_REVERSAL','2026-07-28',545.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',226,2,226,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.538059',268),(421,19,'OPENING_SITE_REVERSAL','2026-07-28',47.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',227,2,227,10,10,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.546584',269),(422,19,'OPENING_SITE_REVERSAL','2026-07-28',140.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',228,2,228,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.555005',270),(423,19,'OPENING_SITE_REVERSAL','2026-07-28',72.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',229,2,229,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.563699',271),(424,19,'OPENING_SITE_REVERSAL','2026-07-28',580.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',230,2,230,16,16,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.571562',272),(425,20,'OPENING_SITE_REVERSAL','2026-07-28',500.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',231,2,231,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.580430',273),(426,21,'OPENING_SITE_REVERSAL','2026-07-28',500.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',232,2,232,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.588669',274),(427,21,'OPENING_SITE_REVERSAL','2026-07-28',293.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',233,2,233,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.596829',275),(428,21,'OPENING_SITE_REVERSAL','2026-07-28',734.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',234,2,234,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.604873',276),(429,21,'OPENING_GODOWN_REVERSAL','2026-07-28',2595.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',235,2,235,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.609783',277),(430,22,'OPENING_SITE_REVERSAL','2026-07-28',22.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',236,2,236,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.619025',278),(431,22,'OPENING_SITE_REVERSAL','2026-07-28',12.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',237,2,237,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.626992',279),(432,22,'OPENING_GODOWN_REVERSAL','2026-07-28',408.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',238,2,238,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.633697',280),(433,23,'OPENING_SITE_REVERSAL','2026-07-28',4.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',239,2,239,1,1,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.644298',281),(434,23,'OPENING_SITE_REVERSAL','2026-07-28',18.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',240,2,240,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.658019',282),(435,23,'OPENING_SITE_REVERSAL','2026-07-28',39.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',241,2,241,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.671623',283),(436,23,'OPENING_SITE_REVERSAL','2026-07-28',28.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',242,2,242,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.681881',284),(437,23,'OPENING_SITE_REVERSAL','2026-07-28',39.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',243,2,243,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.691509',285),(438,23,'OPENING_SITE_REVERSAL','2026-07-28',80.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',244,2,244,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:44.700997',286),(439,23,'OPENING_SITE_REVERSAL','2026-07-28',44.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',245,2,245,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.491247',287),(440,23,'OPENING_SITE_REVERSAL','2026-07-28',50.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',246,2,246,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.499976',288),(441,23,'OPENING_SITE_REVERSAL','2026-07-28',10.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',247,2,247,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.508361',289),(442,23,'OPENING_GODOWN_REVERSAL','2026-07-28',850.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',248,2,248,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.514573',290),(443,24,'OPENING_SITE_REVERSAL','2026-07-28',6.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',249,2,249,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.522925',291),(444,25,'OPENING_GODOWN_REVERSAL','2026-07-28',700.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',250,2,250,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.941492',292),(445,26,'OPENING_SITE_REVERSAL','2026-07-28',22.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',251,2,251,1,1,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.950584',293),(446,26,'OPENING_SITE_REVERSAL','2026-07-28',126.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',252,2,252,2,2,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.958778',294),(447,26,'OPENING_SITE_REVERSAL','2026-07-28',58.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',253,2,253,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.968455',295),(448,26,'OPENING_SITE_REVERSAL','2026-07-28',70.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',254,2,254,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.977579',296),(449,26,'OPENING_SITE_REVERSAL','2026-07-28',41.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',255,2,255,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.986599',297),(450,26,'OPENING_SITE_REVERSAL','2026-07-28',32.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',256,2,256,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:45.994748',298),(451,26,'OPENING_SITE_REVERSAL','2026-07-28',133.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',257,2,257,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.004184',299),(452,26,'OPENING_SITE_REVERSAL','2026-07-28',96.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',258,2,258,10,10,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.012702',300),(453,26,'OPENING_SITE_REVERSAL','2026-07-28',74.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',259,2,259,11,11,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.021339',301),(454,26,'OPENING_SITE_REVERSAL','2026-07-28',50.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',260,2,260,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.029008',302),(455,26,'OPENING_SITE_REVERSAL','2026-07-28',38.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',261,2,261,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.037717',303),(456,26,'OPENING_SITE_REVERSAL','2026-07-28',50.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',262,2,262,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.046444',304),(457,26,'OPENING_SITE_REVERSAL','2026-07-28',22.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',263,2,263,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.055010',305),(458,26,'OPENING_GODOWN_REVERSAL','2026-07-28',1150.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',264,2,264,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.166910',306),(459,27,'OPENING_SITE_REVERSAL','2026-07-28',8.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',265,2,265,1,1,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.175152',307),(460,27,'OPENING_SITE_REVERSAL','2026-07-28',60.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',266,2,266,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.188062',308),(461,27,'OPENING_SITE_REVERSAL','2026-07-28',229.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',267,2,267,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.197641',309),(462,27,'OPENING_SITE_REVERSAL','2026-07-28',117.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',268,2,268,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.206712',310),(463,27,'OPENING_SITE_REVERSAL','2026-07-28',90.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',269,2,269,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.215827',311),(464,27,'OPENING_SITE_REVERSAL','2026-07-28',200.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',270,2,270,11,11,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.224399',312),(465,27,'OPENING_SITE_REVERSAL','2026-07-28',180.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',271,2,271,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.233625',313),(466,27,'OPENING_SITE_REVERSAL','2026-07-28',114.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',272,2,272,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.242105',314),(467,27,'OPENING_SITE_REVERSAL','2026-07-28',150.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',273,2,273,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.251843',315),(468,27,'OPENING_SITE_REVERSAL','2026-07-28',34.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',274,2,274,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.260548',316),(469,28,'OPENING_GODOWN_REVERSAL','2026-07-28',1400.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',275,2,275,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.267845',317),(470,29,'OPENING_SITE_REVERSAL','2026-07-28',1000.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',276,2,276,4,4,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.275843',318),(471,29,'OPENING_SITE_REVERSAL','2026-07-28',10.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',277,2,277,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.284984',319),(472,29,'OPENING_SITE_REVERSAL','2026-07-28',146.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',278,2,278,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.292905',320),(473,29,'OPENING_SITE_REVERSAL','2026-07-28',141.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',279,2,279,9,9,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.302525',321),(474,29,'OPENING_SITE_REVERSAL','2026-07-28',175.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',280,2,280,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.312526',322),(475,29,'OPENING_SITE_REVERSAL','2026-07-28',100.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',281,2,281,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.324433',323),(476,29,'OPENING_SITE_REVERSAL','2026-07-28',22.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',282,2,282,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.332505',324),(477,29,'OPENING_SITE_REVERSAL','2026-07-28',80.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',283,2,283,16,16,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.340184',325),(478,29,'OPENING_GODOWN_REVERSAL','2026-07-28',800.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',284,2,284,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.346122',326),(479,30,'OPENING_SITE_REVERSAL','2026-07-28',2.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',285,2,285,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:46.354283',327),(480,30,'OPENING_SITE_REVERSAL','2026-07-28',4.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',286,2,286,13,13,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.112161',328),(481,30,'OPENING_GODOWN_REVERSAL','2026-07-28',260.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',287,2,287,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.346737',329),(482,31,'OPENING_SITE_REVERSAL','2026-07-28',4.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',288,2,288,6,6,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.355037',330),(483,32,'OPENING_SITE_REVERSAL','2026-07-28',6.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',289,2,289,8,8,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.364105',331),(484,32,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',290,2,290,14,14,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.372238',332),(485,32,'OPENING_GODOWN_REVERSAL','2026-07-28',130.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',291,2,291,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.378270',333),(486,33,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',292,2,292,7,7,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.387023',334),(487,33,'OPENING_SITE_REVERSAL','2026-07-28',195.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',293,2,293,11,11,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.395404',335),(488,33,'OPENING_SITE_REVERSAL','2026-07-28',6.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',294,2,294,15,15,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.404443',336),(489,33,'OPENING_GODOWN_REVERSAL','2026-07-28',200.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',295,2,295,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.410544',337),(490,34,'OPENING_SITE_REVERSAL','2026-07-28',2.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',296,2,296,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.420699',338),(491,35,'OPENING_SITE_REVERSAL','2026-07-28',20.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',297,2,297,5,5,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.429672',339),(492,36,'OPENING_SITE_REVERSAL','2026-07-28',25.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',298,2,298,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.525036',340),(493,37,'OPENING_GODOWN_REVERSAL','2026-07-28',30.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',299,2,299,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.531505',341),(494,38,'OPENING_GODOWN_REVERSAL','2026-07-28',112.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',300,2,300,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.536964',342),(495,39,'OPENING_GODOWN_REVERSAL','2026-07-28',5.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',301,2,301,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.542141',343),(496,40,'OPENING_GODOWN_REVERSAL','2026-07-28',23.0000,0.0000,'OUT','AVAILABLE','STOCK_IMPORT_REVERSAL',302,2,302,NULL,NULL,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.548111',344),(497,41,'OPENING_SITE_REVERSAL','2026-07-28',100.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',303,2,303,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.555942',345),(498,42,'OPENING_SITE_REVERSAL','2026-07-28',50.0000,0.0000,'OUT','ISSUED','STOCK_IMPORT_REVERSAL',304,2,304,12,12,'E2E prove compensating reversal and non-destructive audit trail','admin','2026-07-28 00:29:47.565176',346);
/*!40000 ALTER TABLE `stock_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tds_details`
--

DROP TABLE IF EXISTS `tds_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tds_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_receipt_id` bigint NOT NULL,
  `tds_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
  `deduction_date` date DEFAULT NULL,
  `section` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `certificate_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `certificate_date` date DEFAULT NULL,
  `certificate_attachment_id` bigint DEFAULT NULL,
  `verification_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rejection_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verified_at` timestamp(6) NULL DEFAULT NULL,
  `verified_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tds_details_payment` (`payment_receipt_id`),
  KEY `fk_tds_details_attachment` (`certificate_attachment_id`),
  KEY `idx_tds_details_status` (`verification_status`),
  CONSTRAINT `fk_tds_details_attachment` FOREIGN KEY (`certificate_attachment_id`) REFERENCES `file_attachments` (`id`),
  CONSTRAINT `fk_tds_details_payment` FOREIGN KEY (`payment_receipt_id`) REFERENCES `payment_receipts` (`id`),
  CONSTRAINT `ck_tds_details_amount` CHECK ((`tds_amount` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tds_details`
--

LOCK TABLES `tds_details` WRITE;
/*!40000 ALTER TABLE `tds_details` DISABLE KEYS */;
INSERT INTO `tds_details` VALUES (1,1,2000.00,'2026-07-28','194C','E2E-TDS-001','2026-07-28',NULL,'VERIFIED',NULL,'2026-07-28 00:19:23.350077','admin','2026-07-28 00:19:23.241533','admin','2026-07-28 00:19:23.355089','admin',2);
/*!40000 ALTER TABLE `tds_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_activity_logs`
--

DROP TABLE IF EXISTS `user_activity_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_activity_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username_snapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entity_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_logs_user_id` (`user_id`),
  KEY `idx_activity_logs_action` (`action`),
  KEY `idx_activity_logs_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=261 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_activity_logs`
--

LOCK TABLES `user_activity_logs` WRITE;
/*!40000 ALTER TABLE `user_activity_logs` DISABLE KEYS */;
INSERT INTO `user_activity_logs` VALUES (1,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:08:22'),(2,1,'admin','STOCK_IMPORT_UPLOADED','StockImportBatch','1','Stock of material as on 25-07-26.xlsx','POST','/api/v1/stock-imports/upload','127.0.0.1','2026-07-28 00:08:23'),(3,1,'admin','STOCK_IMPORT_PARSED','StockImportBatch','1','42 source rows, 152 balance rows','POST','/api/v1/stock-imports/upload','127.0.0.1','2026-07-28 00:08:23'),(4,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 5','PUT','/api/v1/stock-imports/1/rows/1/item-mapping','127.0.0.1','2026-07-28 00:08:24'),(5,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 6','PUT','/api/v1/stock-imports/1/rows/10/item-mapping','127.0.0.1','2026-07-28 00:08:24'),(6,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 7','PUT','/api/v1/stock-imports/1/rows/11/item-mapping','127.0.0.1','2026-07-28 00:08:24'),(7,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 8','PUT','/api/v1/stock-imports/1/rows/12/item-mapping','127.0.0.1','2026-07-28 00:08:24'),(8,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 9','PUT','/api/v1/stock-imports/1/rows/21/item-mapping','127.0.0.1','2026-07-28 00:08:24'),(9,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 10','PUT','/api/v1/stock-imports/1/rows/22/item-mapping','127.0.0.1','2026-07-28 00:08:25'),(10,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 11','PUT','/api/v1/stock-imports/1/rows/23/item-mapping','127.0.0.1','2026-07-28 00:08:25'),(11,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 12','PUT','/api/v1/stock-imports/1/rows/36/item-mapping','127.0.0.1','2026-07-28 00:08:25'),(12,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 13','PUT','/api/v1/stock-imports/1/rows/44/item-mapping','127.0.0.1','2026-07-28 00:08:25'),(13,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 14','PUT','/api/v1/stock-imports/1/rows/47/item-mapping','127.0.0.1','2026-07-28 00:08:25'),(14,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 15','PUT','/api/v1/stock-imports/1/rows/53/item-mapping','127.0.0.1','2026-07-28 00:08:25'),(15,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 16','PUT','/api/v1/stock-imports/1/rows/56/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(16,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 17','PUT','/api/v1/stock-imports/1/rows/59/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(17,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 18','PUT','/api/v1/stock-imports/1/rows/62/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(18,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 19','PUT','/api/v1/stock-imports/1/rows/64/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(19,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 20','PUT','/api/v1/stock-imports/1/rows/68/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(20,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 21','PUT','/api/v1/stock-imports/1/rows/69/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(21,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 22','PUT','/api/v1/stock-imports/1/rows/72/item-mapping','127.0.0.1','2026-07-28 00:08:26'),(22,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 23','PUT','/api/v1/stock-imports/1/rows/74/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(23,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 24','PUT','/api/v1/stock-imports/1/rows/79/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(24,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 25','PUT','/api/v1/stock-imports/1/rows/80/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(25,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 26','PUT','/api/v1/stock-imports/1/rows/84/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(26,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 27','PUT','/api/v1/stock-imports/1/rows/87/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(27,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 28','PUT','/api/v1/stock-imports/1/rows/97/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(28,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 29','PUT','/api/v1/stock-imports/1/rows/98/item-mapping','127.0.0.1','2026-07-28 00:08:27'),(29,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 30','PUT','/api/v1/stock-imports/1/rows/99/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(30,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 31','PUT','/api/v1/stock-imports/1/rows/113/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(31,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 32','PUT','/api/v1/stock-imports/1/rows/123/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(32,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 33','PUT','/api/v1/stock-imports/1/rows/124/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(33,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 34','PUT','/api/v1/stock-imports/1/rows/133/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(34,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 35','PUT','/api/v1/stock-imports/1/rows/136/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(35,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 36','PUT','/api/v1/stock-imports/1/rows/137/item-mapping','127.0.0.1','2026-07-28 00:08:28'),(36,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 37','PUT','/api/v1/stock-imports/1/rows/140/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(37,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 38','PUT','/api/v1/stock-imports/1/rows/144/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(38,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 39','PUT','/api/v1/stock-imports/1/rows/145/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(39,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 40','PUT','/api/v1/stock-imports/1/rows/146/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(40,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 41','PUT','/api/v1/stock-imports/1/rows/147/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(41,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 42','PUT','/api/v1/stock-imports/1/rows/148/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(42,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 43','PUT','/api/v1/stock-imports/1/rows/149/item-mapping','127.0.0.1','2026-07-28 00:08:29'),(43,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 44','PUT','/api/v1/stock-imports/1/rows/150/item-mapping','127.0.0.1','2026-07-28 00:08:30'),(44,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 45','PUT','/api/v1/stock-imports/1/rows/151/item-mapping','127.0.0.1','2026-07-28 00:08:30'),(45,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Item mapping for source Excel row 46','PUT','/api/v1/stock-imports/1/rows/152/item-mapping','127.0.0.1','2026-07-28 00:08:30'),(46,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for 4m Façade','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:30'),(47,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Ali Designer','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:30'),(48,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Avighnaa Kandivali','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:30'),(49,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Engarc','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:30'),(50,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Imperial','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:30'),(51,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Noble','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(52,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Zeeco Media','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(53,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Raymond GS','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(54,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Raymond Tenex','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(55,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Epilson','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(56,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for SBUT','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(57,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for SK Interior','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(58,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Sukoon','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:31'),(59,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for ANV','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:32'),(60,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Rocks & Logs','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:32'),(61,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','1','Location mapping for Innovator Façade','PUT','/api/v1/stock-imports/1/location-mappings','127.0.0.1','2026-07-28 00:08:32'),(62,1,'admin','STOCK_IMPORT_VALIDATED','StockImportBatch','1','Corrected combined total 46019.0000','POST','/api/v1/stock-imports/1/validate','127.0.0.1','2026-07-28 00:08:32'),(63,1,'admin','STOCK_IMPORT_POSTED','StockImportBatch','1','Posted 152 opening rows','POST','/api/v1/stock-imports/1/post','127.0.0.1','2026-07-28 00:08:33'),(64,1,'admin','QUOTATION_TEMPLATE_CREATED','QuotationTemplate','1','E2E-QT-20260728','POST','/api/v1/quotation-templates','127.0.0.1','2026-07-28 00:09:50'),(65,1,'admin','QUOTATION_CREATED','Quotation','1','Draft created','POST','/api/v1/quotations','127.0.0.1','2026-07-28 00:10:15'),(66,1,'admin','QUOTATION_SENT','Quotation','1','DRAFT to SENT','POST','/api/v1/quotations/1/send','127.0.0.1','2026-07-28 00:10:15'),(67,1,'admin','QUOTATION_APPROVED','Quotation','1','SENT to APPROVED','POST','/api/v1/quotations/1/approve','127.0.0.1','2026-07-28 00:10:15'),(68,1,'admin','QUOTATION_PDF_GENERATED','Quotation','1','PDF generated','GET','/api/v1/quotations/1/pdf','127.0.0.1','2026-07-28 00:10:28'),(69,1,'admin','AGREEMENT_CREATED_FROM_QUOTATION','Agreement','1','Source quotation QT/2026-27/0001','POST','/api/v1/agreements/from-quotation/1','127.0.0.1','2026-07-28 00:10:58'),(70,1,'admin','QUOTATION_CONVERTED_TO_AGREEMENT','Agreement','1','Agreement AGR/2026-27/0001','POST','/api/v1/agreements/from-quotation/1','127.0.0.1','2026-07-28 00:10:58'),(71,1,'admin','AGREEMENT_UPDATED','Agreement','1','Draft updated','PUT','/api/v1/agreements/1','127.0.0.1','2026-07-28 00:10:58'),(72,1,'admin','AGREEMENT_DOCUMENT_GENERATED','Agreement','1','agreement-AGR-2026-27-0001.pdf','POST','/api/v1/agreements/1/generate-document','127.0.0.1','2026-07-28 00:10:58'),(73,1,'admin','AGREEMENT_READY_FOR_REVIEW','Agreement','1','Ready for review','POST','/api/v1/agreements/1/ready-for-review','127.0.0.1','2026-07-28 00:10:58'),(74,1,'admin','AGREEMENT_ACTIVATED','Agreement','1','Agreement activated','POST','/api/v1/agreements/1/activate','127.0.0.1','2026-07-28 00:10:58'),(75,1,'admin','QUOTATION_CREATED','Quotation','2','Draft created','POST','/api/v1/quotations','127.0.0.1','2026-07-28 00:11:21'),(76,1,'admin','QUOTATION_SENT','Quotation','2','DRAFT to SENT','POST','/api/v1/quotations/2/send','127.0.0.1','2026-07-28 00:11:21'),(77,1,'admin','QUOTATION_APPROVED','Quotation','2','SENT to APPROVED','POST','/api/v1/quotations/2/approve','127.0.0.1','2026-07-28 00:11:21'),(78,1,'admin','AGREEMENT_CREATED_FROM_QUOTATION','Agreement','2','Source quotation QT/2026-27/0002','POST','/api/v1/agreements/from-quotation/2','127.0.0.1','2026-07-28 00:11:21'),(79,1,'admin','QUOTATION_CONVERTED_TO_AGREEMENT','Agreement','2','Agreement AGR/2026-27/0002','POST','/api/v1/agreements/from-quotation/2','127.0.0.1','2026-07-28 00:11:21'),(80,1,'admin','AGREEMENT_UPDATED','Agreement','2','Draft updated','PUT','/api/v1/agreements/2','127.0.0.1','2026-07-28 00:11:21'),(81,1,'admin','AGREEMENT_DOCUMENT_GENERATED','Agreement','2','agreement-AGR-2026-27-0002.pdf','POST','/api/v1/agreements/2/generate-document','127.0.0.1','2026-07-28 00:11:21'),(82,1,'admin','AGREEMENT_READY_FOR_REVIEW','Agreement','2','Ready for review','POST','/api/v1/agreements/2/ready-for-review','127.0.0.1','2026-07-28 00:11:21'),(83,1,'admin','AGREEMENT_ACTIVATED','Agreement','2','Agreement activated','POST','/api/v1/agreements/2/activate','127.0.0.1','2026-07-28 00:11:21'),(84,1,'admin','ORDER_DRAFT_CREATED','Order','1','Draft created','POST','/api/v1/orders','127.0.0.1','2026-07-28 00:11:54'),(85,1,'admin','ORDER_CONFIRMED','Order','1','Order confirmed','POST','/api/v1/orders/1/confirm','127.0.0.1','2026-07-28 00:11:54'),(86,1,'admin','STOCK_CHALLAN_ISSUED','IssuedChallan','1','IC/2026-27/0001','POST','/api/v1/challans/issued','127.0.0.1','2026-07-28 00:11:54'),(87,1,'admin','STOCK_CHALLAN_ISSUED','IssuedChallan','2','IC/2026-27/0002','POST','/api/v1/challans/issued','127.0.0.1','2026-07-28 00:11:54'),(88,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:13:30'),(89,1,'admin','RECEIVING_CHALLAN_CREATED','ReceivingChallan','1','RC/2026-27/0001','POST','/api/v1/challans/receiving','127.0.0.1','2026-07-28 00:13:30'),(90,1,'admin','STOCK_LOSS_APPROVED','StockLoss','1','Created receiving-linked loss: LOSS/2026-27/0001','POST','/api/v1/challans/receiving/1/post','127.0.0.1','2026-07-28 00:13:30'),(91,1,'admin','STOCK_DAMAGE_RECORDED','StockDamage','1','Created receiving-linked damage: DMG/2026-27/0001','POST','/api/v1/challans/receiving/1/post','127.0.0.1','2026-07-28 00:13:30'),(92,1,'admin','RECEIVING_CHALLAN_POSTED','ReceivingChallan','1','RC/2026-27/0001','POST','/api/v1/challans/receiving/1/post','127.0.0.1','2026-07-28 00:13:30'),(93,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:14:39'),(94,1,'admin','STOCK_DAMAGE_REPAIR_STARTED','StockDamage','1','Started repair for: DMG/2026-27/0001','POST','/api/v1/stock-damages/1/start-repair','127.0.0.1','2026-07-28 00:15:04'),(95,1,'admin','STOCK_DAMAGE_REPAIRED','StockDamage','1','Repaired stock damage: DMG/2026-27/0001','POST','/api/v1/stock-damages/1/mark-repaired','127.0.0.1','2026-07-28 00:15:04'),(96,1,'admin','STOCK_DAMAGE_CREATED','StockDamage','2','Created manual damage: DMG/2026-27/0002','POST','/api/v1/stock-damages','127.0.0.1','2026-07-28 00:15:35'),(97,1,'admin','STOCK_DAMAGE_RECORDED','StockDamage','2','Recorded manual damage: DMG/2026-27/0002','POST','/api/v1/stock-damages/2/record','127.0.0.1','2026-07-28 00:15:35'),(98,1,'admin','STOCK_DAMAGE_SCRAPPED','StockDamage','2','Scrapped stock damage: DMG/2026-27/0002','POST','/api/v1/stock-damages/2/scrap','127.0.0.1','2026-07-28 00:15:35'),(99,1,'admin','SITE_TRANSFER_CREATED','SiteTransfer','1','Created site transfer: ST/2026-27/0001','POST','/api/v1/site-transfers','127.0.0.1','2026-07-28 00:16:00'),(100,1,'admin','SITE_TRANSFER_POSTED','SiteTransfer','1','Posted site transfer: ST/2026-27/0001','POST','/api/v1/site-transfers/1/post','127.0.0.1','2026-07-28 00:16:00'),(101,1,'admin','QUOTATION_CREATED','Quotation','3','Draft created','POST','/api/v1/quotations','127.0.0.1','2026-07-28 00:17:28'),(102,1,'admin','QUOTATION_CREATED','Quotation','4','Draft created','POST','/api/v1/quotations','127.0.0.1','2026-07-28 00:17:28'),(103,1,'admin','QUOTATION_SENT','Quotation','3','DRAFT to SENT','POST','/api/v1/quotations/3/send','127.0.0.1','2026-07-28 00:17:28'),(104,1,'admin','QUOTATION_APPROVED','Quotation','3','SENT to APPROVED','POST','/api/v1/quotations/3/approve','127.0.0.1','2026-07-28 00:17:28'),(105,1,'admin','AGREEMENT_CREATED_FROM_QUOTATION','Agreement','3','Source quotation QT/2026-27/0003','POST','/api/v1/agreements/from-quotation/3','127.0.0.1','2026-07-28 00:17:28'),(106,1,'admin','QUOTATION_CONVERTED_TO_AGREEMENT','Agreement','3','Agreement AGR/2026-27/0003','POST','/api/v1/agreements/from-quotation/3','127.0.0.1','2026-07-28 00:17:28'),(107,1,'admin','AGREEMENT_UPDATED','Agreement','3','Draft updated','PUT','/api/v1/agreements/3','127.0.0.1','2026-07-28 00:17:29'),(108,1,'admin','AGREEMENT_DOCUMENT_GENERATED','Agreement','3','agreement-AGR-2026-27-0003.pdf','POST','/api/v1/agreements/3/generate-document','127.0.0.1','2026-07-28 00:17:29'),(109,1,'admin','AGREEMENT_READY_FOR_REVIEW','Agreement','3','Ready for review','POST','/api/v1/agreements/3/ready-for-review','127.0.0.1','2026-07-28 00:17:29'),(110,1,'admin','AGREEMENT_ACTIVATED','Agreement','3','Agreement activated','POST','/api/v1/agreements/3/activate','127.0.0.1','2026-07-28 00:17:29'),(111,1,'admin','QUOTATION_SENT','Quotation','4','DRAFT to SENT','POST','/api/v1/quotations/4/send','127.0.0.1','2026-07-28 00:17:29'),(112,1,'admin','QUOTATION_APPROVED','Quotation','4','SENT to APPROVED','POST','/api/v1/quotations/4/approve','127.0.0.1','2026-07-28 00:17:29'),(113,1,'admin','AGREEMENT_CREATED_FROM_QUOTATION','Agreement','4','Source quotation QT/2026-27/0004','POST','/api/v1/agreements/from-quotation/4','127.0.0.1','2026-07-28 00:17:29'),(114,1,'admin','QUOTATION_CONVERTED_TO_AGREEMENT','Agreement','4','Agreement AGR/2026-27/0004','POST','/api/v1/agreements/from-quotation/4','127.0.0.1','2026-07-28 00:17:29'),(115,1,'admin','AGREEMENT_UPDATED','Agreement','4','Draft updated','PUT','/api/v1/agreements/4','127.0.0.1','2026-07-28 00:17:29'),(116,1,'admin','AGREEMENT_DOCUMENT_GENERATED','Agreement','4','agreement-AGR-2026-27-0004.pdf','POST','/api/v1/agreements/4/generate-document','127.0.0.1','2026-07-28 00:17:29'),(117,1,'admin','AGREEMENT_READY_FOR_REVIEW','Agreement','4','Ready for review','POST','/api/v1/agreements/4/ready-for-review','127.0.0.1','2026-07-28 00:17:29'),(118,1,'admin','AGREEMENT_ACTIVATED','Agreement','4','Agreement activated','POST','/api/v1/agreements/4/activate','127.0.0.1','2026-07-28 00:17:29'),(119,1,'admin','ORDER_DRAFT_CREATED','Order','2','Draft created','POST','/api/v1/orders','127.0.0.1','2026-07-28 00:17:54'),(120,1,'admin','ORDER_CONFIRMED','Order','2','Order confirmed','POST','/api/v1/orders/2/confirm','127.0.0.1','2026-07-28 00:17:54'),(121,1,'admin','STOCK_CHALLAN_ISSUED','IssuedChallan','3','IC/2026-27/0003','POST','/api/v1/challans/issued','127.0.0.1','2026-07-28 00:17:54'),(122,1,'admin','STOCK_CHALLAN_ISSUED','IssuedChallan','4','IC/2026-27/0004','POST','/api/v1/challans/issued','127.0.0.1','2026-07-28 00:17:55'),(123,1,'admin','RECEIVING_CHALLAN_CREATED','ReceivingChallan','2','RC/2026-27/0002','POST','/api/v1/challans/receiving','127.0.0.1','2026-07-28 00:17:55'),(124,1,'admin','STOCK_LOSS_APPROVED','StockLoss','2','Created receiving-linked loss: LOSS/2026-27/0002','POST','/api/v1/challans/receiving/2/post','127.0.0.1','2026-07-28 00:17:56'),(125,1,'admin','STOCK_DAMAGE_RECORDED','StockDamage','3','Created receiving-linked damage: DMG/2026-27/0003','POST','/api/v1/challans/receiving/2/post','127.0.0.1','2026-07-28 00:17:56'),(126,1,'admin','RECEIVING_CHALLAN_POSTED','ReceivingChallan','2','RC/2026-27/0002','POST','/api/v1/challans/receiving/2/post','127.0.0.1','2026-07-28 00:17:56'),(127,1,'admin','SITE_TRANSFER_CREATED','SiteTransfer','2','Created site transfer: ST/2026-27/0002','POST','/api/v1/site-transfers','127.0.0.1','2026-07-28 00:17:56'),(128,1,'admin','SITE_TRANSFER_POSTED','SiteTransfer','2','Posted site transfer: ST/2026-27/0002','POST','/api/v1/site-transfers/2/post','127.0.0.1','2026-07-28 00:17:56'),(129,1,'admin','BILLING_RUN_CREATED','BillingRun','1','Created draft billing run BR/2026-27/0001','POST','/api/v1/billing-runs','127.0.0.1','2026-07-28 00:18:22'),(130,1,'admin','BILLING_RUN_CALCULATED','BillingRun','1','Calculated rental timeline segments for run BR/2026-27/0001','POST','/api/v1/billing-runs','127.0.0.1','2026-07-28 00:18:22'),(131,1,'admin','BILLING_RUN_CALCULATED','BillingRun','1','Calculated rental timeline segments for run BR/2026-27/0001','POST','/api/v1/billing-runs/1/calculate','127.0.0.1','2026-07-28 00:18:22'),(132,1,'admin','BILLING_RUN_FINALIZED','BillingRun','1','Finalized billing run BR/2026-27/0001','POST','/api/v1/billing-runs/1/finalize','127.0.0.1','2026-07-28 00:18:22'),(133,1,'admin','INVOICE_CREATED','Invoice','1','Generated draft invoice INV/2026-27/0001 from run BR/2026-27/0001','POST','/api/v1/invoices/from-billing-run/1','127.0.0.1','2026-07-28 00:18:22'),(134,1,'admin','INVOICE_PDF_GENERATED','Invoice','1','Generated PDF file for invoice INV/2026-27/0001','POST','/api/v1/invoices/1/generate-pdf','127.0.0.1','2026-07-28 00:18:23'),(135,1,'admin','INVOICE_ISSUED','Invoice','1','Issued tax invoice INV/2026-27/0001','POST','/api/v1/invoices/1/issue','127.0.0.1','2026-07-28 00:18:23'),(136,1,'admin','PAYMENT_CREATED','PaymentReceipt','1','Created payment draft PR/2026-27/0001','POST','/api/v1/payments','127.0.0.1','2026-07-28 00:19:23'),(137,1,'admin','PAYMENT_POSTED','PaymentReceipt','1','Posted payment PR/2026-27/0001','POST','/api/v1/payments/1/post','127.0.0.1','2026-07-28 00:19:23'),(138,1,'admin','TDS_DETAILS_UPDATED','PaymentReceipt','1','Updated TDS certificate details for PR/2026-27/0001','PUT','/api/v1/payments/1/tds-details','127.0.0.1','2026-07-28 00:19:23'),(139,1,'admin','TDS_VERIFIED','PaymentReceipt','1','Verified TDS for payment PR/2026-27/0001','POST','/api/v1/payments/1/tds/verify','127.0.0.1','2026-07-28 00:19:23'),(140,1,'admin','PAYMENT_RECEIPT_GENERATED','PaymentReceipt','1','Generated receipt PDF for PR/2026-27/0001','GET','/api/v1/payments/1/receipt','127.0.0.1','2026-07-28 00:19:23'),(141,1,'admin','SECURITY_DEPOSIT_RECEIVED','SecurityDeposit','1','Received security deposit SD/2026-27/0001','POST','/api/v1/security-deposits/receipt','127.0.0.1','2026-07-28 00:19:24'),(142,1,'admin','SECURITY_DEPOSIT_ADJUSTED','SecurityDeposit','2','Adjusted security deposit SD/2026-27/0002 to invoice INV/2026-27/0001','POST','/api/v1/security-deposits/adjust-to-invoice','127.0.0.1','2026-07-28 00:19:24'),(143,1,'admin','PAYMENT_CREATED','PaymentReceipt','2','Created payment draft PR/2026-27/0002','POST','/api/v1/payments','127.0.0.1','2026-07-28 00:19:24'),(144,1,'admin','PAYMENT_POSTED','PaymentReceipt','2','Posted payment PR/2026-27/0002','POST','/api/v1/payments/2/post','127.0.0.1','2026-07-28 00:19:24'),(145,1,'admin','PAYMENT_REVERSED','PaymentReceipt','2','Reversed payment PR/2026-27/0002, reason: Controlled E2E payment reversal','POST','/api/v1/payments/2/reverse','127.0.0.1','2026-07-28 00:19:24'),(146,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:20:20'),(147,1,'admin','STOCK_LOSS_CREATED','StockLoss','3','Created manual loss: LOSS/2026-27/0003','POST','/api/v1/stock-losses','127.0.0.1','2026-07-28 00:20:20'),(148,1,'admin','STOCK_LOSS_APPROVED','StockLoss','3','Approved stock loss: LOSS/2026-27/0003','POST','/api/v1/stock-losses/3/approve','127.0.0.1','2026-07-28 00:20:20'),(149,1,'admin','STOCK_DAMAGE_CREATED','StockDamage','4','Created manual damage: DMG/2026-27/0004','POST','/api/v1/stock-damages','127.0.0.1','2026-07-28 00:20:20'),(150,1,'admin','STOCK_DAMAGE_RECORDED','StockDamage','4','Recorded manual damage: DMG/2026-27/0004','POST','/api/v1/stock-damages/4/record','127.0.0.1','2026-07-28 00:20:20'),(151,1,'admin','BILLING_RUN_CREATED','BillingRun','2','Created draft billing run BR/2026-27/0002','POST','/api/v1/billing-runs','127.0.0.1','2026-07-28 00:20:20'),(152,1,'admin','BILLING_RUN_CALCULATED','BillingRun','2','Calculated rental timeline segments for run BR/2026-27/0002','POST','/api/v1/billing-runs','127.0.0.1','2026-07-28 00:20:21'),(153,1,'admin','BILLING_RUN_CALCULATED','BillingRun','2','Calculated rental timeline segments for run BR/2026-27/0002','POST','/api/v1/billing-runs/2/calculate','127.0.0.1','2026-07-28 00:20:21'),(154,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:23:39'),(155,NULL,'admin','REPORT_PREVIEWED','REPORT','CURRENT_STOCK_SUMMARY','Previewed report CURRENT_STOCK_SUMMARY','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:23:39'),(156,NULL,'admin','REPORT_PREVIEWED','REPORT','GODOWN_STOCK','Previewed report GODOWN_STOCK','POST','/api/v1/reports/GODOWN_STOCK/preview','127.0.0.1','2026-07-28 00:23:39'),(157,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_PENDING_STOCK','Previewed report SITE_PENDING_STOCK','POST','/api/v1/reports/SITE_PENDING_STOCK/preview','127.0.0.1','2026-07-28 00:23:39'),(158,NULL,'admin','REPORT_PREVIEWED','REPORT','ITEM_LEDGER','Previewed report ITEM_LEDGER','POST','/api/v1/reports/ITEM_LEDGER/preview','127.0.0.1','2026-07-28 00:23:39'),(159,NULL,'admin','REPORT_PREVIEWED','REPORT','PARTY_STOCK_SUMMARY','Previewed report PARTY_STOCK_SUMMARY','POST','/api/v1/reports/PARTY_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:23:39'),(160,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_STOCK_SUMMARY','Previewed report SITE_STOCK_SUMMARY','POST','/api/v1/reports/SITE_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:23:39'),(161,NULL,'admin','REPORT_PREVIEWED','REPORT','PARTY_OUTSTANDING','Previewed report PARTY_OUTSTANDING','POST','/api/v1/reports/PARTY_OUTSTANDING/preview','127.0.0.1','2026-07-28 00:23:40'),(162,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_OUTSTANDING','Previewed report SITE_OUTSTANDING','POST','/api/v1/reports/SITE_OUTSTANDING/preview','127.0.0.1','2026-07-28 00:23:40'),(163,NULL,'admin','REPORT_PREVIEWED','REPORT','ISSUED_CHALLANS_REGISTER','Previewed report ISSUED_CHALLANS_REGISTER','POST','/api/v1/reports/ISSUED_CHALLANS_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(164,NULL,'admin','REPORT_PREVIEWED','REPORT','RECEIVING_CHALLANS_REGISTER','Previewed report RECEIVING_CHALLANS_REGISTER','POST','/api/v1/reports/RECEIVING_CHALLANS_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(165,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_TRANSFERS_REGISTER','Previewed report SITE_TRANSFERS_REGISTER','POST','/api/v1/reports/SITE_TRANSFERS_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(166,NULL,'admin','REPORT_PREVIEWED','REPORT','QUOTATION_REGISTER','Previewed report QUOTATION_REGISTER','POST','/api/v1/reports/QUOTATION_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(167,NULL,'admin','REPORT_PREVIEWED','REPORT','BILLING_RUN_REGISTER','Previewed report BILLING_RUN_REGISTER','POST','/api/v1/reports/BILLING_RUN_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(168,NULL,'admin','REPORT_PREVIEWED','REPORT','INVOICE_REGISTER','Previewed report INVOICE_REGISTER','POST','/api/v1/reports/INVOICE_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(169,NULL,'admin','REPORT_PREVIEWED','REPORT','PAYMENT_REGISTER','Previewed report PAYMENT_REGISTER','POST','/api/v1/reports/PAYMENT_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(170,NULL,'admin','REPORT_PREVIEWED','REPORT','TDS_REPORT','Previewed report TDS_REPORT','POST','/api/v1/reports/TDS_REPORT/preview','127.0.0.1','2026-07-28 00:23:40'),(171,NULL,'admin','REPORT_PREVIEWED','REPORT','SECURITY_DEPOSIT_REPORT','Previewed report SECURITY_DEPOSIT_REPORT','POST','/api/v1/reports/SECURITY_DEPOSIT_REPORT/preview','127.0.0.1','2026-07-28 00:23:40'),(172,NULL,'admin','REPORT_PREVIEWED','REPORT','OUTSTANDING_AGEING','Previewed report OUTSTANDING_AGEING','POST','/api/v1/reports/OUTSTANDING_AGEING/preview','127.0.0.1','2026-07-28 00:23:40'),(173,NULL,'admin','REPORT_PREVIEWED','REPORT','PARTY_SITE_LEDGER','Previewed report PARTY_SITE_LEDGER','POST','/api/v1/reports/PARTY_SITE_LEDGER/preview','127.0.0.1','2026-07-28 00:23:40'),(174,NULL,'admin','REPORT_PREVIEWED','REPORT','GST_SALES_REGISTER','Previewed report GST_SALES_REGISTER','POST','/api/v1/reports/GST_SALES_REGISTER/preview','127.0.0.1','2026-07-28 00:23:40'),(175,NULL,'admin','REPORT_PREVIEWED','REPORT','GST_TAX_SUMMARY','Previewed report GST_TAX_SUMMARY','POST','/api/v1/reports/GST_TAX_SUMMARY/preview','127.0.0.1','2026-07-28 00:23:40'),(176,NULL,'admin','REPORT_PREVIEWED','REPORT','GSTR1_PREPARATION','Previewed report GSTR1_PREPARATION','POST','/api/v1/reports/GSTR1_PREPARATION/preview','127.0.0.1','2026-07-28 00:23:40'),(177,NULL,'admin','REPORT_PREVIEWED','REPORT','GSTR3B_SUMMARY','Previewed report GSTR3B_SUMMARY','POST','/api/v1/reports/GSTR3B_SUMMARY/preview','127.0.0.1','2026-07-28 00:23:40'),(178,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:25:21'),(179,NULL,'admin','REPORT_PREVIEWED','REPORT','CURRENT_STOCK_SUMMARY','Previewed report CURRENT_STOCK_SUMMARY','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:25:21'),(180,NULL,'admin','REPORT_PREVIEWED','REPORT','GODOWN_STOCK','Previewed report GODOWN_STOCK','POST','/api/v1/reports/GODOWN_STOCK/preview','127.0.0.1','2026-07-28 00:25:21'),(181,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_PENDING_STOCK','Previewed report SITE_PENDING_STOCK','POST','/api/v1/reports/SITE_PENDING_STOCK/preview','127.0.0.1','2026-07-28 00:25:21'),(182,NULL,'admin','REPORT_PREVIEWED','REPORT','ITEM_LEDGER','Previewed report ITEM_LEDGER','POST','/api/v1/reports/ITEM_LEDGER/preview','127.0.0.1','2026-07-28 00:25:21'),(183,NULL,'admin','REPORT_PREVIEWED','REPORT','DAMAGE_LOSS_SCRAP','Previewed report DAMAGE_LOSS_SCRAP','POST','/api/v1/reports/DAMAGE_LOSS_SCRAP/preview','127.0.0.1','2026-07-28 00:25:21'),(184,NULL,'admin','REPORT_PREVIEWED','REPORT','PARTY_STOCK_SUMMARY','Previewed report PARTY_STOCK_SUMMARY','POST','/api/v1/reports/PARTY_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:25:21'),(185,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_STOCK_SUMMARY','Previewed report SITE_STOCK_SUMMARY','POST','/api/v1/reports/SITE_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:25:21'),(186,NULL,'admin','REPORT_PREVIEWED','REPORT','PARTY_OUTSTANDING','Previewed report PARTY_OUTSTANDING','POST','/api/v1/reports/PARTY_OUTSTANDING/preview','127.0.0.1','2026-07-28 00:25:22'),(187,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_OUTSTANDING','Previewed report SITE_OUTSTANDING','POST','/api/v1/reports/SITE_OUTSTANDING/preview','127.0.0.1','2026-07-28 00:25:22'),(188,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_ORDERS_REGISTER','Previewed report SITE_ORDERS_REGISTER','POST','/api/v1/reports/SITE_ORDERS_REGISTER/preview','127.0.0.1','2026-07-28 00:25:22'),(189,NULL,'admin','REPORT_PREVIEWED','REPORT','ISSUED_CHALLANS_REGISTER','Previewed report ISSUED_CHALLANS_REGISTER','POST','/api/v1/reports/ISSUED_CHALLANS_REGISTER/preview','127.0.0.1','2026-07-28 00:25:22'),(190,NULL,'admin','REPORT_PREVIEWED','REPORT','RECEIVING_CHALLANS_REGISTER','Previewed report RECEIVING_CHALLANS_REGISTER','POST','/api/v1/reports/RECEIVING_CHALLANS_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(191,NULL,'admin','REPORT_PREVIEWED','REPORT','SITE_TRANSFERS_REGISTER','Previewed report SITE_TRANSFERS_REGISTER','POST','/api/v1/reports/SITE_TRANSFERS_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(192,NULL,'admin','REPORT_PREVIEWED','REPORT','LOSS_RECORDS_REGISTER','Previewed report LOSS_RECORDS_REGISTER','POST','/api/v1/reports/LOSS_RECORDS_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(193,NULL,'admin','REPORT_PREVIEWED','REPORT','DAMAGE_RECORDS_REGISTER','Previewed report DAMAGE_RECORDS_REGISTER','POST','/api/v1/reports/DAMAGE_RECORDS_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(194,NULL,'admin','REPORT_PREVIEWED','REPORT','QUOTATION_REGISTER','Previewed report QUOTATION_REGISTER','POST','/api/v1/reports/QUOTATION_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(195,NULL,'admin','REPORT_PREVIEWED','REPORT','AGREEMENT_REGISTER','Previewed report AGREEMENT_REGISTER','POST','/api/v1/reports/AGREEMENT_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(196,NULL,'admin','REPORT_PREVIEWED','REPORT','BILLING_RUN_REGISTER','Previewed report BILLING_RUN_REGISTER','POST','/api/v1/reports/BILLING_RUN_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(197,NULL,'admin','REPORT_PREVIEWED','REPORT','INVOICE_REGISTER','Previewed report INVOICE_REGISTER','POST','/api/v1/reports/INVOICE_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(198,NULL,'admin','REPORT_PREVIEWED','REPORT','PAYMENT_REGISTER','Previewed report PAYMENT_REGISTER','POST','/api/v1/reports/PAYMENT_REGISTER/preview','127.0.0.1','2026-07-28 00:25:23'),(199,NULL,'admin','REPORT_PREVIEWED','REPORT','TDS_REPORT','Previewed report TDS_REPORT','POST','/api/v1/reports/TDS_REPORT/preview','127.0.0.1','2026-07-28 00:25:24'),(200,NULL,'admin','REPORT_PREVIEWED','REPORT','SECURITY_DEPOSIT_REPORT','Previewed report SECURITY_DEPOSIT_REPORT','POST','/api/v1/reports/SECURITY_DEPOSIT_REPORT/preview','127.0.0.1','2026-07-28 00:25:24'),(201,NULL,'admin','REPORT_PREVIEWED','REPORT','OUTSTANDING_AGEING','Previewed report OUTSTANDING_AGEING','POST','/api/v1/reports/OUTSTANDING_AGEING/preview','127.0.0.1','2026-07-28 00:25:25'),(202,NULL,'admin','REPORT_PREVIEWED','REPORT','PARTY_SITE_LEDGER','Previewed report PARTY_SITE_LEDGER','POST','/api/v1/reports/PARTY_SITE_LEDGER/preview','127.0.0.1','2026-07-28 00:25:25'),(203,NULL,'admin','REPORT_PREVIEWED','REPORT','GST_SALES_REGISTER','Previewed report GST_SALES_REGISTER','POST','/api/v1/reports/GST_SALES_REGISTER/preview','127.0.0.1','2026-07-28 00:25:25'),(204,NULL,'admin','REPORT_PREVIEWED','REPORT','GST_TAX_SUMMARY','Previewed report GST_TAX_SUMMARY','POST','/api/v1/reports/GST_TAX_SUMMARY/preview','127.0.0.1','2026-07-28 00:25:25'),(205,NULL,'admin','REPORT_PREVIEWED','REPORT','GSTR1_PREPARATION','Previewed report GSTR1_PREPARATION','POST','/api/v1/reports/GSTR1_PREPARATION/preview','127.0.0.1','2026-07-28 00:25:26'),(206,NULL,'admin','REPORT_PREVIEWED','REPORT','GSTR3B_SUMMARY','Previewed report GSTR3B_SUMMARY','POST','/api/v1/reports/GSTR3B_SUMMARY/preview','127.0.0.1','2026-07-28 00:25:26'),(207,NULL,'admin','REPORT_EXPORTED','REPORT_EXPORT','1','CURRENT_STOCK_SUMMARY CSV','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/export','127.0.0.1','2026-07-28 00:25:50'),(208,NULL,'admin','REPORT_EXPORTED','REPORT_EXPORT','2','CURRENT_STOCK_SUMMARY EXCEL','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/export','127.0.0.1','2026-07-28 00:25:51'),(209,NULL,'admin','REPORT_EXPORTED','REPORT_EXPORT','3','CURRENT_STOCK_SUMMARY PDF','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/export','127.0.0.1','2026-07-28 00:25:52'),(210,NULL,'admin','GST_REPORT_EXPORTED','REPORT_EXPORT','4','GST_SALES_REGISTER EXCEL','POST','/api/v1/reports/GST_SALES_REGISTER/export','127.0.0.1','2026-07-28 00:25:52'),(211,NULL,'admin','GST_REPORT_EXPORTED','REPORT_EXPORT','5','GSTR3B_SUMMARY EXCEL','POST','/api/v1/reports/GSTR3B_SUMMARY/export','127.0.0.1','2026-07-28 00:25:52'),(212,NULL,'e2eops','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:26:30'),(213,NULL,'e2eaccounts','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:26:30'),(214,NULL,'e2eviewer','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:26:30'),(215,2,'e2eops','USER_CREATED','User','2','User created: e2eops by admin','POST','/api/v1/users','127.0.0.1','2026-07-28 00:27:03'),(216,3,'e2eaccounts','USER_CREATED','User','3','User created: e2eaccounts by admin','POST','/api/v1/users','127.0.0.1','2026-07-28 00:27:03'),(217,4,'e2eviewer','USER_CREATED','User','4','User created: e2eviewer by admin','POST','/api/v1/users','127.0.0.1','2026-07-28 00:27:04'),(218,2,'e2eops','LOGIN_SUCCESS','Auth','2','Successful login for user: e2eops','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:27:04'),(219,3,'e2eaccounts','LOGIN_SUCCESS','Auth','3','Successful login for user: e2eaccounts','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:27:04'),(220,4,'e2eviewer','LOGIN_SUCCESS','Auth','4','Successful login for user: e2eviewer','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:27:04'),(221,NULL,'e2eops','REPORT_PREVIEWED','REPORT','CURRENT_STOCK_SUMMARY','Previewed report CURRENT_STOCK_SUMMARY','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:27:04'),(222,NULL,'e2eaccounts','REPORT_PREVIEWED','REPORT','CURRENT_STOCK_SUMMARY','Previewed report CURRENT_STOCK_SUMMARY','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:27:04'),(223,NULL,'e2eviewer','REPORT_PREVIEWED','REPORT','CURRENT_STOCK_SUMMARY','Previewed report CURRENT_STOCK_SUMMARY','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:27:04'),(224,2,'e2eops','ORDER_DRAFT_CREATED','Order','3','Draft created','POST','/api/v1/orders','127.0.0.1','2026-07-28 00:27:39'),(225,1,'admin','STOCK_IMPORT_UPLOADED','StockImportBatch','2','StockSync_Client_Opening_Stock_Import.xlsx','POST','/api/v1/stock-imports/upload','127.0.0.1','2026-07-28 00:28:08'),(226,1,'admin','STOCK_IMPORT_PARSED','StockImportBatch','2','42 source rows, 152 balance rows','POST','/api/v1/stock-imports/upload','127.0.0.1','2026-07-28 00:28:08'),(227,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for 4m Façade','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(228,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Ali Designer','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(229,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Avighnaa Kandivali','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(230,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Engarc','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(231,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Imperial','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(232,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Noble','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(233,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Zeeco Media','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:30'),(234,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Raymond GS','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(235,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Raymond Tenex','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(236,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Epilson','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(237,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for SBUT','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(238,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for SK Interior','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(239,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Sukoon','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(240,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for ANV','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(241,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Rocks & Logs','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(242,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Location mapping for Innovator Façade','PUT','/api/v1/stock-imports/2/location-mappings','127.0.0.1','2026-07-28 00:28:31'),(243,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Item mapping for source Excel row 6','PUT','/api/v1/stock-imports/2/rows/162/item-mapping','127.0.0.1','2026-07-28 00:28:46'),(244,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Item mapping for source Excel row 7','PUT','/api/v1/stock-imports/2/rows/163/item-mapping','127.0.0.1','2026-07-28 00:28:46'),(245,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Item mapping for source Excel row 37','PUT','/api/v1/stock-imports/2/rows/292/item-mapping','127.0.0.1','2026-07-28 00:28:46'),(246,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Item mapping for source Excel row 39','PUT','/api/v1/stock-imports/2/rows/297/item-mapping','127.0.0.1','2026-07-28 00:28:47'),(247,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Item mapping for source Excel row 6','PUT','/api/v1/stock-imports/2/rows/162/item-mapping','127.0.0.1','2026-07-28 00:29:27'),(248,1,'admin','STOCK_IMPORT_MAPPING_UPDATED','StockImportBatch','2','Item mapping for source Excel row 7','PUT','/api/v1/stock-imports/2/rows/163/item-mapping','127.0.0.1','2026-07-28 00:29:27'),(249,1,'admin','STOCK_IMPORT_VALIDATED','StockImportBatch','2','Corrected combined total 46019.0000','POST','/api/v1/stock-imports/2/validate','127.0.0.1','2026-07-28 00:29:27'),(250,1,'admin','STOCK_IMPORT_POSTED','StockImportBatch','2','Posted 152 opening rows','POST','/api/v1/stock-imports/2/post','127.0.0.1','2026-07-28 00:29:43'),(251,1,'admin','STOCK_IMPORT_REVERSED','StockImportBatch','2','E2E prove compensating reversal and non-destructive audit trail','POST','/api/v1/stock-imports/2/reverse','127.0.0.1','2026-07-28 00:29:48'),(252,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:40:56'),(253,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:43:43'),(254,NULL,'admin','REPORT_PREVIEWED','REPORT','CURRENT_STOCK_SUMMARY','Previewed report CURRENT_STOCK_SUMMARY','POST','/api/v1/reports/CURRENT_STOCK_SUMMARY/preview','127.0.0.1','2026-07-28 00:43:43'),(255,NULL,'admin','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:44:39'),(256,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 00:44:56'),(257,NULL,'admin','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 01:31:56'),(258,NULL,'admin','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 01:31:59'),(259,NULL,'admin','LOGIN_FAILURE','Auth',NULL,'Failed login attempt: invalid credentials','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 01:32:04'),(260,1,'admin','LOGIN_SUCCESS','Auth','1','Successful login for user: admin','POST','/api/v1/auth/login','127.0.0.1','2026-07-28 01:32:15');
/*!40000 ALTER TABLE `user_activity_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_user_roles_role` (`role_id`),
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (3,'ROLE_ACCOUNTS'),(1,'ROLE_ADMIN'),(2,'ROLE_OPERATIONS'),(4,'ROLE_VIEWER');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `last_login_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_username` (`username`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_active` (`active`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'E2E Admin','admin','admin@stocksync.local','$2a$10$/gX54yJbtquf8v4XUiEI3uITnwGhx1Gd7nOd885VVNDlWJSSisxhS',1,'2026-07-28 01:32:15','2026-07-28 00:07:57','bootstrap','2026-07-28 01:32:15','bootstrap',10),(2,'E2E ROLE_OPERATIONS','e2eops','e2eops@example.test','$2a$10$zMHkeHGW2l4LMZfsgxROgu0xHOlm3xCEa7S0fIWPi4VHh5VwSg.Z6',1,'2026-07-28 00:27:04','2026-07-28 00:27:03','admin','2026-07-28 00:27:04','admin',1),(3,'E2E ROLE_ACCOUNTS','e2eaccounts','e2eaccounts@example.test','$2a$10$OcYRKNqcEapu/b83CBQtzO/Sb361dFs91jAHmuyijFu6idv4XF40.',1,'2026-07-28 00:27:04','2026-07-28 00:27:03','admin','2026-07-28 00:27:04','admin',1),(4,'E2E ROLE_VIEWER','e2eviewer','e2eviewer@example.test','$2a$10$VRqQsfFXhWbCAKuKhRKKOOrdxJyvRMvDNIsU/un1yRN2sHMSs2flu',1,'2026-07-28 00:27:04','2026-07-28 00:27:04','admin','2026-07-28 00:27:04','admin',1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vendors`
--

DROP TABLE IF EXISTS `vendors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vendors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gstin` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vendors_name` (`name`),
  UNIQUE KEY `uk_vendors_gstin` (`gstin`),
  KEY `idx_vendors_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vendors`
--

LOCK TABLES `vendors` WRITE;
/*!40000 ALTER TABLE `vendors` DISABLE KEYS */;
/*!40000 ALTER TABLE `vendors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'shuttering_inventory_e2e'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-28 12:40:34
