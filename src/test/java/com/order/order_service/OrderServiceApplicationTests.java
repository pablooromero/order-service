package com.order.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"USER_SERVICE_URL=http://localhost:8082",
		"PRODUCT_SERVICE_URL=http://localhost:8083"
})
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
