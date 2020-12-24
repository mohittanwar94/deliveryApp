package com.ezymd.restaurantapp.delivery.order.model;

public interface OrderStatus {
    int PROCESSING = 1;
    int ORDER_PREPARING = 2;
    int ORDER_ASSIGN_FOR_DELIVERY = 3;
    int ORDER_ACCEPTED = 4;
}
