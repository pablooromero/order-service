package com.order.order_service.services.implementations;

import com.order.order_service.dtos.ProductQuantityRecord;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.ProductServiceException;
import com.order.order_service.models.OrderItem;
import com.order.order_service.services.ProductClientService;
import com.order.order_service.utils.Constants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ProductClientServiceImplementation implements ProductClientService {

    private static final Logger logger = LoggerFactory.getLogger(ProductClientService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${PRODUCT_SERVICE_URL}")
    private String PRODUCT_SERVICE_URL;

    private Integer attempt = 0;

    @CircuitBreaker(name = "productBreaker", fallbackMethod = "getExistentProductsFallback")
    @Retry(name = "productRetry", fallbackMethod = "getExistentProductsFallback")
    @RateLimiter(name = "productRateLimiter", fallbackMethod = "getExistentProductsFallback")
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
            logger.error(Constants.COM_ERR_PROD + "{}", e.getMessage());
            throw new ProductServiceException(Constants.COM_ERR_PROD);
        }
    }

    public HashMap<Long, Integer> getExistentProductsFallback(List<ProductQuantityRecord> productQuantityRecordList, Throwable t) {
        System.out.println("Attemps Existent Products: " + attempt);
        attempt++;
        logger.error("Fallback triggered for getExistentProducts: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded or service unavailable. Please try again later.");
    }


    @CircuitBreaker(name = "productBreaker", fallbackMethod = "updateProductsFallback")
    @Retry(name = "productRetry", fallbackMethod = "updateProductsFallback")
    @RateLimiter(name = "productRateLimiter", fallbackMethod = "updateProductsFallback")
    @Override
    public void updateProducts(List<OrderItem> orderItemList, int factor) throws ProductServiceException {
        logger.info("Updating product stock with factor: {}", factor);
        List<ProductQuantityRecord> productQuantityRecordList = new ArrayList<>();

        orderItemList.forEach(orderItem -> {
            productQuantityRecordList.add(new ProductQuantityRecord(orderItem.getProductId(), factor * orderItem.getQuantity()));
        });

        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);

        try {
            restTemplate.exchange(PRODUCT_SERVICE_URL + "/private/to-order", HttpMethod.PUT ,httpEntity, String.class);
        } catch(RestClientException e) {
            logger.error(Constants.UPDATE_STOCK_ERROR + " {}", e.getMessage());
            throw new ProductServiceException(Constants.COM_ERR_PROD);
        }
    }

    public void updateProductsFallback(List<OrderItem> orderItemList, int factor, Throwable throwable) throws ProductServiceException {
        System.out.println("Attemps Update Products: " + attempt);
        attempt++;
        logger.error("Fallback triggered for updateProducts due to: {}", throwable.getMessage());

        throw new ProductServiceException("Rate limit exceeded or service unavailable. Please try again later.");
    }
}