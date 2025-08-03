package com.transferer.transaction.presentation;

import com.transferer.account.domain.AccountId;
import com.transferer.transaction.application.TransactionService;
import com.transferer.transaction.application.dto.CreateTransactionRequest;
import com.transferer.transaction.application.dto.TransactionResponse;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.TransactionStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public Mono<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        AccountId senderAccountId = AccountId.of(request.getSenderAccountId());
        AccountId recipientAccountId = AccountId.of(request.getRecipientAccountId());
        
        return transactionService.createTransaction(
                senderAccountId,
                recipientAccountId,
                request.getAmount(),
                request.getDescription()
        ).map(TransactionResponse::new);
    }

    @GetMapping("/{id}")
    public Mono<TransactionResponse> getTransaction(@PathVariable String id) {
        TransactionId transactionId = TransactionId.of(id);
        return transactionService.getTransaction(transactionId)
                .map(TransactionResponse::new);
    }

    @GetMapping("/account/{accountId}")
    public Flux<TransactionResponse> getTransactionsByAccount(@PathVariable String accountId) {
        AccountId accountIdObj = AccountId.of(accountId);
        return transactionService.getTransactionsByAccount(accountIdObj)
                .map(TransactionResponse::new);
    }

    @GetMapping("/sender/{accountId}")
    public Flux<TransactionResponse> getTransactionsBySender(@PathVariable String accountId) {
        AccountId senderAccountId = AccountId.of(accountId);
        return transactionService.getTransactionsBySender(senderAccountId)
                .map(TransactionResponse::new);
    }

    @GetMapping("/recipient/{accountId}")
    public Flux<TransactionResponse> getTransactionsByRecipient(@PathVariable String accountId) {
        AccountId recipientAccountId = AccountId.of(accountId);
        return transactionService.getTransactionsByRecipient(recipientAccountId)
                .map(TransactionResponse::new);
    }

    @GetMapping("/status/{status}")
    public Flux<TransactionResponse> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        return transactionService.getTransactionsByStatus(status)
                .map(TransactionResponse::new);
    }

    @PutMapping("/{id}/processing")
    public Mono<TransactionResponse> markAsProcessing(@PathVariable String id) {
        TransactionId transactionId = TransactionId.of(id);
        return transactionService.markTransactionAsProcessing(transactionId)
                .map(TransactionResponse::new);
    }

    @PutMapping("/{id}/completed")
    public Mono<TransactionResponse> markAsCompleted(@PathVariable String id) {
        TransactionId transactionId = TransactionId.of(id);
        return transactionService.markTransactionAsCompleted(transactionId)
                .map(TransactionResponse::new);
    }

    @PutMapping("/{id}/failed")
    public Mono<TransactionResponse> markAsFailed(@PathVariable String id, @RequestParam String reason) {
        TransactionId transactionId = TransactionId.of(id);
        return transactionService.markTransactionAsFailed(transactionId, reason)
                .map(TransactionResponse::new);
    }
}