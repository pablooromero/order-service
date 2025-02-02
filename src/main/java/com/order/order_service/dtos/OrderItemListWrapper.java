package com.order.order_service.dtos;

import com.order.order_service.models.OrderItem;

import java.util.List;

public class OrderItemListWrapper {
    private List<OrderItem> orderItemList;
    private List<ErrorProductRecord> errorProductRecordList;

    public OrderItemListWrapper(List<OrderItem> orderItemList, List<ErrorProductRecord> errorProductRecordList) {
        this.orderItemList = orderItemList;
        this.errorProductRecordList = errorProductRecordList;
    }

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }

    public List<ErrorProductRecord> getErrorProductRecordList() {
        return errorProductRecordList;
    }

    public void setErrorProductRecordList(List<ErrorProductRecord> errorProductRecordList) {
        this.errorProductRecordList = errorProductRecordList;
    }
}
