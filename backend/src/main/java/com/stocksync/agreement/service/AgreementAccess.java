package com.stocksync.agreement.service;
import com.stocksync.agreement.entity.AgreementStatus;
import java.math.BigDecimal;
import java.util.List;
public interface AgreementAccess {
    OrderAgreement requireForOrder(Long agreementId);
    record OrderAgreement(Long id,Long partyId,Long siteId,AgreementStatus status,List<OrderAgreementItem>items){}
    record OrderAgreementItem(Long itemId,BigDecimal agreedQuantity){}
}
