package com.transferer.account.infrastructure;

import com.transferer.account.domain.AccountId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

public class AccountIdConverter {

    @Component
    @ReadingConverter
    public static class AccountIdReadingConverter implements Converter<String, AccountId> {
        @Override
        public AccountId convert(String source) {
            return source != null ? AccountId.of(source) : null;
        }
    }

    @Component
    @WritingConverter
    public static class AccountIdWritingConverter implements Converter<AccountId, String> {
        @Override
        public String convert(AccountId source) {
            return source != null ? source.getValue() : null;
        }
    }
}