package com.stocksync.common.numbering;

public enum DocumentType {
    QUOTATION("QT");

    private final String prefix;
    DocumentType(String prefix) { this.prefix = prefix; }
    public String prefix() { return prefix; }
}

