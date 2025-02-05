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
import com.order.order_service.services.OutboxMessageService;
import com.order.order_service.services.ProductClientService;
import com.order.order_service.utils.Constants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private OutboxMessageService outboxMessageService;

    @Autowired
    private ProductClientService productClientService;

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
                    logger.error(Constants.ORDER_NOT_FOUND_WITH_ID + "{}", id);
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

    private OrderEntityItemListWrapper createOrderEntity(Long userId, HashMap<Long, Integer> existentProductMap, List<ProductQuantityRecord> wantedProducts) throws OrderException {
        OrderEntity order = new OrderEntity(null, userId, OrderStatusEnum.PENDING);
        saveOrder(order);

        OrderItemListWrapper orderItemListWrapper = setOrderItemList(existentProductMap, wantedProducts, order);

        saveOrder(order);

        OrderEntityItemListWrapper orderEntityItemListWrapper = new OrderEntityItemListWrapper();
        orderEntityItemListWrapper.setOrderEntity(order);
        orderEntityItemListWrapper.setOrderItemListWrapper(orderItemListWrapper);

        return orderEntityItemListWrapper;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResponseEntity<OrderCreatedRecord> createOrder(String email, NewOrderRecord newOrder) throws OrderException {
        logger.info("Creating order for user with email: {}", email);
        Long userId = getUserIdFromEmail(email);

        HashMap<Long,Integer> existentProductMap = productClientService.getExistentProducts(newOrder.recordList());

        OrderEntityItemListWrapper orderEntityItemListWrapper = createOrderEntity(userId, existentProductMap, newOrder.recordList());

        OrderEntity order = orderEntityItemListWrapper.getOrderEntity();
        List<OrderItem> orderItemList = orderEntityItemListWrapper.getOrderItemListWrapper().getOrderItemList();
        List<ErrorProductRecord> orderItemsError = orderEntityItemListWrapper.getOrderItemListWrapper().getErrorProductRecordList();

        try {
            productClientService.updateProducts(orderItemList,-1);
        } catch(ProductServiceException e){
            logger.error(Constants.UPDATE_STOCK_ERROR + "{}", e.getMessage());

            compensateOrderCreation(order);
            throw new OrderException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }

        OrderDTO orderDTO = new OrderDTO(order);

        logger.info("Order created successfully: {}", order.getId());
        return new ResponseEntity<>(new OrderCreatedRecord(orderDTO, orderItemsError), HttpStatus.CREATED);

    }


    @Override
    public OrderItemListWrapper setOrderItemList(HashMap<Long, Integer> existentProducts, List<ProductQuantityRecord> wantedProducts, OrderEntity order) {
        logger.info("Setting order items for order {}", order.getId());

        List<OrderItem> orderItemList = new ArrayList<>();
        List<ErrorProductRecord> errorProductList = new ArrayList<>();

        for (ProductQuantityRecord wantedProduct : wantedProducts) {
            if (existentProducts.containsKey(wantedProduct.id())) {
                Integer realQuantity = existentProducts.get(wantedProduct.id());
                if (realQuantity >= wantedProduct.quantity()) {
                    OrderItem orderItem = new OrderItem(wantedProduct.id(), wantedProduct.quantity(), order);
                    orderItemList.add(orderItem);
                } else {
                    errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductErrorEnum.NO_STOCK));
                }
            } else {
                errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductErrorEnum.NOT_FOUND));
            }
        }

        if (!orderItemList.isEmpty()) {
            orderItemList = orderItemRepository.saveAll(orderItemList);
        }

        return new OrderItemListWrapper(orderItemList, errorProductList);
    }


    @Transactional(rollbackFor = Exception.class)
    public void compensateOrderCreation(OrderEntity order) {
        logger.info("Compensating order creation for order id: {}", order.getId());

        try {
            productClientService.updateProducts(order.getOrderItemList(), +1);
            deleteOrder(order.getId());
        } catch (ProductServiceException e) {
            logger.error("Error compensating product update for order {}: {}", order.getId(), e.getMessage());
        } catch (OrderNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long getUserIdFromEmail(String email) throws OrderException {
        try{
            String url = USER_SERVICE_URL + "/private/email/" + email;

            return restTemplate.getForObject(url, Long.class);
        } catch (RestClientException e) {

            if (e instanceof HttpStatusCodeException aux){
                throw new OrderException(Constants.USER_NOT_FOUND, (HttpStatus) aux.getStatusCode());
            } else {
                throw new OrderException(Constants.COM_USR_PROD, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<OrderDTO> changeStatus(Long userId, String userMail, Long orderId, OrderStatusEnum orderStatus) throws OrderException {
        logger.info("Changing status of order {} to {}", orderId, orderStatus);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error(Constants.ORDER_NOT_FOUND_WITH_ID + "{}", orderId);
                    return new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        validateOrderOwner(userId,order.getUserId());

        order.setStatus(orderStatus);

        order = orderRepository.save(order);

        if (order.getStatus() == OrderStatusEnum.COMPLETED){
            OrderToPdfDTO orderToPdfDTO = sendDataToGeneratePdf(order, userMail);
            outboxMessageService.saveOutboxMessage(
                    orderToPdfDTO,
                    "OrderPdfEvent",
                    "email-exchange",
                    "user.pdf"
            );
            logger.info("Outbox message saved for order {}", order.getId());
        }

        return new ResponseEntity<>(new OrderDTO(order), HttpStatus.OK);
    }

    private OrderToPdfDTO sendDataToGeneratePdf(OrderEntity order, String userMail) {
        logger.info("Sending data to generate PDF for order {}", order.getId());
        List<ProductRecord> listProducts = new ArrayList<>();

        for (OrderItem item : order.getOrderItemList()){
            try {
                ProductRecord product = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/public/" + item.getProductId(), ProductRecord.class );

                assert product != null;
                ProductRecord productRecord = new ProductRecord(product.id(), product.name(), product.description(), product.price(), item.getQuantity());
                listProducts.add(productRecord);
            } catch (RestClientException e){
                logger.warn("Failed to fetch product {} details", item.getProductId());
            }
        }

        return new OrderToPdfDTO(order.getId(), order.getUserId(), userMail, listProducts);
    }


    @Override
    public ResponseEntity<String> deleteOrder(Long id) throws OrderNotFoundException {
        logger.info("Attempting to delete order with ID: {}", id);
        orderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error(Constants.ORDER_NOT_FOUND_WITH_ID + "{}", id);
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
