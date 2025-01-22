package com.order.order_service;

import com.order.order_service.enums.OrderStatusEnum;
import com.order.order_service.models.OrderEntity;
import com.order.order_service.models.OrderItem;
import com.order.order_service.repositories.OrderItemRepository;
import com.order.order_service.repositories.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
		return args -> {

			OrderEntity order = new OrderEntity();
			order.setStatus(OrderStatusEnum.PENDING);
			order.setUserId(1L);
			orderRepository.save(order);

			OrderItem item1 = new OrderItem();
			item1.setProductId(1L);
			item1.setQuantity(2);

			order.addProduct(item1);
			orderItemRepository.save(item1);

			OrderItem item2 = new OrderItem();
			item2.setProductId(2L);
			item2.setQuantity(1);

			order.addProduct(item2);
			orderItemRepository.save(item2);

			System.out.println("Order with products initialized successfully!");
		};
	}

}
