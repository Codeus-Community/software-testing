package org.codeus.unit_test.repository;

import org.codeus.unit_test.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String id);
    List<Account> findByClientId(String clientId);
    void delete(String id);
    List<Account> findAll();
}