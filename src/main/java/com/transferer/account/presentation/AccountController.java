package com.transferer.account.presentation;

import com.transferer.account.application.AccountService;
import com.transferer.account.application.dto.*;
import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public Mono<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return accountService.openAccount(request.getHolderName(), request.getInitialBalance())
                .map(AccountResponse::new);
    }

    @GetMapping("/{id}")
    public Mono<AccountResponse> getAccount(@PathVariable String id) {
        AccountId accountId = AccountId.of(id);
        return accountService.getAccount(accountId)
                .map(AccountResponse::new);
    }

    @GetMapping("/{id}/balance")
    public Mono<BalanceResponse> getAccountBalance(@PathVariable String id) {
        AccountId accountId = AccountId.of(id);
        return accountService.getAccountBalance(accountId)
                .map(balance -> new BalanceResponse(id, balance));
    }

    @PutMapping("/{id}/balance")
    public Mono<AccountResponse> updateBalance(
            @PathVariable String id,
            @Valid @RequestBody UpdateBalanceRequest request) {
        
        AccountId accountId = AccountId.of(id);
        
        if (request.getOperation() == UpdateBalanceRequest.BalanceOperation.CREDIT) {
            return accountService.creditAccount(accountId, request.getAmount())
                    .map(AccountResponse::new);
        } else {
            return accountService.debitAccount(accountId, request.getAmount())
                    .map(AccountResponse::new);
        }
    }

    @PutMapping("/{id}/suspend")
    public Mono<AccountResponse> suspendAccount(@PathVariable String id) {
        AccountId accountId = AccountId.of(id);
        return accountService.suspendAccount(accountId)
                .map(AccountResponse::new);
    }

    @PutMapping("/{id}/activate")
    public Mono<AccountResponse> activateAccount(@PathVariable String id) {
        AccountId accountId = AccountId.of(id);
        return accountService.activateAccount(accountId)
                .map(AccountResponse::new);
    }

    @PutMapping("/{id}/deactivate")
    public Mono<AccountResponse> deactivateAccount(@PathVariable String id) {
        AccountId accountId = AccountId.of(id);
        return accountService.deactivateAccount(accountId)
                .map(AccountResponse::new);
    }
}