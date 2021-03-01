package com.example.order.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    private long id;
    // user available
    private Map<Integer, Long> availableBalance = new HashMap<>();
    // user pending ask orders
    private Set<OrderBookEntry> askOrders = new HashSet<>();
    // user pending bid orders
    private Set<OrderBookEntry> bidOrders = new HashSet<>();

    public User(long id) {
        this.id = id;
    }

    public Long getAvailableBalanceFor(int assetId) {
        if (availableBalance.get(assetId) == null) {
            return 0L;
        }

        return availableBalance.get(assetId);
    }

    public void setAvailableBalanceFor(int assetId, long amount) {
        availableBalance.put(assetId, amount);
    }

    public void addBalance(int assetId, long amount) {
        availableBalance.putIfAbsent(assetId, 0L);
        availableBalance.put(assetId, availableBalance.get(assetId) + amount);
    }

    public void deductBalance(int assetId, long amount) {
        availableBalance.putIfAbsent(assetId, 0L);
        availableBalance.put(assetId, availableBalance.get(assetId) - amount);
    }

    public void removeAskOrder(OrderBookEntry sellOrder) {
        askOrders.remove(sellOrder);
    }

    public void removeBidOrder(OrderBookEntry buyOrder) {
        bidOrders.remove(buyOrder);
    }

    public Long getTotalBidOrderAmount(int baseAssetId, int quoteAssetId) {
        return bidOrders.stream()
            .filter(orderBookEntry -> orderBookEntry.getBaseAssetId() == baseAssetId
                    && orderBookEntry.getQuoteAssetId() == quoteAssetId)
            .map(orderBookEntry -> (long) (orderBookEntry.getPrice() * orderBookEntry.getQuantity()))
            .reduce(0L, Long::sum);
    }

    public Long getTotalAskOrderAmount(int baseAssetId, int quoteAssetId) {
        return askOrders.stream()
            .filter(orderBookEntry -> orderBookEntry.getBaseAssetId() == baseAssetId
                && orderBookEntry.getQuoteAssetId() == quoteAssetId)
            .map(OrderBookEntry::getQuantity)
            .reduce(0L, Long::sum);
    }
}
