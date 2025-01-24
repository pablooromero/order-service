package com.order.order_service.models;

import jakarta.persistence.*;

@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private Integer quantity;

    public OrderItem(Long productId, Integer quantity, OrderEntity order) {
        this.productId = productId;
        this.quantity = quantity;
        this.order = order;
    }

    @ManyToOne
    @JoinColumn(name = "order_entity_id")
    private OrderEntity order;

    public OrderItem() {

    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }
}
