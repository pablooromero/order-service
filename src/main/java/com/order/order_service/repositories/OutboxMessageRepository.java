package com.order.order_service.repositories;

import com.order.order_service.models.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findByProcessedFalseAndRetryCountLessThan(int maxRetries);
}
