package com.order.order_service.services.implementations;

import com.order.order_service.dtos.*;
import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.enums.ProductErrorEnum;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.exceptions.ProductServiceException;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import com.order.order_service.repositories.OrderItemRepository;
import com.order.order_service.repositories.OrderRepository;
import com.order.order_service.services.OrderService;
import com.order.order_service.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImplementation implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImplementation.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${USER_SERVICE_URL}")
    private String USER_SERVICE_URL;

    @Value("${PRODUCT_SERVICE_URL}")
    private String PRODUCT_SERVICE_URL;

    @Override
    public void saveOrder(OrderEntity orderEntity) {
        logger.info("Saving order: {}", orderEntity);
        orderRepository.save(orderEntity);
    }

    @Override
    public ResponseEntity<Set<OrderDTO>> getAllOrders() {
        logger.info("Fetching all orders...");
        Set<OrderDTO> orders = orderRepository.findAll()
                .stream()
                .map(OrderDTO::new)
                .collect(Collectors.toSet());
        logger.info("Found {} orders", orders.size());
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Set<OrderDTO>> getAllOrdersByUserId(Long id) {
        logger.info("Fetching orders for user ID: {}", id);
        Set<OrderDTO> orders = orderRepository.findByUserId(id)
                .stream()
                .map(OrderDTO::new)
                .collect(Collectors.toSet());

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<OrderDTO> getOrderById(Long id) throws OrderException {
        logger.info("Fetching order with ID: {}", id);
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Order with ID {} not found", id);
                    return new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        return new ResponseEntity<>(new OrderDTO(order), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<OrderDTO> getOrderByUserId(Long userId, Long orderId) throws OrderException {
        logger.info("Fetching order {} for user {}", orderId, userId);
        OrderDTO order = getOrderById(orderId).getBody();
        validateOrderOwner(userId, order.getUserId());
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<OrderCreatedRecord> createOrder(String email, NewOrderRecord newOrder) throws OrderException {
        logger.info("Creating order for user with email: {}", email);
        Long userId = getUserIdFromEmail(email);

        HashMap<Long,Integer> existentProductMap = getExistentProducts(newOrder.recordList());

        OrderEntity order = new OrderEntity(null, userId, OrderStatusEnum.PENDING);

        saveOrder(order);

        List<ErrorProductRecord> orderItemsError = setOrderItemList(existentProductMap, newOrder.recordList(), order);

        try {
            updateProducts(order.getOrderItemList(),-1);
        } catch(ProductServiceException e){
            logger.error("Error updating product stock: {}", e.getMessage());
            throw new OrderException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }

        OrderDTO orderDTO = new OrderDTO(order);

        logger.info("Order created successfully: {}", order.getId());
        return new ResponseEntity<>(new OrderCreatedRecord(orderDTO, orderItemsError), HttpStatus.CREATED);

    }

    @Override
    public HashMap<Long,Integer> getExistentProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException {
        logger.info("Checking product availability for: {}", productQuantityRecordList);
        ParameterizedTypeReference<HashMap<Long, Integer>> responseType =
                new ParameterizedTypeReference<>() {};

        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);

        try{
            ResponseEntity<HashMap<Long, Integer>> responseEntity = restTemplate.exchange(PRODUCT_SERVICE_URL + "/private", HttpMethod.PUT, httpEntity, responseType);
            System.out.println(PRODUCT_SERVICE_URL + "/private");
            return responseEntity.getBody();
        } catch(RestClientException e) {
            logger.error("Error communicating with Product Service: {}", e.getMessage());
            throw new OrderException(Constants.COM_ERR_PROD, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<ErrorProductRecord> setOrderItemList(HashMap<Long, Integer> existentProducts, List<ProductQuantityRecord> wantedProducts, OrderEntity order){
        logger.info("Setting order items for order {}", order.getId());

        List<OrderItem> orderItemList = new ArrayList<>();
        List<ErrorProductRecord> errorProductList = new ArrayList<>();

        wantedProducts.forEach(wantedProduct -> {
            if (existentProducts.containsKey(wantedProduct.id())) {
                Integer realQuantity = existentProducts.get(wantedProduct.id());

                if (realQuantity>= wantedProduct.quantity()) {
                    OrderItem orderItem = new OrderItem(wantedProduct.id(), wantedProduct.quantity(), order);
                    orderItemRepository.save(orderItem);
                    orderItemList.add(orderItem);
                } else {
                    errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductErrorEnum.NO_STOCK));
                }

            } else {
                errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductErrorEnum.NOT_FOUND));
            }
        });

        order.setOrderItemList(orderItemList);
        saveOrder(order);
        logger.info("Order {} updated with {} items and {} errors", order.getId(), orderItemList.size(), errorProductList.size());
        return errorProductList;
    }

    @Override
    public void updateProducts(List<OrderItem> orderItemList, int factor) throws ProductServiceException {
        logger.info("Updating product stock with factor: {}", factor);
        List<ProductQuantityRecord> productQuantityRecordList = new ArrayList<>();

        orderItemList.forEach(orderItem -> {
            productQuantityRecordList.add(new ProductQuantityRecord(orderItem.getProductId(), factor*orderItem.getQuantity()));
        });

        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);

        try {
            restTemplate.exchange(PRODUCT_SERVICE_URL + "/private/to-order", HttpMethod.PUT ,httpEntity, String.class);
        } catch(RestClientException e) {
            logger.error("Error updating product stock: {}", e.getMessage());
            throw new ProductServiceException(Constants.COM_ERR_PROD);
        }
    }

    @Override
    public Long getUserIdFromEmail(String email) throws OrderException {
        try{
            String url = USER_SERVICE_URL + "/private/email/" + email;

            return restTemplate.getForObject(url, Long.class);
        } catch (RestClientException e) {

            if (e instanceof HttpStatusCodeException){
                HttpStatusCodeException aux = (HttpStatusCodeException)e;
                throw new OrderException(Constants.USER_NOT_FOUND, (HttpStatus) aux.getStatusCode());
            } else {
                throw new OrderException(Constants.COM_USR_PROD, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    public ResponseEntity<OrderDTO> changeStatus(Long userId, String userMail, Long orderId, OrderStatusEnum orderStatus) throws OrderException {
        logger.info("Changing status of order {} to {}", orderId, orderStatus);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found: {}", orderId);
                    return new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        validateOrderOwner(userId,order.getUserId());

        order.setStatus(orderStatus);

        order = orderRepository.save(order);

        if (order.getStatus() == OrderStatusEnum.COMPLETED){
            sendDataToGeneratePdf(order, userMail);
        }

        return new ResponseEntity<>(new OrderDTO(order), HttpStatus.OK);
    }

    private void sendDataToGeneratePdf(OrderEntity order, String userMail) {
        logger.info("Sending data to generate PDF for order {}", order.getId());
        List<ProductRecord> listProducts = new ArrayList<>();

        for (OrderItem item : order.getOrderItemList()){
            try {
                ProductRecord product = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/" + item.getProductId(), ProductRecord.class );
                listProducts.add(product);
            } catch (RestClientException e){
                logger.warn("Failed to fetch product {} details", item.getProductId());
            }
        }

        OrderToPdfDTO orderToPdfDTO = new OrderToPdfDTO(order.getId(), order.getUserId(), userMail, listProducts);
        rabbitTemplate.convertAndSend("email-exchange", "user.pdf", orderToPdfDTO);
    }


    @Override
    public ResponseEntity<String> deleteOrder(Long id) throws OrderNotFoundException {
        logger.info("Attempting to delete order with ID: {}", id);
        orderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Order with ID {} not found", id);
                    return new OrderNotFoundException(Constants.ORDER_NOT_FOUND + id);
                });

        orderRepository.deleteById(id);
        logger.info("Order with ID {} deleted successfully", id);

        return new ResponseEntity<>(Constants.ORDER_DELETED, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteOrderUser(Long userId, Long orderId) throws OrderNotFoundException, OrderException {
        logger.info("Attempting to delete order {} for user {}", orderId, userId);
        OrderDTO order = getOrderById(orderId).getBody();

        validateOrderOwner(userId, order.getUserId());

        logger.info("Order {} deleted successfully for user {}", orderId, userId);
        return deleteOrder(orderId);
    }

    @Override
    public boolean existsOrder(Long id) {
        logger.info("Checking existence of order {}", id);
        return orderRepository.existsById(id);
    }

    @Override
    public void validateOrderOwner(Long userId, Long orderId) throws OrderException {
        logger.info("Validating order ownership: user {} and order {}", userId, orderId);
        if (!Objects.equals(userId, orderId)){
            logger.error("User {} is not the owner of order {}", userId, orderId);
            throw new OrderException(Constants.NOT_PERM, HttpStatus.UNAUTHORIZED);
        }

        logger.info("User {} successfully validated as owner of order {}", userId, orderId);
    }
}
