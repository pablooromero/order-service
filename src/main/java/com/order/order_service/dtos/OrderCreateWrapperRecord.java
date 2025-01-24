package com.order.order_service.dtos;

import java.util.List;

public record OrderCreateWrapperRecord(OrderDTO orderDTO, List<ErrorProductRecord> errorProductRecordList) {
}
