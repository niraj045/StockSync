package com.stocksync.common.numbering;

public enum DocumentType {
    QUOTATION("QT"),
    AGREEMENT("AGR"),
    SITE_ORDER("ORD"),
    ISSUED_CHALLAN("IC"),
    RECEIVING_CHALLAN("RC"),
    SITE_TRANSFER("ST"),
    STOCK_LOSS("LOSS"),
    STOCK_DAMAGE("DMG"),
    ITEM_EXCHANGE("EX");

    private final String prefix;
    DocumentType(String prefix) { this.prefix = prefix; }
    public String prefix() { return prefix; }
}
