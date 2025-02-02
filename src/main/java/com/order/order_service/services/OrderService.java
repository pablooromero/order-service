package com.order.order_service.services;

import com.order.order_service.dtos.*;
import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.exceptions.ProductServiceException;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface OrderService {
    ResponseEntity<Set<OrderDTO>> getAllOrders();

    ResponseEntity<Set<OrderDTO>> getAllOrdersByUserId(Long id);

    void saveOrder(OrderEntity orderEntity);

    ResponseEntity<OrderDTO> getOrderById(Long id) throws OrderException;

    ResponseEntity<OrderDTO> getOrderByUserId(Long userId, Long orderId) throws OrderException;

    ResponseEntity<OrderCreatedRecord> createOrder(String email, NewOrderRecord newOrder) throws OrderException;

    HashMap<Long,Integer> getExistentProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException;

    OrderItemListWrapper setOrderItemList(HashMap<Long, Integer> existentProducts, List<ProductQuantityRecord> wantedProducts, OrderEntity order);

    void updateProducts(List<OrderItem> orderItemList, int factor) throws ProductServiceException;

    Long getUserIdFromEmail(String email) throws OrderException;

    ResponseEntity<OrderDTO> changeStatus(Long userId, String userMail, Long orderId, OrderStatusEnum orderStatus) throws OrderException;

    ResponseEntity<String> deleteOrder(Long id) throws OrderNotFoundException;

    ResponseEntity<String> deleteOrderUser(Long userId, Long orderId) throws OrderNotFoundException, OrderException;

    boolean existsOrder(Long id);

    void validateOrderOwner(Long userId, Long orderId) throws OrderException;
}
