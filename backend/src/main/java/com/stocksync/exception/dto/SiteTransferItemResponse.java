package com.stocksync.exception.dto;

import java.math.BigDecimal;

public record SiteTransferItemResponse(
    Long id,
    Long sourceAgreementItemId,
    Long destinationAgreementItemId,
    Long itemId,
    String itemCode,
    String itemName,
    String description,
    String size,
    String unit,
    BigDecimal weightPerPiece,
    BigDecimal quantity,
    BigDecimal totalWeight,
    BigDecimal sourcePendingBefore,
    BigDecimal sourcePendingAfter,
    BigDecimal destinationPendingBefore,
    BigDecimal destinationPendingAfter,
    int sequence
) {}
