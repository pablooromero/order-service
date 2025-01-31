package com.order.order_service.dtos;

import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.models.OrderEntity;

import java.util.List;

public class OrderDTO {
    private Long id;
    private Long userId;
    private OrderStatusEnum status;
    private List<OrderItemDTO> orderItems;


    public OrderDTO() {}

    public OrderDTO(OrderEntity orderEntity) {
        id = orderEntity.getId();
        userId = orderEntity.getUserId();
        status = orderEntity.getStatus();
        orderItems = orderEntity.getOrderItemList()
                .stream()
                .map(OrderItemDTO::new)
                .toList();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<OrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }
}
