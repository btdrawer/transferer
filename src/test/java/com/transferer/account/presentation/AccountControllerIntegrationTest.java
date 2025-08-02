package com.transferer.account.presentation;

import com.transferer.account.application.dto.OpenAccountRequest;
import com.transferer.account.application.dto.UpdateBalanceRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///test",
    "spring.r2dbc.username=sa",
    "spring.r2dbc.password=password"
})
class AccountControllerIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void openAccount_WithValidRequest_ShouldCreateAccount() {
        OpenAccountRequest request = new OpenAccountRequest("John Doe", BigDecimal.valueOf(1000));

        webTestClient.post()
                .uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.holderName").isEqualTo("John Doe")
                .jsonPath("$.balance").isEqualTo(1000)
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void openAccount_WithInvalidRequest_ShouldReturnBadRequest() {
        OpenAccountRequest request = new OpenAccountRequest("", BigDecimal.valueOf(-100));

        webTestClient.post()
                .uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.holderName").exists()
                .jsonPath("$.fieldErrors.initialBalance").exists();
    }

    @Test
    void getAccount_WithExistingId_ShouldReturnAccount() {
        OpenAccountRequest openRequest = new OpenAccountRequest("Jane Doe", BigDecimal.valueOf(2000));

        String accountId = new String(
                webTestClient.post()
                .uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(openRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists()
                .returnResult()
                .getResponseBodyContent(),
                StandardCharsets.UTF_8
        );

        webTestClient.get()
                .uri("/api/v1/accounts/{id}", "test-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.holderName").isEqualTo("Jane Doe")
                .jsonPath("$.balance").isEqualTo(2000);
    }

    @Test
    void updateBalance_WithCreditOperation_ShouldIncreaseBalance() {
        OpenAccountRequest openRequest = new OpenAccountRequest("Bob Smith", BigDecimal.valueOf(500));
        
        String accountId = webTestClient.post()
                .uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(openRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        UpdateBalanceRequest updateRequest = new UpdateBalanceRequest(
                BigDecimal.valueOf(300), 
                UpdateBalanceRequest.BalanceOperation.CREDIT
        );

        webTestClient.put()
                .uri("/api/v1/accounts/{id}/balance", "test-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(800);
    }

    @Test
    void getAccountBalance_WithExistingAccount_ShouldReturnBalance() {
        OpenAccountRequest openRequest = new OpenAccountRequest("Alice Johnson", BigDecimal.valueOf(1500));
        
        String accountId = webTestClient.post()
                .uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(openRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        webTestClient.get()
                .uri("/api/v1/accounts/{id}/balance", "test-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1500);
    }
}