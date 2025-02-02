package com.order.order_service.services.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.order_service.models.OutboxMessage;
import com.order.order_service.repositories.OutboxMessageRepository;
import com.order.order_service.services.OutboxMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OutboxMessageServiceImplementation implements OutboxMessageService {

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void saveOutboxMessage(Object event, String eventType, String exchange, String routingKey) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxMessage outboxMessage = new OutboxMessage(
                    eventType,
                    payload,
                    exchange,
                    routingKey,
                    LocalDateTime.now(),
                    false,
                    0
            );
            outboxMessageRepository.save(outboxMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing event payload", e);
        }
    }
}
