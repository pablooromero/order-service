package com.order.order_service.dtos;

import com.order.order_service.models.OrderItem;

public class OrderItemDTO {

    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;

    public OrderItemDTO() {}

    public OrderItemDTO(OrderItem orderItem) {
        id = orderItem.getId();
        orderId = orderItem.getOrder().getId();
        productId = orderItem.getProductId();
        quantity = orderItem.getQuantity();
    }

    public OrderItemDTO(Long id, Long productId, Integer quantity) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
}
