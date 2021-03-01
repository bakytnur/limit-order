package com.example.order.model.event;

import com.example.order.model.EventType;
import lombok.Data;

@Data
public class DepositEvent implements IEvent {
    private EventType eventType;
    private long userId;
    private int assetId;
    private long amount;

    // event time
    private long eventTime;
}
