package com.example.order.service;

import com.example.order.model.Asset;
import com.example.order.model.EventType;
import com.example.order.model.OrderBookEntry;
import com.example.order.model.TradingPair;
import com.example.order.model.User;
import com.example.order.model.event.AddAssetEvent;
import com.example.order.model.event.AddTradingPairEvent;
import com.example.order.model.event.AddUserEvent;
import com.example.order.model.event.AskOrderEvent;
import com.example.order.model.event.BidOrderEvent;
import com.example.order.model.event.DepositEvent;
import com.example.order.model.event.IEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Actual business logic for order matching engine
 */
@Order(2)
@RequiredArgsConstructor
@Service
public class OrderMatchingService {
    private final AssetManager assetManager;
    Logger LOGGER = LoggerFactory.getLogger(OrderMatchingService.class);

    void processOrder(IEvent event) {
        switch (event.getEventType()) {
            case DEPOSIT_EVENT:
                DepositEvent depositEvent = (DepositEvent) event;
                processUserBalance(depositEvent);
                break;
            case ASK_ORDER_EVENT:
                AskOrderEvent askOrderEvent = (AskOrderEvent) event;
                processAskOrder(askOrderEvent);
                break;
            case BID_ORDER_EVENT:
                BidOrderEvent bidOrderEvent = (BidOrderEvent) event;
                processBidOrder(bidOrderEvent);
                break;
            case ADD_USER_EVENT:
                AddUserEvent addUserEvent = (AddUserEvent) event;
                processAddUser(addUserEvent);
                break;
            case ADD_ASSET_EVENT:
                AddAssetEvent addAssetEvent = (AddAssetEvent) event;
                processAddAsset(addAssetEvent);
                break;
            case ADD_TRADING_PAIR_EVENT:
                AddTradingPairEvent addTradingPairEvent = (AddTradingPairEvent) event;
                processAddTradingPair(addTradingPairEvent);
            default:
                LOGGER.error("UNKNOWN event {}", event);
                break;
        }
    }

    private void processAddTradingPair(AddTradingPairEvent event) {
        HashMap<Integer, Asset> assetMap = assetManager.getAssetMap();
        Asset baseAsset = assetMap.get(event.getBaseAssetId());
        Asset quoteAsset = assetMap.get(event.getQuoteAssetId());

        HashMap<String, TradingPair> tradingPairMap = assetManager.getTradingPairMap();
        TradingPair tradingPair = new TradingPair();
        tradingPair.setBaseAssetId(event.getBaseAssetId());
        tradingPair.setQuoteAssetId(event.getQuoteAssetId());

        String tradingPairCode = baseAsset.getDenom() + "-" + quoteAsset.getDenom();
        tradingPair.setDenom(tradingPairCode);
        tradingPairMap.put(tradingPairCode, tradingPair);
    }

    private void processAddUser(AddUserEvent event) {
        HashMap<Long, User> userMap = assetManager.getUserMap();
        userMap.put(event.getUserId(), new User(event.getUserId()));
    }

    private void processAddAsset(AddAssetEvent event) {
        HashMap<Integer, Asset> assetMap = assetManager.getAssetMap();
        assetMap.put(event.getAssetId(), new Asset(event.getAssetId(), event.getDenom()));

        HashMap<String, Integer> assetCodeMap = assetManager.getAssetDenomMap();
        assetCodeMap.put(event.getDenom(), event.getAssetId());
    }

    private void processUserBalance(DepositEvent depositEvent) {
        User user = assetManager.getUserById(depositEvent.getUserId());
        user.addBalance(depositEvent.getAssetId(), depositEvent.getAmount());
    }

