package com.order.order_service.services.implementations;

import com.order.order_service.dtos.OrderDTO;
import com.order.order_service.dtos.OrderItemRecord;
import com.order.order_service.dtos.ProductQuantityRecord;
import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.exceptions.*;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import com.order.order_service.repositories.OrderItemRepository;
import com.order.order_service.repositories.OrderRepository;
import com.order.order_service.services.OrderItemService;
import com.order.order_service.services.OrderService;
import com.order.order_service.services.ProductClientService;
import com.order.order_service.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderItemServiceImplementation implements OrderItemService {


    private static final Logger logger = LoggerFactory.getLogger(OrderItemServiceImplementation.class);

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductClientService productClientService;

    @Override
    public void saveOrderItem(OrderItem orderItem) {
        logger.info("Saving order item: {}", orderItem);
        orderItemRepository.save(orderItem);
    }

    @Override
    public ResponseEntity<Set<OrderItemRecord>> getAllOrderItemsByOrderId(Long userId, Long id) throws OrderException {
        logger.info("Fetching all order items for order ID: {} and user ID: {}", id, userId);

        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Order not found with ID: {}", id);
                    return new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        orderService.validateOrderOwner(userId, id);

        Set<OrderItemRecord> orderItemSet = order.getOrderItemList().stream()
                .map(orderItem -> new OrderItemRecord(orderItem.getId(), orderItem.getProductId(), orderItem.getQuantity()))
                .collect(Collectors.toSet());

        logger.info("Fetched {} items for order ID: {}", orderItemSet.size(), id);
        return new ResponseEntity<>(orderItemSet, HttpStatus.OK);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ResponseEntity<OrderItemRecord> addOrderItem(Long userId, Long orderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException {
        logger.info("Adding item to order ID: {} for user ID: {}", orderId, userId);

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with ID: {}", orderId);
                    return new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        orderService.validateOrderOwner(userId, order.getUserId());
        validOrderStatus(order.getId());
        validateOrderItem(userId, order.getId(), productQuantityRecord.id());

        if (productQuantityRecord.quantity() < 0) {
            logger.error("Invalid quantity: {}", productQuantityRecord.quantity());
            throw new OrderItemException(Constants.INV_QUANTITY);
        }

        List<ProductQuantityRecord> auxList = List.of(productQuantityRecord);
        HashMap<Long, Integer> existentProductMap = productClientService.getExistentProducts(auxList);

        if (existentProductMap.containsKey(productQuantityRecord.id())) {
            Integer realQuantity = existentProductMap.get(productQuantityRecord.id());

            if (realQuantity >= productQuantityRecord.quantity()) {
                OrderItem orderItem = new OrderItem(productQuantityRecord.id(), productQuantityRecord.quantity(), order);
                List<OrderItem> orderItemList = List.of(orderItem);

                try {
                    productClientService.updateProducts(orderItemList, -1);
                } catch (ProductServiceException e) {
                    logger.error("Error updating products: {}", e.getMessage());
                    throw new OrderException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }

                saveOrderItem(orderItem);
                order.addOrderItem(orderItem);
                orderRepository.save(order);

                logger.info("Successfully added item {} to order {}", productQuantityRecord.id(), orderId);
                return new ResponseEntity<>(new OrderItemRecord(order.getId(), orderItem.getProductId(), orderItem.getQuantity()), HttpStatus.CREATED);
            } else {
                logger.warn("Insufficient stock for product ID: {}", productQuantityRecord.id());
                throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_FOUND);
            }
        } else {
            logger.warn("Product not found with ID: {}", productQuantityRecord.id());
            throw new OrderException(Constants.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    private void validateOrderItem(Long userId, Long orderId, Long orderItemProductId) throws OrderException {
        logger.info("Validating order item existence for product ID: {}", orderItemProductId);

        Set<OrderItemRecord> orderItemSet = getAllOrderItemsByOrderId(userId, orderId).getBody();

        for (OrderItemRecord orderItemRecord : orderItemSet) {
            if (Objects.equals(orderItemRecord.productId(), orderItemProductId)) {
                logger.warn("Item already exists in order ID: {}", orderId);
                throw new OrderException(Constants.ITEM_ALREADY_EXISTS);
            }
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ResponseEntity<OrderItemRecord> updateOrderItemQuantity(Long userId, Long orderItemId, Integer quantity) throws OrderItemException, OrderException {
        logger.info("Updating quantity for order item ID: {}", orderItemId);

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> {
                    logger.error("Order item not found with ID: {}", orderItemId);
                    return new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        orderService.validateOrderOwner(userId, orderItem.getOrderEntity().getUserId());
        validOrderStatus(orderItem.getOrderEntity().getId());

        int difference = orderItem.getQuantity() - quantity;

        if (quantity > 0 && !Objects.equals(orderItem.getQuantity(), quantity)) {
            HashMap<Long, Integer> existentProduct = productClientService.getExistentProducts(List.of(new ProductQuantityRecord(orderItem.getProductId(), quantity)));

            try {
                if (difference > 0) {
                    productClientService.updateProducts(List.of(new OrderItem(orderItem.getProductId(), difference, null)), 1);
                } else {
                    if (existentProduct.get(orderItem.getProductId()) >= -1 * difference) {
                        productClientService.updateProducts(List.of(new OrderItem(orderItem.getProductId(), difference, null)), 1);
                    } else {
                        logger.warn("Insufficient stock for product ID: {}", orderItem.getProductId());
                        throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_ACCEPTABLE);
                    }
                }
            } catch (ProductServiceException e) {
                logger.error("Error updating products: {}", e.getMessage());
                throw new OrderException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            orderItem.setQuantity(quantity);
            orderItem = orderItemRepository.save(orderItem);

            logger.info("Successfully updated order item ID: {} to quantity {}", orderItemId, quantity);
            return new ResponseEntity<>(new OrderItemRecord(orderItem.getId(), orderItem.getProductId(), orderItem.getQuantity()), HttpStatus.CREATED);
        } else {
            logger.error("Invalid quantity: {}", quantity);
            throw new OrderException(Constants.INV_QUANTITY, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public ResponseEntity<String> deleteOrderItem(Long userId, Long orderItemId) throws OrderItemException, OrderException {
        logger.info("Deleting order item ID: {}", orderItemId);

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> {
                    logger.error("Order item not found with ID: {}", orderItemId);
                    return new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        orderService.validateOrderOwner(userId, orderItem.getOrderEntity().getUserId());
        validOrderStatus(orderItem.getOrderEntity().getId());

        productClientService.updateProducts(List.of(orderItem), 1);
        orderItemRepository.delete(orderItem);

        logger.info("Successfully deleted order item ID: {}", orderItemId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @Override
    public boolean existsOrderItem(Long id) {
        return orderItemRepository.existsById(id);
    }

    private void validOrderStatus(Long id) throws OrderException {
        OrderDTO order = orderService.getOrderById(id).getBody();

        if (order.getStatus() == OrderStatusEnum.COMPLETED){
            throw new OrderException(Constants.ORDER_COMPLETED,HttpStatus.UNAUTHORIZED);
        }
    }
}
