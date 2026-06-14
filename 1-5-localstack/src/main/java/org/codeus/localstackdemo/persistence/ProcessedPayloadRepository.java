package org.codeus.localstackdemo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPayloadRepository extends JpaRepository<ProcessedPayloadEntity, Integer> {
}
