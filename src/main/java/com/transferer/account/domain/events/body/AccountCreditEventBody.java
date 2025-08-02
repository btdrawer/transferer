package com.transferer.account.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.body.DomainEventBody;

import java.math.BigDecimal;

public class AccountCreditEventBody extends DomainEventBody {
    private final AccountId accountId;
    private final String accountNumber;
    private final BigDecimal amount;
    private final BigDecimal newBalance;

    public AccountCreditEventBody(
            AccountId accountId,
            String accountNumber,
            BigDecimal amount,
            BigDecimal newBalance
    ) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.newBalance = newBalance;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }
}
