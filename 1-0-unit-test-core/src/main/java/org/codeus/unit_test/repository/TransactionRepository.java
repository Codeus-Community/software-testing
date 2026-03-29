package org.codeus.unit_test.repository;

import org.codeus.unit_test.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(String id);
    List<Transaction> findByAccountId(String accountId);
    List<Transaction> findByAccountIdAndDateRange(String accountId, LocalDateTime from, LocalDateTime to);
    List<Transaction> findAll();
}