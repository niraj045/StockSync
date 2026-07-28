package com.stocksync.dashboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Profile("local-scale")
public class LocalScaleDataGenerator implements ApplicationRunner, Ordered {
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final boolean exitWhenComplete;
    private final String profile;
    private final long seed;
    private final DashboardOverviewService dashboard;
    private final ConfigurableApplicationContext context;

    public LocalScaleDataGenerator(JdbcTemplate jdbc,
            DashboardOverviewService dashboard,
            ConfigurableApplicationContext context,
            @Value("${stocksync.scale.generator.enabled:false}") boolean enabled,
            @Value("${stocksync.scale.generator.exit:false}") boolean exitWhenComplete,
            @Value("${stocksync.scale.profile:SMALL}") String profile,
            @Value("${stocksync.scale.seed:20260728}") long seed) {
        this.jdbc = jdbc;
        this.dashboard = dashboard;
        this.context = context;
        this.enabled = enabled;
        this.exitWhenComplete = exitWhenComplete;
        this.profile = profile;
        this.seed = seed;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        Counts counts = Counts.of(profile);
        Random random = new Random(seed);
        LocalDate base = LocalDate.now().minusDays(120);
        System.out.printf("Starting StockSync local-scale data generation profile=%s seed=%d%n", counts.name(), seed);

        List<Long> categories = categories(6);
        List<Long> items = items(counts.items(), categories, random);
        List<Long> parties = parties(counts.parties());
        List<Long> sites = sites(parties, counts.sitesPerParty());
        List<Long> agreements = agreements(parties, sites, counts.agreements(), base, random);
        orders(agreements, parties, sites, counts.orders(), base, random);
        stock(items, sites, counts.stockTransactions(), base, random);
        commercial(agreements, parties, sites, counts.invoices(), base, random);
        exceptions(agreements, parties, sites, items, counts.exceptions(), base, random);

        System.out.printf("Completed StockSync local-scale data generation: parties=%d sites=%d items=%d agreements=%d orders=%d invoices=%d stockTransactions=%d%n",
                parties.size(), sites.size(), items.size(), agreements.size(), counts.orders(), counts.invoices(), counts.stockTransactions());
        printTimings();
        if (exitWhenComplete) {
            Thread shutdown = new Thread(() -> {
                try {
                    // Let the remaining ApplicationRunner beans finish before
                    // closing infrastructure such as the EntityManagerFactory.
                    Thread.sleep(250);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                SpringApplication.exit(context, () -> 0);
            }, "local-scale-shutdown");
            shutdown.setDaemon(false);
            shutdown.start();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void printTimings() {
        System.out.println("Starting StockSync local-scale dashboard timings");
        var auth = new UsernamePasswordAuthenticationToken("scale", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        long dashboardMs = timed(() -> dashboard.overview(null, null, null, LocalDate.now().minusDays(29), LocalDate.now(), auth));
        long stockPageMs = timed(() -> jdbc.queryForList("""
                SELECT i.item_code, i.item_name, sb.available_quantity, sb.damaged_quantity, sb.lost_quantity
                FROM stock_balances sb JOIN items i ON i.id=sb.item_id
                ORDER BY i.item_name LIMIT 50
                """));
        long ledgerPageMs = timed(() -> jdbc.queryForList("""
                SELECT transaction_date, item_id, site_id, party_id, direction, quantity
                FROM stock_transactions
                ORDER BY transaction_date DESC, id DESC LIMIT 50
                """));
        long invoicePageMs = timed(() -> jdbc.queryForList("""
                SELECT invoice_number, party_id, site_id, invoice_date, due_date, status, outstanding_amount
                FROM invoices
                WHERE status <> 'CANCELLED'
                ORDER BY invoice_date DESC, id DESC LIMIT 50
                """));
        long outstandingPageMs = timed(() -> jdbc.queryForList("""
                SELECT party_id, SUM(outstanding_amount) outstanding
                FROM invoices
                WHERE status <> 'CANCELLED'
                GROUP BY party_id
                ORDER BY outstanding DESC LIMIT 50
                """));
        System.out.printf("StockSync scale timings: dashboardOverviewMs=%d stockPageMs=%d ledgerPageMs=%d invoicePageMs=%d outstandingPageMs=%d%n",
                dashboardMs, stockPageMs, ledgerPageMs, invoicePageMs, outstandingPageMs);
    }

    private long timed(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private List<Long> categories(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            jdbc.update("INSERT INTO item_categories(name,active,created_by,updated_by) VALUES(?,?,?,?)", "Scale Category " + i, true, "scale", "scale");
            ids.add(id());
        }
        return ids;
    }

    private List<Long> items(int count, List<Long> categories, Random random) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Long categoryId = categories.get(i % categories.size());
            BigDecimal minimum = BigDecimal.valueOf(10 + random.nextInt(40));
            jdbc.update("INSERT INTO items(category_id,item_code,item_name,unit,minimum_stock,active,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?)",
                    categoryId, "SC-IT-" + i, "Scale Item " + i, "PCS", minimum, i % 17 != 0, "scale", "scale");
            ids.add(id());
        }
        return ids;
    }

    private List<Long> parties(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            jdbc.update("INSERT INTO parties(legal_name,gstin,address,state,active,created_by,updated_by) VALUES(?,?,?,?,?,?,?)",
                    "Scale Party " + i, i % 3 == 0 ? null : "24ABCDE" + String.format("%04d", i % 9999) + "F1Z5", "Local scale address", "GUJARAT", i % 19 != 0, "scale", "scale");
            ids.add(id());
        }
        return ids;
    }

    private List<Long> sites(List<Long> parties, int sitesPerParty) {
        List<Long> ids = new ArrayList<>();
        int number = 1;
        for (Long party : parties) {
            for (int i = 0; i < sitesPerParty; i++) {
                jdbc.update("INSERT INTO sites(party_id,site_name,site_code,address,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?)",
                        party, "Scale Site " + number, "SCL-" + number, "Scale site address", number % 23 == 0 ? "CLOSED" : "ACTIVE", "scale", "scale");
                ids.add(id());
                number++;
            }
        }
        return ids;
    }

    private List<Long> agreements(List<Long> parties, List<Long> sites, int count, LocalDate base, Random random) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Long site = sites.get(i % sites.size());
            Long party = partyForSite(site);
            LocalDate effective = base.plusDays(random.nextInt(90));
            LocalDate expiry = effective.plusDays(60 + random.nextInt(180));
            String status = i % 13 == 0 ? "TERMINATED" : expiry.isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE";
            jdbc.update("""
                    INSERT INTO agreements(agreement_number,party_id,site_id,agreement_date,effective_date,expiry_date,rental_type,billing_cycle,status,security_deposit,
                      party_legal_name_snapshot,party_address_snapshot,party_state_snapshot,site_name_snapshot,site_code_snapshot,site_address_snapshot,created_by,updated_by)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, "AGR-SCALE-" + i, party, site, effective, effective, expiry, i % 5 == 0 ? "SLAB_BASED" : "PER_PIECE_PER_DAY", "MONTHLY", status,
                    BigDecimal.valueOf(5000 + random.nextInt(50000)), "Scale Party", "Address", "GUJARAT", "Scale Site", "SCL", "Address", "scale", "scale");
            ids.add(id());
        }
        return ids;
    }

    private void orders(List<Long> agreements, List<Long> parties, List<Long> sites, int count, LocalDate base, Random random) {
        String[] statuses = {"DRAFT", "CONFIRMED", "PARTIALLY_FULFILLED", "FULFILLED", "COMPLETED"};
        for (int i = 1; i <= count; i++) {
            Long agreement = agreements.get(i % agreements.size());
            Long site = agreementSite(agreement);
            Long party = partyForSite(site);
            jdbc.update("INSERT INTO site_orders(order_number,agreement_id,party_id,site_id,order_date,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?)",
                    "SO-SCALE-" + i, agreement, party, site, base.plusDays(random.nextInt(120)), statuses[i % statuses.length], "scale", "scale");
        }
    }

    private void stock(List<Long> items, List<Long> sites, int transactions, LocalDate base, Random random) {
        for (Long item : items) {
            BigDecimal available = BigDecimal.valueOf(20 + random.nextInt(300));
            BigDecimal damaged = BigDecimal.valueOf(random.nextInt(12));
            BigDecimal lost = BigDecimal.valueOf(random.nextInt(5));
            BigDecimal scrapped = BigDecimal.valueOf(random.nextInt(8));
            jdbc.update("INSERT INTO stock_balances(item_id,available_quantity,damaged_quantity,lost_quantity,scrapped_quantity) VALUES(?,?,?,?,?)", item, available, damaged, lost, scrapped);
        }
        for (int i = 1; i <= Math.min(items.size() * 3, sites.size() * 20); i++) {
            jdbc.update("INSERT INTO site_stock_balances(site_id,item_id,pending_quantity) VALUES(?,?,?) ON DUPLICATE KEY UPDATE pending_quantity=pending_quantity+VALUES(pending_quantity)",
                    sites.get(i % sites.size()), items.get(i % items.size()), BigDecimal.valueOf(1 + random.nextInt(80)));
        }
        for (int i = 1; i <= transactions; i++) {
            Long site = sites.get(i % sites.size());
            Long party = partyForSite(site);
            String direction = i % 4 == 0 ? "IN" : "OUT";
            jdbc.update("INSERT INTO stock_transactions(item_id,transaction_type,transaction_date,quantity,direction,source_type,source_id,site_id,party_id,created_by) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    items.get(i % items.size()), direction.equals("OUT") ? "ISSUE" : "RETURN", base.plusDays(random.nextInt(120)), BigDecimal.valueOf(1 + random.nextInt(30)),
                    direction, direction.equals("OUT") ? "ISSUED_CHALLAN" : "RECEIVING_CHALLAN", (long) i, site, party, "scale");
        }
    }

    private void commercial(List<Long> agreements, List<Long> parties, List<Long> sites, int invoices, LocalDate base, Random random) {
        for (int i = 1; i <= invoices; i++) {
            Long agreement = agreements.get(i % agreements.size());
            Long site = agreementSite(agreement);
            Long party = partyForSite(site);
            BigDecimal total = BigDecimal.valueOf(1000 + random.nextInt(200000));
            BigDecimal outstanding = i % 4 == 0 ? BigDecimal.ZERO : total.multiply(BigDecimal.valueOf((i % 70) / 100.0)).setScale(2, java.math.RoundingMode.HALF_UP);
            LocalDate date = base.plusDays(random.nextInt(120));
            jdbc.update("INSERT INTO billing_runs(billing_run_number,agreement_id,party_id,site_id,period_start,period_end,status,grand_total,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    "BR-SCALE-" + i, agreement, party, site, date.minusDays(30), date, i % 9 == 0 ? "DRAFT" : "FINALIZED", total, "scale", "scale");
            Long runId = id();
            jdbc.update("""
                    INSERT INTO invoices(invoice_number,billing_run_id,agreement_id,party_id,site_id,invoice_date,due_date,period_start,period_end,status,
                     company_name_snapshot,company_address_snapshot,company_gstin_snapshot,party_legal_name_snapshot,party_gstin_snapshot,party_address_snapshot,party_state_snapshot,
                     site_name_snapshot,site_code_snapshot,site_address_snapshot,agreement_number_snapshot,taxable_amount,cgst_rate,cgst_amount,sgst_rate,sgst_amount,igst_rate,igst_amount,total_tax,grand_total,cash_allocated_total,tds_allocated_total,outstanding_amount,created_by,updated_by)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, "INV-SCALE-" + i, runId, agreement, party, site, date, date.plusDays(i % 5 == 0 ? -10 : 15), date.minusDays(30), date, i % 11 == 0 ? "DRAFT" : "ISSUED",
                    "StockSync", "Ahmedabad", "24AAAAA1111A1Z1", "Scale Party", null, "Address", "GUJARAT", "Scale Site", "SCL", "Address", "AGR-SCALE",
                    total.multiply(new BigDecimal("0.8475")).setScale(2, java.math.RoundingMode.HALF_UP), BigDecimal.valueOf(9), total.multiply(new BigDecimal("0.07625")).setScale(2, java.math.RoundingMode.HALF_UP),
                    BigDecimal.valueOf(9), total.multiply(new BigDecimal("0.07625")).setScale(2, java.math.RoundingMode.HALF_UP), BigDecimal.ZERO, BigDecimal.ZERO,
                    total.multiply(new BigDecimal("0.1525")).setScale(2, java.math.RoundingMode.HALF_UP), total, total.subtract(outstanding), BigDecimal.ZERO, outstanding, "scale", "scale");
            if (i % 2 == 0) {
                jdbc.update("INSERT INTO payment_receipts(receipt_number,party_id,site_id,party_name_snapshot,site_name_snapshot,payment_date,payment_mode,cash_amount,tds_amount,total_settlement_amount,unallocated_amount,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        "PR-SCALE-" + i, party, site, "Scale Party", "Scale Site", date.plusDays(3), "NEFT", total.subtract(outstanding), BigDecimal.valueOf(i % 7 == 0 ? 250 : 0), total.subtract(outstanding).add(BigDecimal.valueOf(i % 7 == 0 ? 250 : 0)), i % 10 == 0 ? BigDecimal.valueOf(500) : BigDecimal.ZERO, "POSTED", "scale", "scale");
            }
            if (i % 20 == 0) {
                jdbc.update("INSERT INTO security_deposit_transactions(deposit_number,agreement_id,party_id,site_id,agreement_number_snapshot,party_name_snapshot,site_name_snapshot,transaction_type,transaction_date,amount,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        "SD-SCALE-" + i, agreement, party, site, "AGR-SCALE", "Scale Party", "Scale Site", "RECEIPT", date, BigDecimal.valueOf(10000), "POSTED", "scale", "scale");
            }
        }
    }

    private void exceptions(List<Long> agreements, List<Long> parties, List<Long> sites, List<Long> items, int count, LocalDate base, Random random) {
        for (int i = 1; i <= count; i++) {
            Long agreement = agreements.get(i % agreements.size());
            Long site = agreementSite(agreement);
            Long party = partyForSite(site);
            Long item = items.get(i % items.size());
            jdbc.update("INSERT INTO loss_records(loss_number,source_type,agreement_id,party_id,site_id,item_id,loss_date,quantity,weight,charge_method,recovery_rate,calculated_recovery_amount,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "LR-SCALE-" + i, "MANUAL", agreement, party, site, item, base.plusDays(random.nextInt(120)), BigDecimal.ONE, BigDecimal.ZERO, "PER_PIECE", BigDecimal.valueOf(100), BigDecimal.valueOf(100), i % 3 == 0 ? "PENDING_APPROVAL" : "APPROVED", "scale", "scale");
            jdbc.update("INSERT INTO damage_records(damage_number,source_type,agreement_id,party_id,site_id,item_id,damage_date,quantity,weight,repairable,damage_type,charge_method,damage_rate,calculated_damage_amount,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "DR-SCALE-" + i, "MANUAL", agreement, party, site, item, base.plusDays(random.nextInt(120)), BigDecimal.ONE, BigDecimal.ZERO, true, "BENT", "PER_PIECE", BigDecimal.valueOf(50), BigDecimal.valueOf(50), i % 4 == 0 ? "UNDER_REPAIR" : "RECORDED", "scale", "scale");
        }
    }

    private Long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long partyForSite(Long siteId) {
        return jdbc.queryForObject("SELECT party_id FROM sites WHERE id=?", Long.class, siteId);
    }

    private Long agreementSite(Long agreementId) {
        return jdbc.queryForObject("SELECT site_id FROM agreements WHERE id=?", Long.class, agreementId);
    }

    private record Counts(String name, int parties, int sitesPerParty, int items, int agreements, int orders, int invoices, int stockTransactions, int exceptions) {
        static Counts of(String profile) {
            return switch (profile == null ? "SMALL" : profile.toUpperCase()) {
                case "LARGE" -> new Counts("LARGE", 180, 3, 1200, 1200, 5000, 10000, 100000, 2000);
                case "MEDIUM" -> new Counts("MEDIUM", 60, 3, 350, 350, 1500, 2500, 25000, 500);
                default -> new Counts("SMALL", 10, 2, 80, 80, 200, 300, 2500, 80);
            };
        }
    }
}
