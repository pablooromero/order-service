package com.order.order_service.services;

import com.order.order_service.dtos.ProductQuantityRecord;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.ProductServiceException;
import com.order.order_service.models.OrderItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.HashMap;
import java.util.List;

public interface ProductClientService {
    @CircuitBreaker(name = "productBreaker", fallbackMethod = "getExistentProductsFallback")
    HashMap<Long,Integer> getExistentProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException;

    @CircuitBreaker(name = "productBreaker")
    void updateProducts(List<OrderItem> orderItemList, int factor) throws ProductServiceException;
}
