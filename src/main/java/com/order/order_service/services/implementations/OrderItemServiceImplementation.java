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
import com.order.order_service.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderItemServiceImplementation implements OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Override
    public void saveOrderItem(OrderItem orderItem) {
        orderItemRepository.save(orderItem);
    }

    @Override
    public ResponseEntity<Set<OrderItemRecord>> getAllOrderItemsByOrderId(Long userId, Long id) throws OrderException {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));
        orderService.validateOrderOwner(userId, id);
        Set<OrderItemRecord> orderItemSet = order.getOrderItemList().stream().map(orderItem -> new OrderItemRecord(orderItem.getId(), orderItem.getProductId(), orderItem.getQuantity()))
                .collect(Collectors.toSet());

        return new ResponseEntity<>(orderItemSet, HttpStatus.OK);
    }


    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ResponseEntity<OrderItemRecord> addOrderItem(Long userId, Long OrderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException {
        OrderEntity order = orderRepository.findById(OrderId).orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));

        orderService.validateOrderOwner(userId, order.getUserId());
        validOrderStatus(order.getId());
        validateOrderItem(userId, order.getId(),productQuantityRecord.id());

        if (productQuantityRecord.quantity()<0){
            throw new OrderItemException(Constants.INV_QUANTITY);
        }

        List<ProductQuantityRecord> auxList = new ArrayList<>();
        auxList.add(productQuantityRecord);

        HashMap<Long, Integer> existentProductMap = orderService.getExistentProducts(auxList);

        if (existentProductMap.containsKey(productQuantityRecord.id())){
            Integer realQuantity = existentProductMap.get(productQuantityRecord.id());
            if (realQuantity>= productQuantityRecord.quantity()){

                OrderItem orderItem = new OrderItem(productQuantityRecord.id(), productQuantityRecord.quantity(), order);

                List<OrderItem> orderItemList = new ArrayList<>();
                orderItemList.add(orderItem);

                try {
                    orderService.updateProducts(orderItemList,-1);
                } catch(ProductServiceException e){
                    throw new OrderException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
                }

                saveOrderItem(orderItem);
                order.addOrderItem(orderItem);
                orderRepository.save(order);

                return new ResponseEntity<>(new OrderItemRecord(order.getId(), orderItem.getProductId(), orderItem.getQuantity()), HttpStatus.CREATED);
            } else {
                throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_FOUND);
            }
        } else {
            throw new OrderException(Constants.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    private void validateOrderItem(Long userId, Long orderId, Long orderItemProductId) throws OrderException {
        Set<OrderItemRecord> orderItemSet = getAllOrderItemsByOrderId(userId, orderId).getBody();

        for (OrderItemRecord orderItemRecord : orderItemSet) {
            if (Objects.equals(orderItemRecord.productId(), orderItemProductId)) {
                throw new OrderException(Constants.ITEM_ALREADY_EXISTS);
            }
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public ResponseEntity<OrderItemRecord> updateOrderItemQuantity(Long userId, Long orderItemId, Integer quantity) throws OrderItemException, OrderException {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));

        orderService.validateOrderOwner(userId, orderItem.getOrderEntity().getUserId());
        validOrderStatus(orderItem.getOrderEntity().getId());

        int difference = orderItem.getQuantity() - quantity;

        if (quantity>0 && !Objects.equals(orderItem.getQuantity(), quantity)){
            HashMap<Long, Integer> existentProduct = orderService.getExistentProducts(List.of(new ProductQuantityRecord(orderItem.getProductId(), quantity)));

            try {
                if (difference>0) {
                    orderService.updateProducts(List.of(new OrderItem(orderItem.getProductId(), difference, null)), 1);
                } else {
                    if(existentProduct.get(orderItem.getProductId())>= -1*difference){
                        orderService.updateProducts(List.of(new OrderItem(orderItem.getProductId(), difference, null)), 1);
                    } else {
                        throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_ACCEPTABLE);
                    }
                }
            }catch (ProductServiceException e){
                throw new OrderException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            orderItem.setQuantity(quantity);
            orderItem = orderItemRepository.save(orderItem);
            return new ResponseEntity<>(new OrderItemRecord(orderItem.getId(),orderItem.getProductId(),orderItem.getQuantity()), HttpStatus.CREATED);

        }else{
            throw new OrderException(Constants.INV_QUANTITY, HttpStatus.NOT_ACCEPTABLE);
        }

    }

    @Override
    public ResponseEntity<String> deleteOrderItem(Long userId, Long orderItemId) throws OrderItemException, OrderException {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(()->new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));

        orderService.validateOrderOwner(userId, orderItem.getOrderEntity().getUserId());

        validOrderStatus(orderItem.getOrderEntity().getId());

        List<OrderItem> orderItemList = new ArrayList<>();
        orderItemList.add(orderItem);

        orderService.updateProducts(orderItemList, 1);

        orderItemRepository.delete(orderItem);
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
