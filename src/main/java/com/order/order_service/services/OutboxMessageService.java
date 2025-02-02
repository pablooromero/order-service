package com.order.order_service.services;

public interface OutboxMessageService {
    void saveOutboxMessage(Object event, String eventType, String exchange, String routingKey);
}
