package com.order.order_service.services.implementations;

import com.order.order_service.dtos.CreateOrderRecord;
import com.order.order_service.dtos.ExistentProductsRecord;
import com.order.order_service.dtos.OrderDTO;
import com.order.order_service.dtos.ProductQuantityRecord;
import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import com.order.order_service.repositories.OrderItemRepository;
import com.order.order_service.repositories.OrderRepository;
import com.order.order_service.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImplementation implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${USER_SERVICE_URL}")
    private String USER_SERVICE_URL;

    @Value("${PRODUCT_SERVICE_URL}")
    private String PRODUCT_SERVICE_URL;


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
    public ResponseEntity<OrderDTO> createOrder(CreateOrderRecord newOrder) throws IllegalAttributeException {
        String userServiceUrl = USER_SERVICE_URL + "/email/" + newOrder.email();
        try {
            Long userId = restTemplate.getForObject(userServiceUrl, Long.class);

            HttpEntity<List<ProductQuantityRecord>> requestEntity = new HttpEntity<>(newOrder.recordList());
            ResponseEntity<List<ExistentProductsRecord>> responseEntity = restTemplate.exchange(
                    PRODUCT_SERVICE_URL,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            OrderEntity order = new OrderEntity(userId, OrderStatusEnum.PENDING, null);
            orderRepository.save(order);

            List<OrderItem> orderItems = generateOrderItems(responseEntity.getBody(), order);
            order.setProducts(orderItems);
            orderRepository.save(order);

            OrderDTO orderDTO = new OrderDTO(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            System.err.println("Error al comunicarse con el servicio externo: " + ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).build();
        } catch (Exception ex) {
            System.err.println("Error inesperado: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<OrderItem> generateOrderItems(List<ExistentProductsRecord> productRecords, OrderEntity order) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (ExistentProductsRecord record : productRecords) {
            OrderItem orderItem = new OrderItem(record.id(), record.quantity(), order);
            orderItemRepository.save(orderItem);
            orderItems.add(orderItem);
        }
        return orderItems;
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
