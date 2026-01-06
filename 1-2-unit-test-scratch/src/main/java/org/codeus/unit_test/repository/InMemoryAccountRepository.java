package org.codeus.unit_test.repository;

import org.codeus.unit_test.model.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of AccountRepository.
 * <p>
 * NOTE: Imagine this is a REAL DATABASE.
 * In tests, this should be treated as an external dependency
 * and mocked/stubbed appropriately to maintain test isolation.
 */
public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> storage = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        storage.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Account> findByClientId(String clientId) {
        return storage.values().stream()
                .filter(account -> account.getClientId().equals(clientId))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        storage.remove(id);
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
    }
}