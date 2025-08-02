package com.transferer.account.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.body.DomainEventBody;

public class AccountActivatedEventBody extends DomainEventBody {
    private final AccountId accountId;
    private final String accountNumber;

    public AccountActivatedEventBody(AccountId accountId, String accountNumber) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
