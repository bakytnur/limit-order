package com.example.order.model.event;

import com.example.order.model.EventType;

public interface IEvent {
    EventType getEventType();
    long getEventTime();
}
