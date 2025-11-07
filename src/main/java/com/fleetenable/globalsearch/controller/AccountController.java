package com.fleetenable.globalsearch.controller;

import com.fleetenable.globalsearch.model.Account;
import com.fleetenable.globalsearch.repository.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Account REST Controller
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management APIs")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountRepository accountRepository;

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieve all active accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountRepository.findAllActive();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID", description = "Retrieve a specific account by ID")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get account by account number", description = "Retrieve account by account number")
    public ResponseEntity<Account> getAccountByNumber(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search accounts with fuzzy matching", description = "Search accounts using pg_trgm")
    public ResponseEntity<List<Account>> searchAccounts(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<Account> accounts = accountRepository.searchFuzzy(q, threshold, limit);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/search/paginated")
    @Operation(summary = "Search accounts with pagination", description = "Paginated fuzzy search")
    public ResponseEntity<Page<Account>> searchAccountsPaginated(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("relevance").descending());
        Page<Account> accounts = accountRepository.searchFuzzyPaginated(q, threshold, pageable);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    @Operation(summary = "Create account", description = "Create a new account")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        Account saved = accountRepository.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account", description = "Update an existing account")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        if (!accountRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        account.setId(id);
        Account updated = accountRepository.save(account);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete account", description = "Soft delete an account")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        return accountRepository.findById(id)
            .map(account -> {
                account.setDeletedAt(java.time.Instant.now());
                accountRepository.save(account);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
