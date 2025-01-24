package com.order.order_service.services;

import com.order.order_service.dtos.CreateOrderRecord;
import com.order.order_service.dtos.OrderDTO;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface OrderService {

    ResponseEntity<List<OrderDTO>> getAllOrders();

    OrderEntity saveOrder(OrderEntity orderEntity);

    ResponseEntity<OrderDTO> createOrder(CreateOrderRecord createOrderRecord) throws IllegalAttributeException;

    ResponseEntity<OrderDTO> updateOrder(Long id, OrderDTO orderDTO) throws OrderNotFoundException, IllegalArgumentException;

    ResponseEntity<String> deleteOrder(Long id) throws OrderNotFoundException;

    void validateOrder(OrderDTO orderDTO) throws IllegalAttributeException;
}
