package com.transferer.account.infrastructure;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.util.List;

@Configuration
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {

    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get("r2dbc:postgresql://transferer:transferer@localhost:5432/transferer");
    }

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(
            new AccountIdConverter.AccountIdReadingConverter(),
            new AccountIdConverter.AccountIdWritingConverter()
        );
    }
}