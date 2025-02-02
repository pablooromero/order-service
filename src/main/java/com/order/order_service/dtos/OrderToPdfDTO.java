package com.order.order_service.dtos;

import java.util.List;

public class OrderToPdfDTO {
    private Long orderId;
    private Long userId;
    private String userMail;
    private List<ProductRecord> newProductList;

    public OrderToPdfDTO(Long orderId, Long userId, String userMail, List<ProductRecord> newProductList) {
        this.orderId = orderId;
        this.userId = userId;
        this.userMail = userMail;
        this.newProductList = newProductList;
    }

    public OrderToPdfDTO() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserMail() {
        return userMail;
    }

    public List<ProductRecord> getNewProductList() {
        return newProductList;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    public void setNewProductList(List<ProductRecord> newProductList) {
        this.newProductList = newProductList;
    }

    @Override
    public String toString() {
        return "OrderToPdfDTO{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", userMail='" + userMail + '\'' +
                ", newProductList=" + newProductList +
                '}';
    }
}
