package com.transferer.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.events.EventBus;
import com.transferer.shared.outbox.OutboxEventBus;
import com.transferer.shared.outbox.OutboxEventRepository;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
    com.transferer.shared.outbox.OutboxEventPublisher.class,
    com.transferer.TestJacksonConfiguration.class,
    PaymentSagaIdempotencyTest.TestConfiguration.class
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentSagaIdempotencyTest extends AbstractPaymentSagaTest {
    
    static class TestConfiguration {
        @Bean
        public OutboxEventBus outboxEventBus(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
            return new OutboxEventBus(outboxEventRepository, objectMapper);
        }
        
        @Bean("duplicateEventBus")
        @Primary
        public EventBus duplicateEventBus(OutboxEventBus outboxEventBus) {
            return new DuplicateEventPublisher(outboxEventBus);
        }
    }
}