package com.order.order_service.dtos;

import java.util.List;

public record NewOrderRecord(List<ProductQuantityRecord> recordList) {
}
