package com.transferer.shared.infrastructure;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.transaction.domain.TransactionId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class R2dbcConfiguration {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new AccountIdToStringConverter());
        converters.add(new StringToAccountIdConverter());
        converters.add(new PaymentIdToStringConverter());
        converters.add(new StringToPaymentIdConverter());
        converters.add(new TransactionIdToStringConverter());
        converters.add(new StringToTransactionIdConverter());
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    @WritingConverter
    static class AccountIdToStringConverter implements Converter<AccountId, String> {
        @Override
        public String convert(AccountId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    static class StringToAccountIdConverter implements Converter<String, AccountId> {
        @Override
        public AccountId convert(String source) {
            return AccountId.of(source);
        }
    }

    @WritingConverter
    static class PaymentIdToStringConverter implements Converter<PaymentId, String> {
        @Override
        public String convert(PaymentId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    static class StringToPaymentIdConverter implements Converter<String, PaymentId> {
        @Override
        public PaymentId convert(String source) {
            return PaymentId.of(source);
        }
    }

    @WritingConverter
    static class TransactionIdToStringConverter implements Converter<TransactionId, String> {
        @Override
        public String convert(TransactionId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    static class StringToTransactionIdConverter implements Converter<String, TransactionId> {
        @Override
        public TransactionId convert(String source) {
            return TransactionId.of(source);
        }
    }
}