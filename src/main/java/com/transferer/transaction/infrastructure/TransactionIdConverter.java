package com.transferer.transaction.infrastructure;

import com.transferer.transaction.domain.TransactionId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

public class TransactionIdConverter {

    @Component
    @ReadingConverter
    public static class TransactionIdReadingConverter implements Converter<String, TransactionId> {
        @Override
        public TransactionId convert(String source) {
            return source != null ? TransactionId.of(source) : null;
        }
    }

    @Component
    @WritingConverter
    public static class TransactionIdWritingConverter implements Converter<TransactionId, String> {
        @Override
        public String convert(TransactionId source) {
            return source != null ? source.getValue() : null;
        }
    }
}