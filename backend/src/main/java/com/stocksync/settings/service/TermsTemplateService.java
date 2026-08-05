package com.stocksync.settings.service;

import com.stocksync.settings.dto.DefaultTermsResponse;
import org.springframework.stereotype.Service;

@Service
public class TermsTemplateService {

    private static final String DEFAULT_TERMS = """
            1. Definitions
            Owner / We / Us means SteelFab Scaffoldings & Engineering Private Limited. Hirer / You / Your means the client accepting this quotation or agreement. Scaffolding Materials means all items supplied on hire under this document.

            2. Ownership and Responsibility
            All scaffolding materials remain the property of SteelFab Scaffoldings & Engineering Private Limited. The hirer shall be responsible for the materials from delivery until return and final reconciliation.

            3. Hirer Responsibilities
            The hirer shall use the materials only for the stated site, keep the materials in safe custody, and return the same materials in usable condition, normal wear and tear excepted.

            4. Hire Period, Returns, Loss and Damage
            Minimum hire period shall apply as mutually agreed. Materials not returned, returned short, or returned damaged shall be charged at the applicable replacement or repair rates.

            5. Security and Advance Payment
            Security deposit and advance rent, wherever applicable, shall be payable before dispatch. Security deposit shall be adjusted or refunded after final reconciliation of all dues, shortages and damages.

            6. Invoicing and Payment Terms
            Hire invoices shall be raised as per the agreed billing cycle. Payment shall be made within the agreed due period from the invoice date.

            7. Transportation
            To and fro transportation, Mathadi union payments, taxes and local statutory payments shall be in the client's scope unless specifically mentioned otherwise.

            8. Delivery and Delays
            Delivery shall be subject to availability of materials, transport arrangement and site readiness. SteelFab shall not be responsible for delays caused by site restrictions, third-party transport, local authority issues or force majeure events.

            9. Cancellation
            Cancellation after confirmation may attract charges for preparation, loading, transport or other committed costs.

            10. Complete Contract Agreement
            This document, together with accepted commercial terms and signed challans, forms the complete understanding between SteelFab and the hirer for the materials supplied on hire.
            """.trim();

    private static final String DEFAULT_HEADER = "With reference to your requirement, we are pleased to submit our quotation for the supply of scaffolding materials on hire.";
    private static final String DEFAULT_AGREEMENT_HEADER = "With reference to our discussions and the approved quotation, we are pleased to confirm this agreement for the hire of scaffolding materials for your project. The commercial details and the conditions governing the hire are recorded below.";

    public DefaultTermsResponse getDefaultTerms(String documentType) {
        // Both Quotation and Agreement currently share the same default terms,
        // but this service encapsulates that logic and allows future divergence.
        String header = "AGREEMENT".equalsIgnoreCase(documentType) ? DEFAULT_AGREEMENT_HEADER : DEFAULT_HEADER;
        return new DefaultTermsResponse(documentType, header, "PART A: HIRE CHARGES & COSTS", null, "PART B: TERMS AND CONDITIONS", DEFAULT_TERMS, 1);
    }
}
