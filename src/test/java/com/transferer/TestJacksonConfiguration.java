package com.transferer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.account.domain.AccountId;
import com.transferer.account.infrastructure.AccountIdConverter;
import com.transferer.payment.domain.PaymentId;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.shared.events.EventBus;
import com.transferer.shared.events.InMemoryEventBus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import java.util.List;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;

import java.util.Arrays;

@TestConfiguration
public class TestJacksonConfiguration extends AbstractR2dbcConfiguration {
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public EventBus eventBus() {
        return new InMemoryEventBus();
    }
    
    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get("r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }
    
    @Override
    protected List<Object> getCustomConverters() {
        return Arrays.asList(
            new AccountIdConverter.AccountIdReadingConverter(),
            new AccountIdConverter.AccountIdWritingConverter(),
            new PaymentIdReadingConverter(),
            new PaymentIdWritingConverter(),
            new TransactionIdReadingConverter(),
            new TransactionIdWritingConverter()
        );
    }

    @ReadingConverter
    public static class PaymentIdReadingConverter implements Converter<String, PaymentId> {
        @Override
        public PaymentId convert(String source) {
            return source != null ? PaymentId.of(source) : null;
        }
    }

    @WritingConverter
    public static class PaymentIdWritingConverter implements Converter<PaymentId, String> {
        @Override
        public String convert(PaymentId source) {
            return source != null ? source.getValue() : null;
        }
    }

    @ReadingConverter
    public static class TransactionIdReadingConverter implements Converter<String, TransactionId> {
        @Override
        public TransactionId convert(String source) {
            return source != null ? TransactionId.of(source) : null;
        }
    }

    @WritingConverter
    public static class TransactionIdWritingConverter implements Converter<TransactionId, String> {
        @Override
        public String convert(TransactionId source) {
            return source != null ? source.getValue() : null;
        }
    }
}