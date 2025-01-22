package com.order.order_service.services.implementations;

import com.order.order_service.dtos.OrderDTO;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.repositories.OrderRepository;
import com.order.order_service.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImplementation implements OrderService {

    @Autowired
    private OrderRepository orderRepository;


    @Override
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderRepository.findAll()
                .stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Override
    public OrderEntity saveOrder(OrderEntity orderEntity) {
        return orderRepository.save(orderEntity);
    }

    @Override
    public ResponseEntity<OrderDTO> createOrder(OrderDTO orderDTO) throws IllegalAttributeException {
        validateOrder(orderDTO);

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setStatus(orderDTO.getStatus());
        orderEntity.setUserId(orderDTO.getUserId());

        OrderEntity savedOrderEntity = saveOrder(orderEntity);
        return new ResponseEntity<>(new OrderDTO(savedOrderEntity), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<OrderDTO> updateOrder(Long id, OrderDTO orderDTO) throws OrderNotFoundException, IllegalAttributeException {
        validateOrder(orderDTO);

        OrderEntity existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        existingOrder.setStatus(orderDTO.getStatus());
        existingOrder.setUserId(orderDTO.getUserId());

        OrderEntity updatedOrderEntity = saveOrder(existingOrder);
        return new ResponseEntity<>(new OrderDTO(updatedOrderEntity), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteOrder(Long id) throws OrderNotFoundException {
        orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        orderRepository.deleteById(id);
        return new ResponseEntity<>("Order deleted!", HttpStatus.OK);
    }

    @Override
    public void validateOrder(OrderDTO orderDTO) throws IllegalAttributeException {
        if (orderDTO.getUserId() == null) {
            throw new IllegalAttributeException("User id cannot be null or empty");
        }

        if (orderDTO.getStatus() == null) {
            throw new IllegalAttributeException("Status cannot be null or empty");
        }
    }
}
