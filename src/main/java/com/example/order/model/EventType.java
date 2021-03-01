package com.example.order.model;

import java.util.HashMap;
import java.util.Map;

public enum EventType {
    DEPOSIT_EVENT(1),                   // deposit event
    ASK_ORDER_EVENT(2),                 // ask order event
    BID_ORDER_EVENT(3),                 // bid order event
    ADD_USER_EVENT(5),                  // add user event
    ADD_ASSET_EVENT(6),                 // add asset event
    ADD_TRADING_PAIR_EVENT(7);          // add trading pair event

    int type;
    EventType(int type) {
        this.type = type;
    }

    private static final Map<Integer, EventType> lookup = new HashMap<>();

    static {
        for (EventType event : EventType.values()) {
            lookup.put(event.type, event);
        }
    }

    public static EventType getEvent(int type) {
        return lookup.get(type);
    }
}
