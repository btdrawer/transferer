package com.transferer.payment.infrastructure;

import com.transferer.payment.domain.PaymentId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

public class PaymentIdConverter {

    @Component
    @ReadingConverter
    public static class PaymentIdReadingConverter implements Converter<String, PaymentId> {
        @Override
        public PaymentId convert(String source) {
            return source != null ? PaymentId.of(source) : null;
        }
    }

    @Component
    @WritingConverter
    public static class PaymentIdWritingConverter implements Converter<PaymentId, String> {
        @Override
        public String convert(PaymentId source) {
            return source != null ? source.getValue() : null;
        }
    }
}