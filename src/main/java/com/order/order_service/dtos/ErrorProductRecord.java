package com.order.order_service.dtos;

import com.order.order_service.enums.ProductErrorEnum;

public record ErrorProductRecord(Long id, ProductErrorEnum productErrorEnum) {
}
