package com.ezymd.restaurantapp.delivery.order.model;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class OrderItems implements Serializable {

    @SerializedName("item")
    @Expose
    private String item;
    @SerializedName("price")
    @Expose
    private Integer price;
    @SerializedName("qty")
    @Expose
    private Integer qty;

    private boolean isSelected;

    @SerializedName("product_option_id")
    @Expose
    private String product_option_id = "";

    @SerializedName("product_option_name")
    @Expose
    private String product_option_name = "";


    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }


    public String getProduct_option_id() {
        return product_option_id;
    }

    public void setProduct_option_id(String product_option_id) {
        this.product_option_id = product_option_id;
    }

    public String getProduct_option_name() {
        return TextUtils.isEmpty(product_option_name) ? "" : product_option_name;
    }

    public void setProduct_option_name(String product_option_name) {
        this.product_option_name = product_option_name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
