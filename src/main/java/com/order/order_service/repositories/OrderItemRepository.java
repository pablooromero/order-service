package com.order.order_service.repositories;

import com.order.order_service.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteById(Long id);
}
