package com.example.order.model.request;

import lombok.Data;

@Data
public class AddTradingPairRequest {
    private int baseAssetId;
    private int quoteAssetId;
}
