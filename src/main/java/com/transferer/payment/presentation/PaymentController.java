package com.transferer.payment.presentation;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.application.PaymentService;
import com.transferer.payment.application.dto.InitiatePaymentRequest;
import com.transferer.payment.application.dto.PaymentResponse;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.transaction.domain.TransactionId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Mono<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        AccountId senderAccountId = AccountId.of(request.getSenderAccountId());
        AccountId recipientAccountId = AccountId.of(request.getRecipientAccountId());
        
        return paymentService.initiatePayment(
                senderAccountId,
                recipientAccountId,
                request.getAmount(),
                request.getDescription()
        ).map(PaymentResponse::new);
    }

    @GetMapping("/{id}")
    public Mono<PaymentResponse> getPayment(@PathVariable String id) {
        PaymentId paymentId = PaymentId.of(id);
        return paymentService.getPayment(paymentId)
                .map(PaymentResponse::new);
    }

    @GetMapping("/transaction/{transactionId}")
    public Mono<PaymentResponse> getPaymentByTransactionId(@PathVariable String transactionId) {
        TransactionId transactionIdObj = TransactionId.of(transactionId);
        return paymentService.getPaymentByTransactionId(transactionIdObj)
                .map(PaymentResponse::new);
    }

    @GetMapping("/account/{accountId}")
    public Flux<PaymentResponse> getPaymentsByAccount(@PathVariable String accountId) {
        AccountId accountIdObj = AccountId.of(accountId);
        return paymentService.getPaymentsByAccount(accountIdObj)
                .map(PaymentResponse::new);
    }

    @GetMapping("/status/{status}")
    public Flux<PaymentResponse> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return paymentService.getPaymentsByStatus(status)
                .map(PaymentResponse::new);
    }
}