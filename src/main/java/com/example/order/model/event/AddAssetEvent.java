package com.example.order.model.event;

import com.example.order.model.EventType;
import lombok.Data;

@Data
public class AddAssetEvent implements IEvent {
    private EventType eventType;
    private int assetId;
    private String denom;

    // event time
    private long eventTime;
}
