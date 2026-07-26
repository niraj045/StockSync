package com.stocksync.quotation.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.quotation.entity.*;
import java.math.*;
import org.springframework.stereotype.Service;

@Service
public class QuotationCalculationService {
    private static final BigDecimal HUNDRED=new BigDecimal("100");
    private static final int MONEY_SCALE=2;

    public void calculate(Quotation q){
        validateTaxes(q);
        BigDecimal subtotal=q.getItems().stream().map(QuotationItem::getLineAmount).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(MONEY_SCALE,RoundingMode.HALF_UP);
        BigDecimal discount=switch(q.getDiscountType()){
            case NONE -> BigDecimal.ZERO;
            case PERCENTAGE -> {
                if(q.getDiscountValue().compareTo(HUNDRED)>0)throw new BusinessRuleException("INVALID_DISCOUNT","Percentage discount cannot exceed 100");
                yield percent(subtotal,q.getDiscountValue());
            }
            case FIXED -> {
                if(q.getDiscountValue().compareTo(subtotal)>0)throw new BusinessRuleException("INVALID_DISCOUNT","Fixed discount cannot exceed line subtotal");
                yield money(q.getDiscountValue());
            }
        };
        // Phase 5A policy: transport/loading/unloading/other charges are taxable.
        BigDecimal charges=q.getTransportCharge().add(q.getLoadingCharge()).add(q.getUnloadingCharge()).add(q.getOtherCharge());
        BigDecimal taxable=subtotal.subtract(discount).add(charges).setScale(MONEY_SCALE,RoundingMode.HALF_UP);
        BigDecimal cgst=percent(taxable,q.getCgstRate()),sgst=percent(taxable,q.getSgstRate()),igst=percent(taxable,q.getIgstRate());
        BigDecimal tax=cgst.add(sgst).add(igst).setScale(MONEY_SCALE,RoundingMode.HALF_UP);
        q.setSubtotal(subtotal);q.setDiscountAmount(discount);q.setTaxableAmount(taxable);
        q.setCgstAmount(cgst);q.setSgstAmount(sgst);q.setIgstAmount(igst);q.setTotalTax(tax);
        q.setTaxRate(q.getCgstRate().add(q.getSgstRate()).add(q.getIgstRate()));
        q.setTaxAmount(tax);q.setGrandTotal(taxable.add(tax).add(q.getRoundOff()).setScale(MONEY_SCALE,RoundingMode.HALF_UP));
    }
    private void validateTaxes(Quotation q){
        boolean split=q.getCgstRate().signum()>0||q.getSgstRate().signum()>0;
        if(split&&q.getIgstRate().signum()>0)throw new BusinessRuleException("CONFLICTING_GST","CGST/SGST and IGST cannot be applied together");
        if(q.getCgstRate().compareTo(q.getSgstRate())!=0)throw new BusinessRuleException("INVALID_SPLIT_GST","CGST and SGST rates must be equal");
    }
    private BigDecimal percent(BigDecimal base,BigDecimal rate){return base.multiply(rate).divide(HUNDRED,MONEY_SCALE,RoundingMode.HALF_UP);}
    private BigDecimal money(BigDecimal value){return value.setScale(MONEY_SCALE,RoundingMode.HALF_UP);}
}
