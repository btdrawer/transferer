package com.transferer.account.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.body.DomainEventBody;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;

public class AccountDebitedEventBody extends DomainEventBody {
    private final AccountId accountId;
    private final TransactionId transactionId;
    private final String accountNumber;
    private final BigDecimal amount;
    private final BigDecimal newBalance;

    public AccountDebitedEventBody(
            AccountId accountId,
            TransactionId transactionId,
            String accountNumber,
            BigDecimal amount,
            BigDecimal newBalance
    ) {
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.newBalance = newBalance;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public TransactionId getTransactionId() { return transactionId; }

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