    private void processAskOrder(AskOrderEvent askOrderEvent) {
        HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> bidOrderBook = assetManager
            .getBidOrderBook();
        User askingUser = assetManager.getUserById(askOrderEvent.getUserId());

        // check `XYZ` bid orders
        ConcurrentSkipListSet<OrderBookEntry> bidOrders = bidOrderBook
            .get(askOrderEvent.getBaseAssetId());

        long quantity = askOrderEvent.getQuantity();

        if (bidOrders != null) {
            for (OrderBookEntry bidOrder : bidOrders) {
                // since order book is sorted there will no more matching orders
                if (bidOrder.getPrice() < askOrderEvent.getPrice()) {
                    break;
                }

                User biddingUser = assetManager.getUserById(bidOrder.getUserId());

                bidOrders.remove(bidOrder);
                biddingUser.removeBidOrder(bidOrder);

                long processQuantity = Math.min(bidOrder.getQuantity(), quantity);

                // compensate bidding user if matching is done lower than bidding price
                biddingUser.addBalance(askOrderEvent.getQuoteAssetId(),
                    (long)(processQuantity * (bidOrder.getPrice() - askOrderEvent.getPrice())));

                // Increase asking user 'USD' balance
                askingUser.addBalance(askOrderEvent.getQuoteAssetId(),
                    (long) (askOrderEvent.getPrice() * processQuantity));

                // Increase bidding user 'XYZ' balance
                biddingUser.addBalance(askOrderEvent.getBaseAssetId(), processQuantity);

                quantity = quantity - processQuantity;
            }
        }

        // if unprocessed quantity left, record it in bid book
        if (quantity > 0) {
            recordToOrderBook(askingUser, askOrderEvent, quantity);
        }
    }

    private void processBidOrder(BidOrderEvent bidOrderEvent) {
        HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> askOrderBook = assetManager
            .getAskOrderBook();
        User biddingUser = assetManager.getUserById(bidOrderEvent.getUserId());

        // check `XYZ` sell orders
        ConcurrentSkipListSet<OrderBookEntry> askOrders = askOrderBook
            .get(bidOrderEvent.getBaseAssetId());

        long quantity = bidOrderEvent.getQuantity();
        if (askOrders != null) {
            for (OrderBookEntry askOrder : askOrders) {
                // since order book is sorted there will no more matching orders
                if (askOrder.getPrice() > bidOrderEvent.getPrice()) {
                    break;
                }

                User askingUser = assetManager.getUserById(askOrder.getUserId());

                askOrders.remove(askOrder);
                askingUser.removeAskOrder(askOrder);

                long processQuantity = Math.min(askOrder.getQuantity(), quantity);

                // compensate bidding user if matching is done lower than bidding price
                biddingUser.addBalance(askOrder.getQuoteAssetId(),
                    (long)(processQuantity * (bidOrderEvent.getPrice() - askOrder.getPrice())));

                // Increase asking user 'USD' balance
                askingUser.addBalance(
                    bidOrderEvent.getQuoteAssetId(), (long) (askOrder.getPrice() * processQuantity));

                // Increase bidding user 'XYZ' balance
                biddingUser.addBalance(bidOrderEvent.getBaseAssetId(), processQuantity);

                quantity = quantity - processQuantity;
            }
        }

        // if unprocessed quantity left, record it in bid book
        if (quantity > 0) {
            recordToOrderBook(biddingUser, bidOrderEvent, quantity);
        }
    }

    void recordToOrderBook(User user, IEvent event, long quantity) {
        if (event.getEventType() == EventType.BID_ORDER_EVENT) {
            BidOrderEvent orderEvent = (BidOrderEvent) event;
            HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> orderBook = assetManager
                .getBidOrderBook();

            OrderBookEntry bidOrder = orderEvent.toOrderBookEntry();
            bidOrder.setQuantity(quantity);
            user.getBidOrders().add(bidOrder);

            ConcurrentSkipListSet<OrderBookEntry> bidOrders = orderBook
                .get(orderEvent.getBaseAssetId());
            if (bidOrders == null) {
                bidOrders = new ConcurrentSkipListSet<>(
                    Comparator.comparing(OrderBookEntry::getPrice).reversed()
                    .thenComparing(OrderBookEntry::getEntryTime)
                        .thenComparing(OrderBookEntry::getQuantity));
            }

            bidOrders.add(bidOrder);
            orderBook.put(orderEvent.getBaseAssetId(), bidOrders);
        } else {
            AskOrderEvent orderEvent = (AskOrderEvent) event;
            HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> orderBook = assetManager
                .getAskOrderBook();

            OrderBookEntry askOrder = orderEvent.toOrderBookEntry();
            askOrder.setQuantity(quantity);
            user.getAskOrders().add(askOrder);

            ConcurrentSkipListSet<OrderBookEntry> askOrders = orderBook
                .get(orderEvent.getBaseAssetId());
            if (askOrders == null) {
                askOrders = new ConcurrentSkipListSet<>(
                    Comparator.comparing(OrderBookEntry::getPrice)
                    .thenComparing(OrderBookEntry::getEntryTime)
                        .thenComparing(OrderBookEntry::getQuantity));
            }

            askOrders.add(askOrder);
            orderBook.put(orderEvent.getBaseAssetId(), askOrders);
        }
    }
}
