package com.order.order_service.services;

import com.order.order_service.dtos.*;
import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;

public interface OrderService {

    ResponseEntity<List<OrderDTO>> getAllOrders();

    OrderEntity saveOrder(OrderEntity orderEntity);

    OrderDTO getOrderById(Long id) throws OrderException;

    OrderCreateWrapperRecord createOrder(CreateOrderRecord newOrder) throws OrderException;

    HashMap<Long,Integer> getExistentProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException;

    List<ErrorProductRecord> setOrderItemList(HashMap<Long, Integer> existentProducts, List<ProductQuantityRecord> wantedProducts, OrderEntity order);

    void updateProducts(List<OrderItem> orderItemList, int factor) throws OrderException;

    OrderDTO changeStatus(Long id, OrderStatusEnum orderStatus) throws OrderException;

    ResponseEntity<String> deleteOrder(Long id) throws OrderNotFoundException;

    void validateOrder(OrderDTO orderDTO) throws IllegalAttributeException;
}
