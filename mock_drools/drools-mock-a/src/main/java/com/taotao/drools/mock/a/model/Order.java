package com.taotao.drools.mock.a.model;

public class Order {
    private Double amount;
    private Double discount;

    public Order(Double amount) {
        this.amount = amount;
    }
    // Getter和Setter
    public Double getAmount() { return amount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Double getDiscount() { return discount; }
}