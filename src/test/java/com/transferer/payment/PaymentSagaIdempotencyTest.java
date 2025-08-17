package com.transferer.payment;

import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

@DataR2dbcTest
@Import({
    com.transferer.payment.application.PaymentService.class, 
    com.transferer.account.application.AccountService.class, 
    com.transferer.transaction.application.TransactionService.class,
    com.transferer.payment.infrastructure.R2dbcPaymentRepository.class,
    com.transferer.account.infrastructure.R2dbcAccountRepository.class,
    com.transferer.transaction.infrastructure.R2dbcTransactionRepository.class,
    DuplicateEventBus.class,
    com.transferer.shared.outbox.OutboxEventPublisher.class,
    com.transferer.TestJacksonConfiguration.class
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentSagaIdempotencyTest extends AbstractPaymentSagaTest {
}