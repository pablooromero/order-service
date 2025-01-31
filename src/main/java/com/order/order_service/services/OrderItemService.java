package com.order.order_service.services;


import com.order.order_service.dtos.OrderItemRecord;
import com.order.order_service.dtos.ProductQuantityRecord;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.OrderItemException;
import com.order.order_service.models.OrderItem;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

public interface OrderItemService {

    void saveOrderItem(OrderItem orderItem);

    ResponseEntity<Set<OrderItemRecord>> getAllOrderItemsByOrderId(Long userId, Long id) throws OrderException;

    @Transactional(rollbackFor = {Exception.class})
    ResponseEntity<OrderItemRecord> addOrderItem(Long userId, Long OrderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException;

    @Transactional(rollbackFor = {Exception.class})
    ResponseEntity<OrderItemRecord> updateOrderItemQuantity(Long userId, Long orderItemId, Integer quantity) throws OrderItemException, OrderException;

    ResponseEntity<String> deleteOrderItem(Long userId, Long orderItemId) throws OrderItemException, OrderException;

    boolean existsOrderItem(Long id);
}
