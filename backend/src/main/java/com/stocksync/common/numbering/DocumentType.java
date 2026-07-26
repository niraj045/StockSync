package com.stocksync.common.numbering;

public enum DocumentType {
    QUOTATION("QT"),
    AGREEMENT("AGR");

    private final String prefix;
    DocumentType(String prefix) { this.prefix = prefix; }
    public String prefix() { return prefix; }
}
