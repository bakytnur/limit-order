package com.example.order.model;

import lombok.Data;

@Data
public class OrderBookEntry {
    private long userId;
    private int baseAssetId;
    private int quoteAssetId;
    private long quantity;
    private Double price;
    private long entryTime;
}
