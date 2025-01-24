package com.order.order_service.models;

import com.order.order_service.enums.OrderStatusEnum;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated
    private OrderStatusEnum status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> products;

    public OrderEntity(Long userId, OrderStatusEnum status, List<OrderItem> products) {
        this.userId = userId;
        this.status = status;
        this.products = products;
    }

    public OrderEntity() {}

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OrderStatusEnum getStatus() {
        return status;
    }

    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }

    public List<OrderItem> getProducts() {
        return products;
    }

    public void setProducts(List<OrderItem> products) {
        this.products = products;
    }

    public void addProduct(OrderItem orderItem) {
        orderItem.setOrder(this);
        products.add(orderItem);
    }
}