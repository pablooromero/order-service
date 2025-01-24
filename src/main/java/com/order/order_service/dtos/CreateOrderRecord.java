package com.order.order_service.dtos;

import java.util.List;

public record CreateOrderRecord(String email, List<ProductQuantityRecord> recordList) {
}
