package com.example.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XYZ-BTC
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingPair {
    // id of 'XYZ'
    private int baseAssetId;
    // id of 'USD'
    private int quoteAssetId;
    // code of trading pair (XYZ-USD)
    private String denom;
}
