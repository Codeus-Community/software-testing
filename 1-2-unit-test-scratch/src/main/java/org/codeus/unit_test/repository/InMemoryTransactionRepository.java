package org.codeus.unit_test.repository;

import org.codeus.unit_test.model.Transaction;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<String, Transaction> storage = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        storage.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) {
        return storage.values().stream()
                .filter(t -> accountId.equals(t.getFromAccountId()) || accountId.equals(t.getToAccountId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByAccountIdAndDateRange(String accountId, LocalDateTime from, LocalDateTime to) {
        return storage.values().stream()
                .filter(t -> accountId.equals(t.getFromAccountId()) || accountId.equals(t.getToAccountId()))
                .filter(t -> !t.getTimestamp().isBefore(from) && !t.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
    }
}