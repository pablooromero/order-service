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
import com.order.order_service.utils.Constants;
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

import java.util.ArrayList;
import java.util.HashMap;
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

    @Autowired
    RabbitTemplate rabbitTemplate;

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
    public OrderDTO getOrderById(Long id) throws OrderException {
        OrderEntity order = orderRepository.findById(id).orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));
        OrderDTO orderDTO = new OrderDTO(order);
        return orderDTO;
    }

    @Override
    public OrderCreateWrapperRecord createOrder(CreateOrderRecord newOrder) throws OrderException {
        Long userId = getUserIdFromEmail(newOrder.email());

        HashMap<Long,Integer> existentProductMap = getExistentProducts(newOrder.recordList());

        OrderEntity order = new OrderEntity(userId, OrderStatusEnum.PENDING, null);

        orderRepository.save(order);

        List<ErrorProductRecord> orderItemsError = setOrderItemList(existentProductMap, newOrder.recordList(), order);

        updateProducts(order.getProducts(),-1);

        orderRepository.save(order);

        OrderDTO orderDTO = new OrderDTO(order);
        OrderCreateWrapperRecord orderCreatedRecord = new OrderCreateWrapperRecord(orderDTO, orderItemsError);

        return orderCreatedRecord;

    }

    @Override
    public HashMap<Long,Integer> getExistentProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException {
        ParameterizedTypeReference<HashMap<Long, Integer>> responseType =
                new ParameterizedTypeReference<>() {};
        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);
        try{
            ResponseEntity<HashMap<Long, Integer>> responseEntity = restTemplate.exchange(PRODUCT_SERVICE_URL, HttpMethod.PUT ,httpEntity, responseType);
            return responseEntity.getBody();
        } catch (RestClientException e) {
            throw new OrderException(Constants.COM_ERR_PROD, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    public List<ErrorProductRecord> setOrderItemList(HashMap<Long, Integer> existentProducts, List<ProductQuantityRecord> wantedProducts, OrderEntity order){
        List<OrderItem> orderItemList = new ArrayList<>();
        List<ErrorProductRecord> errorProductList = new ArrayList<>();
        wantedProducts.forEach(wantedProduct -> {
            if (existentProducts.containsKey(wantedProduct.id())){
                Integer realQuantity = existentProducts.get(wantedProduct.id());
                if (realQuantity>= wantedProduct.quantity()){
                    OrderItem orderItem = new OrderItem(wantedProduct.id(), wantedProduct.quantity(), order);
                    orderItemRepository.save(orderItem);
                    orderItemList.add(orderItem);
                }else{
                    errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductErrorEnum.NO_STOCK));
                }
            }else{
                errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductErrorEnum.NOT_FOUND));
            }
        });

        order.setProducts(orderItemList);

        return errorProductList;
    }

    @Override
    public void updateProducts(List<OrderItem> orderItemList, int factor) throws OrderException {

        List<ProductQuantityRecord> productQuantityRecordList = new ArrayList<>();
        orderItemList.forEach(orderItem -> {
            productQuantityRecordList.add(new ProductQuantityRecord(orderItem.getProductId(), factor*orderItem.getQuantity()));
        });

        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);

        try{
            restTemplate.exchange(PRODUCT_SERVICE_URL + "/to-order", HttpMethod.PUT ,httpEntity, String.class);
        } catch (RestClientException e) {
            throw new OrderException(Constants.COM_ERR_PROD, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    public OrderDTO changeStatus(Long id, OrderStatusEnum orderStatus) throws OrderException {
        OrderEntity order = orderRepository.findById(id).orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));
        order.setStatus(orderStatus);
        order = orderRepository.save(order);
        if (order.getStatus() == OrderStatusEnum.COMPLETED){
            sendDataToGeneratePdf(order);
        }
        return new OrderDTO(order);
    }

    private void sendDataToGeneratePdf(OrderEntity order){
        List<ProductRecord> listProducts = new ArrayList<>();
        for (OrderItem item : order.getProducts()){
            try {
                ProductRecord product = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/" + item.getProductId(), ProductRecord.class );
                listProducts.add(product);
            } catch(RestClientException e){
            }
        }
        OrderToPdfDTO orderToPdfDTO = new OrderToPdfDTO(order.getId(), order.getUserId(), "pabloromerook@gmail.com", listProducts);
        rabbitTemplate.convertAndSend("email-exchange", "user.pdf", orderToPdfDTO);
    }

    private Long getUserIdFromEmail(String email) throws OrderException {
        try{
            String url = USER_SERVICE_URL + "/email/" + email;
            return restTemplate.getForObject(url, Long.class);
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException aux){
                throw new OrderException("Error communicating with product-service", (HttpStatus) aux.getStatusCode());
            }else{
                throw new OrderException("Error communicating with user-service", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }
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
