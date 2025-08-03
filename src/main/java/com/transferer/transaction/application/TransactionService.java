package com.transferer.transaction.application;

import com.transferer.account.domain.AccountId;
import com.transferer.transaction.domain.Transaction;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.TransactionRepository;
import com.transferer.transaction.domain.TransactionStatus;
import com.transferer.transaction.domain.events.TransactionCreatedEvent;
import com.transferer.transaction.domain.events.TransactionCompletedEvent;
import com.transferer.transaction.domain.events.TransactionFailedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Mono<Transaction> createTransaction(AccountId senderAccountId, AccountId recipientAccountId, BigDecimal amount, String description) {
        if (senderAccountId == null) {
            return Mono.error(new IllegalArgumentException("Sender account ID cannot be null"));
        }
        if (recipientAccountId == null) {
            return Mono.error(new IllegalArgumentException("Recipient account ID cannot be null"));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be positive"));
        }

        return Mono.fromCallable(() -> new Transaction(senderAccountId, recipientAccountId, amount, description))
                .flatMap(transaction -> {
                    TransactionCreatedEvent event = new TransactionCreatedEvent(
                            transaction.getId(),
                            transaction.getSenderAccountId(),
                            transaction.getRecipientAccountId(),
                            transaction.getAmount(),
                            transaction.getDescription()
                    );
                    return transactionRepository.saveAndPublishEvents(transaction, Collections.singletonList(event));
                });
    }

    @Transactional(readOnly = true)
    public Mono<Transaction> getTransaction(TransactionId transactionId) {
        return transactionRepository.findById(transactionId)
                .switchIfEmpty(Mono.error(new TransactionNotFoundException("Transaction not found with ID: " + transactionId)));
    }

    @Transactional(readOnly = true)
    public Flux<Transaction> getTransactionsByAccount(AccountId accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public Flux<Transaction> getTransactionsBySender(AccountId senderAccountId) {
        return transactionRepository.findBySenderAccountId(senderAccountId);
    }

    @Transactional(readOnly = true)
    public Flux<Transaction> getTransactionsByRecipient(AccountId recipientAccountId) {
        return transactionRepository.findByRecipientAccountId(recipientAccountId);
    }

    @Transactional(readOnly = true)
    public Flux<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public Mono<Transaction> markTransactionAsProcessing(TransactionId transactionId) {
        return getTransaction(transactionId)
                .doOnNext(Transaction::markAsProcessing)
                .flatMap(transactionRepository::save);
    }

    public Mono<Transaction> markTransactionAsCompleted(TransactionId transactionId) {
        return getTransaction(transactionId)
                .doOnNext(Transaction::markAsCompleted)
                .flatMap(transaction -> {
                    TransactionCompletedEvent event = new TransactionCompletedEvent(
                            transaction.getId(),
                            transaction.getSenderAccountId(),
                            transaction.getRecipientAccountId(),
                            transaction.getAmount(),
                            transaction.getCompletedAt()
                    );
                    return transactionRepository.saveAndPublishEvents(transaction, Collections.singletonList(event));
                });
    }

    public Mono<Transaction> markTransactionAsFailed(TransactionId transactionId, String failureReason) {
        return getTransaction(transactionId)
                .doOnNext(Transaction::markAsFailed)
                .flatMap(transaction -> {
                    TransactionFailedEvent event = new TransactionFailedEvent(
                            transaction.getId(),
                            transaction.getSenderAccountId(),
                            transaction.getRecipientAccountId(),
                            transaction.getAmount(),
                            failureReason
                    );
                    return transactionRepository.saveAndPublishEvents(transaction, Collections.singletonList(event));
                });
    }
}