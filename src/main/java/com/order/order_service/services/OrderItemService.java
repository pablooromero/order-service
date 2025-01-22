package com.order.order_service.services;

import com.order.order_service.dtos.OrderItemDTO;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderItem;
import org.springframework.http.ResponseEntity;

public interface OrderItemService {
    void saveOrderItem(OrderItem orderItem);

    ResponseEntity<OrderItemDTO> createOrderItem(OrderItemDTO orderItemDTO) throws OrderNotFoundException;

    ResponseEntity<OrderItemDTO> updateOrderItem(Long id, OrderItemDTO orderItemDTO);

    ResponseEntity<String> deleteOrderItem(Long id) throws OrderNotFoundException;

    void validateOrderItem(OrderItemDTO orderItemDTO) throws IllegalAttributeException;
}
