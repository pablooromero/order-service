package com.order.order_service.dtos;

import com.order.order_service.enums.OrderStatusEnum;

public record UpdateOrderRecord(OrderStatusEnum orderStatus) {
}
