package com.example.order.model.event;

import com.example.order.model.EventType;
import com.example.order.model.OrderBookEntry;
import lombok.Data;

@Data
public class AskOrderEvent implements IEvent {
    private EventType eventType;
    private long userId;
    private int baseAssetId;
    private int quoteAssetId;
    private long quantity;
    private Double price;

    // event time
    private long eventTime;

    public OrderBookEntry toOrderBookEntry() {
        OrderBookEntry orderBookEntry = new OrderBookEntry();
        orderBookEntry.setUserId(userId);
        orderBookEntry.setPrice(price);
        orderBookEntry.setQuantity(quantity);
        orderBookEntry.setBaseAssetId(baseAssetId);
        orderBookEntry.setQuoteAssetId(quoteAssetId);
        orderBookEntry.setEntryTime(eventTime);

        return orderBookEntry;
    }
}
