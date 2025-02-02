package com.order.order_service.models;

import com.order.order_service.enums.OrderStatusEnum;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated
    private OrderStatusEnum status;

    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL)
    private List<OrderItem> orderItemList;

    public OrderEntity(List<OrderItem> orderItemList, Long userId, OrderStatusEnum status) {
        this.orderItemList = new ArrayList<>();
        this.userId = userId;
        this.status = status;
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

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> products) {
        this.orderItemList = orderItemList;
    }

    public void addOrderItemList(OrderItem orderItem) {
        orderItem.setOrderEntity(this);
        orderItemList.add(orderItem);
    }

    public void addOrderItem(OrderItem orderItem){
        orderItemList.add(orderItem);
        orderItem.setOrderEntity(this);
    }
}