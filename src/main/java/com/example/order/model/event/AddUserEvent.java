package com.example.order.model.event;

import com.example.order.model.EventType;
import lombok.Data;

@Data
public class AddUserEvent implements IEvent {
    private EventType eventType;
    private long userId;

    // event time
    private long eventTime;
}
