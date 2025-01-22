package com.order.order_service.services.implementations;

import com.order.order_service.dtos.OrderItemDTO;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderItemNotFoundException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import com.order.order_service.repositories.OrderItemRepository;
import com.order.order_service.repositories.OrderRepository;
import com.order.order_service.services.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImplementation implements OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void saveOrderItem(OrderItem orderItem) {
        orderItemRepository.save(orderItem);
    }

    @Override
    public ResponseEntity<OrderItemDTO> createOrderItem(OrderItemDTO orderItemDTO) throws OrderNotFoundException, IllegalAttributeException {
        validateOrderItem(orderItemDTO);

        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(orderItemDTO.getQuantity());
        orderItem.setProductId(orderItemDTO.getProductId());

        Long orderId = orderItemDTO.getOrderId();
        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        orderEntity.addProduct(orderItem);
        orderRepository.save(orderEntity);

        saveOrderItem(orderItem);
        return new ResponseEntity<>(orderItemDTO, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<OrderItemDTO> updateOrderItem(Long id, OrderItemDTO orderItemDTO) throws OrderItemNotFoundException, IllegalAttributeException {
        validateOrderItem(orderItemDTO);

        OrderItem existingOrderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Order not found with ID: " + id));

        existingOrderItem.setQuantity(orderItemDTO.getQuantity());
        existingOrderItem.setProductId(orderItemDTO.getProductId());

        saveOrderItem(existingOrderItem);
        return new ResponseEntity<>(orderItemDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteOrderItem(Long id) throws OrderItemNotFoundException {
        orderItemRepository.findById(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Order item not found with ID: " + id));

        orderItemRepository.deleteById(id);
        return new ResponseEntity<>("Order item deleted!", HttpStatus.OK);
    }

    @Override
    public void validateOrderItem(OrderItemDTO orderItemDTO) throws IllegalAttributeException {
        if (orderItemDTO.getProductId() == null) {
            throw new IllegalAttributeException("Product id cannot be null or empty");
        }

        if (orderItemDTO.getQuantity() == null) {
            throw new IllegalAttributeException("Quantity cannot be null or empty");
        }

        if (orderItemDTO.getOrderId() == null) {
            throw new IllegalAttributeException("Order id cannot be null or empty");
        }

    }
}
