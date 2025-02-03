package com.order.order_service.dtos;

import com.order.order_service.models.OrderEntity;

public class OrderEntityItemListWrapper {
    OrderEntity orderEntity;
    OrderItemListWrapper orderItemListWrapper;

    public OrderEntity getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(OrderEntity orderEntity) {
        this.orderEntity = orderEntity;
    }

    public OrderItemListWrapper getOrderItemListWrapper() {
        return orderItemListWrapper;
    }

    public void setOrderItemListWrapper(OrderItemListWrapper orderItemListWrapper) {
        this.orderItemListWrapper = orderItemListWrapper;
    }
}
