package com.order.order_service.services;

import com.order.order_service.dtos.OrderItemDTO;
import com.order.order_service.dtos.ProductQuantityRecord;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.OrderItemException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderItem;
import org.springframework.http.ResponseEntity;

import java.util.Set;

public interface OrderItemService {
    void saveOrderItem(OrderItem orderItem);

    Set<OrderItemDTO> getAllOrderItemsByOrderId(Long id) throws OrderException;

    OrderItemDTO addOrderItem(Long OrderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException;

    OrderItemDTO updateOrderItemQuantity(Long id, Integer quantity) throws OrderItemException, OrderException;

    ResponseEntity<String> deleteOrderItem(Long id) throws OrderNotFoundException;

    boolean existsOrderItem(Long id);

    void validateOrderItem(OrderItemDTO orderItemDTO) throws IllegalAttributeException;
}
