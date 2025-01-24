package com.order.order_service.services.implementations;

import com.order.order_service.dtos.*;
import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.enums.ProductErrorEnum;
import com.order.order_service.exceptions.IllegalAttributeException;
import com.order.order_service.exceptions.OrderException;
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
import org.springframework.web.client.*;

import java.util.ArrayList;
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
    public ResponseEntity<OrderCreateWrapperRecord> createOrder(CreateOrderRecord newOrder) throws IllegalAttributeException, OrderException {
        Long userId = getUserIdFromEmail(newOrder.email());
        HttpEntity<List<ProductQuantityRecord>> requestEntity = new HttpEntity<>(newOrder.recordList());

        try {
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

            List<ErrorProductRecord> errorList = generateErrorProductsList(newOrder.recordList(), responseEntity.getBody());

            OrderDTO orderDTO = new OrderDTO(order);
            OrderCreateWrapperRecord orderCreateWrapperRecord = new OrderCreateWrapperRecord(orderDTO, errorList);

            return ResponseEntity.status(HttpStatus.CREATED).body(orderCreateWrapperRecord);

        } catch (HttpClientErrorException e){
            throw new OrderException("Error communicating with product-service", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<ErrorProductRecord> generateErrorProductsList(List<ProductQuantityRecord> userProductsList, List<ExistentProductsRecord> existentProductsList) {
        List<ErrorProductRecord> errorProductsList = new ArrayList<>();

        userProductsList.stream()
                .filter(userProduct ->
                        !existentProductsList.stream().anyMatch(availableProduct ->
                                availableProduct.id().equals(userProduct.id()) && availableProduct.price() != null)
                )
                .forEach(userProduct -> {
                    boolean productExists = existentProductsList.stream()
                            .anyMatch(p -> p.id().equals(userProduct.id()));

                    if (productExists) {
                        errorProductsList.add(new ErrorProductRecord(userProduct.id(), ProductErrorEnum.NO_STOCK));
                    } else {
                        errorProductsList.add(new ErrorProductRecord(userProduct.id(), ProductErrorEnum.NOT_FOUND));
                    }
                });

        return errorProductsList;
    }


    private List<OrderItem> generateOrderItems(List<ExistentProductsRecord> productRecords, OrderEntity order) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (ExistentProductsRecord record : productRecords) {
            if(record.price() != null) {
            OrderItem orderItem = new OrderItem(record.id(), record.quantity(), order);

            orderItemRepository.save(orderItem);
            orderItems.add(orderItem);
            }
        }
        return orderItems;
    }

    private Long getUserIdFromEmail(String email) throws OrderException {
        try{
            String url = USER_SERVICE_URL + "/email/" + email;
            return restTemplate.getForObject(url, Long.class);
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException){
                HttpStatusCodeException aux = (HttpStatusCodeException)e;
                throw new OrderException("Error communicating with product-service", (HttpStatus) aux.getStatusCode());
            }else{
                throw new OrderException("Error communicating with user-service", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }
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
