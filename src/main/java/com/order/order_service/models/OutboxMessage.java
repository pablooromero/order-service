package com.order.order_service.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private String exchange;
    private String routingKey;
    private LocalDateTime createdAt;
    private boolean processed;
    private int retryCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    public OutboxMessage() {
    }

    public OutboxMessage(String eventType, String payload, String exchange, String routingKey,
                         LocalDateTime createdAt, boolean processed, int retryCount) {
        this.eventType = eventType;
        this.payload = payload;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.createdAt = createdAt;
        this.processed = processed;
        this.retryCount = retryCount;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
