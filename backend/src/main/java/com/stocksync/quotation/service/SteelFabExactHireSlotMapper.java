package com.stocksync.quotation.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.quotation.dto.QuotationItemResponse;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SteelFabExactHireSlotMapper {
    public enum Slot { H_FRAME, BRACING, MS_PIPE, PLATE_PIPE, BASE_JACK, PLATFORM, COUPLER }

    public Map<Slot, QuotationItemResponse> map(List<QuotationItemResponse> items) {
        Map<Slot, QuotationItemResponse> result = new EnumMap<>(Slot.class);
        for (QuotationItemResponse item : items) {
            Slot slot = resolve(item.itemCodeSnapshot() + " " + item.itemNameSnapshot());
            if (slot == null) {
                throw new BusinessRuleException("UNSUPPORTED_EXACT_HIRE_ITEM",
                        "Exact SteelFab PDF supports only H Frame, Bracing, MS Pipe, Plate Pipe, Base Jack, Platform and Coupler");
            }
            if (result.put(slot, item) != null) {
                throw new BusinessRuleException("DUPLICATE_EXACT_HIRE_SLOT", "Only one item may occupy each exact PDF material row");
            }
        }
        if (result.size() != Slot.values().length) {
            throw new BusinessRuleException("EXACT_HIRE_ITEMS_REQUIRED", "All seven SteelFab material rows are required");
        }
        return result;
    }

    Slot resolve(String value) {
        String key = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (key.contains("platepipe")) return Slot.PLATE_PIPE;
        if (key.contains("hframe") || key.contains("hframes")) return Slot.H_FRAME;
        if (key.contains("bracing") || key.contains("crossbrace")) return Slot.BRACING;
        if (key.contains("20ftpipe") || key.contains("mspipe") || key.contains("steelpipe")) return Slot.MS_PIPE;
        if (key.contains("basejack")) return Slot.BASE_JACK;
        if (key.contains("platform") || key.contains("walkway")) return Slot.PLATFORM;
        if (key.contains("coupler") || key.contains("clamp")) return Slot.COUPLER;
        return null;
    }
}
