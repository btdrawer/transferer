package com.transferer.account.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.body.DomainEventBody;

import java.math.BigDecimal;

public class AccountOpenedEventBody extends DomainEventBody {
    private final AccountId accountId;
    private final String accountNumber;
    private final String holderName;
    private final BigDecimal initialBalance;

    public AccountOpenedEventBody(
            AccountId accountId,
            String accountNumber,
            String holderName,
            BigDecimal initialBalance
    ) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.initialBalance = initialBalance;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
}