package com.example.order.model.request;

import lombok.Data;

/**
 * User '1' asks for 'A' amount of `XYZ-USD` for a price of 'B'
 */
@Data
public class AskRequest {
    private long userId;
    private String tradingPair;
    private Double quantity;
    private Double price;
}
