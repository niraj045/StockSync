package com.stocksync.quotation;

import static org.assertj.core.api.Assertions.*;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.quotation.entity.*;
import com.stocksync.quotation.service.QuotationCalculationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuotationCalculationServiceTest {
    private final QuotationCalculationService service=new QuotationCalculationService();

    @Test void calculatesTaxableChargesDiscountAndSplitGst(){
        Quotation q=quotation();q.setDiscountType(DiscountType.PERCENTAGE);q.setDiscountValue(new BigDecimal("10"));
        q.setTransportCharge(new BigDecimal("100"));q.setCgstRate(new BigDecimal("9"));q.setSgstRate(new BigDecimal("9"));
        service.calculate(q);
        assertThat(q.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(q.getDiscountAmount()).isEqualByComparingTo("100.00");
        assertThat(q.getTaxableAmount()).isEqualByComparingTo("1000.00");
        assertThat(q.getTotalTax()).isEqualByComparingTo("180.00");
        assertThat(q.getGrandTotal()).isEqualByComparingTo("1180.00");
    }
    @Test void rejectsSplitAndIntegratedGstTogether(){
        Quotation q=quotation();q.setCgstRate(new BigDecimal("9"));q.setSgstRate(new BigDecimal("9"));q.setIgstRate(new BigDecimal("18"));
        assertThatThrownBy(()->service.calculate(q)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("cannot be applied together");
    }
    @Test void rejectsPercentageOverOneHundred(){
        Quotation q=quotation();q.setDiscountType(DiscountType.PERCENTAGE);q.setDiscountValue(new BigDecimal("101"));
        assertThatThrownBy(()->service.calculate(q)).isInstanceOf(BusinessRuleException.class);
    }
    private Quotation quotation(){
        Quotation q=new Quotation();QuotationItem i=new QuotationItem();i.setLineAmount(new BigDecimal("1000"));q.addItem(i);
        q.setDiscountType(DiscountType.NONE);q.setDiscountValue(BigDecimal.ZERO);q.setTransportCharge(BigDecimal.ZERO);q.setLoadingCharge(BigDecimal.ZERO);
        q.setUnloadingCharge(BigDecimal.ZERO);q.setOtherCharge(BigDecimal.ZERO);q.setCgstRate(BigDecimal.ZERO);q.setSgstRate(BigDecimal.ZERO);
        q.setIgstRate(BigDecimal.ZERO);q.setRoundOff(BigDecimal.ZERO);return q;
    }
}

