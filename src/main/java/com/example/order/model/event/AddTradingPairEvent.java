package com.example.order.model.event;

import com.example.order.model.EventType;
import lombok.Data;

@Data
public class AddTradingPairEvent implements IEvent {
    private EventType eventType;
    private int baseAssetId;
    private int quoteAssetId;

    // event time
    private long eventTime;
}
