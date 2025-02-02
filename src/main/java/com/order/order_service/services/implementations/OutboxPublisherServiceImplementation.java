package com.order.order_service.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.order_service.dtos.OrderToPdfDTO;
import com.order.order_service.models.OutboxMessage;
import com.order.order_service.repositories.OutboxMessageRepository;
import com.order.order_service.services.OutboxPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxPublisherServiceImplementation implements OutboxPublisherService {
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisherServiceImplementation.class);
    private static final int MAX_RETRIES = 5;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findByProcessedFalseAndRetryCountLessThan(MAX_RETRIES);

        if (pendingMessages.isEmpty()) {
            logger.debug("No pending outbox messages found.");
            return;
        }

        logger.info("Processing {} pending outbox messages.", pendingMessages.size());
        for (OutboxMessage message : pendingMessages) {
            try {
                OrderToPdfDTO event = objectMapper.readValue(message.getPayload(), OrderToPdfDTO.class);

                if (message.getId() % 2 == 0) {
                    throw new RuntimeException("Simulated send failure");
                }


                rabbitTemplate.convertAndSend(message.getExchange(), message.getRoutingKey(), event);

                message.setProcessed(true);
                outboxMessageRepository.save(message);

                logger.info("Successfully sent outbox message with ID: {}", message.getId());
            } catch (Exception e) {
                message.setRetryCount(message.getRetryCount() + 1);
                outboxMessageRepository.save(message);

                logger.error("Failed to send outbox message with ID: {}. Retry count: {}. Error: {}",
                        message.getId(), message.getRetryCount(), e.getMessage());
            }
        }
    }
}
