package com.example.order.service;

import com.example.order.model.Asset;
import com.example.order.model.OrderBookEntry;
import com.example.order.model.TradingPair;
import com.example.order.model.User;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
public class AssetManager {
    // exchange assets
    private final HashMap<Integer, Asset> assetMap = new HashMap<>();
    // exchange assetID/denom map
    private final HashMap<String, Integer> assetDenomMap = new HashMap<>();
    // exchange users
    private final HashMap<Long, User> userMap = new HashMap<>();
    // exchange trading pairs
    private final HashMap<String, TradingPair> tradingPairMap = new HashMap<>();
    // ask book
    private final HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> askOrderBook
        = new HashMap<>();
    // bid book
    private final HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> bidOrderBook
        = new HashMap<>();

    public Asset getAssetById(int assetId) {
        return assetMap.get(assetId);
    }

    public Integer getAssetByDenom(String assetCode) {
        return assetDenomMap.get(assetCode);
    }

    public User getUserById(long userId) {
        return userMap.get(userId);
    }

    public TradingPair getTradingPair(String pairCode) {
        return tradingPairMap.get(pairCode);
    }

    public HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> getAskOrderBook() {
        return askOrderBook;
    }

    public HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> getBidOrderBook() {
        return bidOrderBook;
    }

    public HashMap<Integer, Asset> getAssetMap() {
        return assetMap;
    }

    public HashMap<String, Integer> getAssetDenomMap() {
        return assetDenomMap;
    }

    public HashMap<String, TradingPair> getTradingPairMap() {
        return tradingPairMap;
    }

    public HashMap<Long, User> getUserMap() {
        return userMap;
    }
}
