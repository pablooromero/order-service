package com.order.order_service.services.implementations;

import com.order.order_service.dtos.OrderDTO;
import com.order.order_service.dtos.OrderItemDTO;
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
    public Set<OrderItemDTO> getAllOrderItemsByOrderId(Long id) throws OrderException {
        OrderEntity order = orderRepository.findById(id).orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));
        Set<OrderItemDTO> orderItemSet = order.getProducts().stream().map(orderItem -> new OrderItemDTO(orderItem.getId(),orderItem.getProductId(),orderItem.getQuantity())).collect(Collectors.toSet());
        return orderItemSet;
    }

    @Override
    public OrderItemDTO addOrderItem(Long OrderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException {
        OrderEntity order = orderRepository.findById(OrderId).orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));
        validOrderStatus(order.getId());
        validateOrderItemExists(order.getId(),productQuantityRecord.id());
        if (productQuantityRecord.quantity()==null || productQuantityRecord.quantity()<0){
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
                orderService.updateProducts(orderItemList,-1);

                orderItemRepository.save(orderItem);
                order.addProduct(orderItem);
                orderRepository.save(order);

                return new OrderItemDTO(order.getId(), orderItem.getProductId(), orderItem.getQuantity());
            }else{
                throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_FOUND);
            }
        }else{
            throw new OrderException(Constants.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

    }

    @Override
    public OrderItemDTO updateOrderItemQuantity(Long id, Integer quantity) throws OrderItemException, OrderException {
        OrderItem orderItem = orderItemRepository.findById(id).orElseThrow(()->new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));
        validOrderStatus(orderItem.getOrder().getId());

        if (quantity>0 && !Objects.equals(orderItem.getQuantity(), quantity)){
            HashMap<Long, Integer> existentProduct = orderService.getExistentProducts(List.of(new ProductQuantityRecord(id,quantity)));

            if (existentProduct.get(id)>=quantity){
                int difference = orderItem.getQuantity()-quantity;
                orderService.updateProducts(List.of(new OrderItem(id, difference, null)), 1);
                orderItem.setQuantity(quantity);
                orderItem = orderItemRepository.save(orderItem);
                return new OrderItemDTO(orderItem.getId(),orderItem.getProductId(),orderItem.getQuantity());
            }else{
                throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_ACCEPTABLE);
            }
        }else{
            throw new OrderException(Constants.INV_QUANTITY, HttpStatus.NOT_ACCEPTABLE);
        }

    }

    @Override
    public ResponseEntity<String> deleteOrderItem(Long id) throws OrderItemNotFoundException {
        orderItemRepository.findById(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Order item not found with ID: " + id));

        orderItemRepository.deleteById(id);
        return new ResponseEntity<>("Order item deleted!", HttpStatus.OK);
    }

    private void validateOrderItemExists(Long orderId,Long orderItemProductId) throws OrderException {
        Set<OrderItemDTO> orderItemSet = getAllOrderItemsByOrderId(orderId);
        for (OrderItemDTO orderItemDTO : orderItemSet) {
            if (Objects.equals(orderItemDTO.getProductId(), orderItemProductId)) {
                throw new OrderException(Constants.ITEM_ALREADY_EXISTS);
            }
        }
    }

    @Override
    public boolean existsOrderItem(Long id) {
        return orderItemRepository.existsById(id);
    }

    private void validOrderStatus(Long id) throws OrderException {
        OrderDTO order = orderService.getOrderById(id);
        if (order.getStatus()== OrderStatusEnum.COMPLETED){
            throw new OrderException(Constants.ORDER_COMPLETED,HttpStatus.UNAUTHORIZED);
        }
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
