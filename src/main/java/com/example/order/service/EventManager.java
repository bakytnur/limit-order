package com.example.order.service;

import com.example.order.exception.AmountTooLowException;
import com.example.order.exception.InsufficientBalanceException;
import com.example.order.exception.RecordAlreadyExistsException;
import com.example.order.exception.RecordNotFoundException;
import com.example.order.model.Asset;
import com.example.order.model.EventType;
import com.example.order.model.TradingPair;
import com.example.order.model.User;
import com.example.order.model.event.AddAssetEvent;
import com.example.order.model.event.AddTradingPairEvent;
import com.example.order.model.event.AddUserEvent;
import com.example.order.model.event.AskOrderEvent;
import com.example.order.model.event.BidOrderEvent;
import com.example.order.model.event.DepositEvent;
import com.example.order.model.request.AddAssetRequest;
import com.example.order.model.request.AddTradingPairRequest;
import com.example.order.model.request.AddUserRequest;
import com.example.order.model.request.AskRequest;
import com.example.order.model.request.BidRequest;
import com.example.order.model.request.DepositRequest;
import com.example.order.utils.Helper;
import java.util.Collection;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Order(3)
@RequiredArgsConstructor
@Service
public class EventManager {
    private final AssetManager assetManager;
    private final OrderMatchingService orderMatchingService;

    public Long depositOrder(DepositRequest request) throws AmountTooLowException {
        // assuming we won't accept anything less than 0.00001
        if (request.getAmount() <= 0.00001) {
            throw new AmountTooLowException("Amount should be over 0.00001");
        }

        User user = assetManager.getUserById(request.getUserId());
        assert user != null;

        Integer assetId = assetManager.getAssetByDenom(request.getAsset());
        assert assetId != null;

        DepositEvent event = new DepositEvent();
        event.setEventType(EventType.DEPOSIT_EVENT);
        event.setUserId(request.getUserId());
        event.setAssetId(assetId);
        event.setAmount(Helper.lengthen(request.getAmount()));
        event.setEventTime(System.currentTimeMillis());

        orderMatchingService.processOrder(event);

        return user.getAvailableBalanceFor(assetId);
    }

    public User displayBalance(long userId) {
        User user = assetManager.getUserById(userId);
        assert user != null;

        return user;
    }

    public void limitSellOrder(AskRequest request)
        throws AmountTooLowException, InsufficientBalanceException {
        if (request.getQuantity() <= 0.0) {
            throw new AmountTooLowException("Quantity should be more than 0");
        }

        if (request.getPrice() <= 0.0) {
            throw new AmountTooLowException("Price should be more than 0");
        }

        User user = assetManager.getUserById(request.getUserId());
        assert user != null;

        TradingPair tradingPair = assetManager.getTradingPair(request.getTradingPair());
        assert tradingPair != null;

        Asset baseAsset = assetManager.getAssetById(tradingPair.getBaseAssetId());
        assert baseAsset != null;

        Asset quoteAsset = assetManager.getAssetById(tradingPair.getQuoteAssetId());
        assert quoteAsset != null;

        // check user has enough balance to SELL. We check base asset 'XYZ' balance here
        Long availableBalance = user.getAvailableBalanceFor(baseAsset.getId());
        // amount
        long amount = Helper.lengthen(request.getQuantity());
        if (availableBalance < amount) {
            throw new InsufficientBalanceException("Balance too low");
        }

        // deduct available balance ...
        user.deductBalance(baseAsset.getId(), amount);

        // create an order event
        AskOrderEvent event = new AskOrderEvent();
        event.setEventType(EventType.ASK_ORDER_EVENT);
        event.setUserId(request.getUserId());
        event.setBaseAssetId(baseAsset.getId());
        event.setQuoteAssetId(quoteAsset.getId());
        event.setQuantity(Helper.lengthen(request.getQuantity()));
        event.setPrice(request.getPrice());
        event.setEventTime(System.currentTimeMillis());

        orderMatchingService.processOrder(event);
    }

    public void limitBuyOrder(BidRequest request)
        throws InsufficientBalanceException, AmountTooLowException {
        if (request.getQuantity() <= 0.0) {
            throw new AmountTooLowException("Quantity should be more than 0");
        }

        if (request.getPrice() <= 0.0) {
            throw new AmountTooLowException("Price should be more than 0");
        }

        User user = assetManager.getUserById(request.getUserId());
        assert user != null;

        TradingPair tradingPair = assetManager.getTradingPair(request.getTradingPair());
        assert tradingPair != null;

        Asset baseAsset = assetManager.getAssetById(tradingPair.getBaseAssetId());
        assert baseAsset != null;

        Asset quoteAsset = assetManager.getAssetById(tradingPair.getQuoteAssetId());
        assert quoteAsset != null;

        // check user has enough balance to BUY, we check quote asset 'USD' balance here
        Long availableBalance = user.getAvailableBalanceFor(quoteAsset.getId());
        // amount
        long amount = Helper.lengthen(request.getQuantity() * request.getPrice());
        if(availableBalance < amount) {
            throw new InsufficientBalanceException("Balance too low");
        }

        // deduct available balance ...
        user.deductBalance(quoteAsset.getId(), amount);

        // create an order event
        BidOrderEvent event = new BidOrderEvent();
        event.setEventType(EventType.BID_ORDER_EVENT);
        event.setUserId(request.getUserId());
        event.setBaseAssetId(baseAsset.getId());
        event.setQuoteAssetId(quoteAsset.getId());
        event.setQuantity(Helper.lengthen(request.getQuantity()));
        event.setPrice(request.getPrice());
        event.setEventTime(System.currentTimeMillis());

        orderMatchingService.processOrder(event);
    }

    public User addUser(AddUserRequest request) throws RecordAlreadyExistsException {
        if (assetManager.getUserById(request.getUserId()) != null) {
            throw new RecordAlreadyExistsException("User with " + request.getUserId() + " already exists");
        }
        AddUserEvent event = new AddUserEvent();
        event.setEventType(EventType.ADD_USER_EVENT);
        event.setUserId(request.getUserId());
        event.setEventTime(System.currentTimeMillis());
        orderMatchingService.processOrder(event);

        return assetManager.getUserById(request.getUserId());
    }

    public Asset addAsset(AddAssetRequest request) throws RecordAlreadyExistsException {
        if (assetManager.getAssetByDenom(request.getDenom()) != null) {
            throw new RecordAlreadyExistsException("Asset with " + request.getDenom() + " already exists");
        }

        if (assetManager.getAssetById(request.getAssetId()) != null) {
            throw new RecordAlreadyExistsException("Asset with " + request.getAssetId() + " already exists");
        }

        AddAssetEvent event = new AddAssetEvent();
        event.setEventType(EventType.ADD_ASSET_EVENT);
        event.setAssetId(request.getAssetId());
        event.setDenom(request.getDenom());
        event.setEventTime(System.currentTimeMillis());

        orderMatchingService.processOrder(event);

        return assetManager.getAssetById(request.getAssetId());
    }

    public TradingPair addTradingPair(AddTradingPairRequest request) throws Exception {
        if (assetManager.getAssetById(request.getBaseAssetId()) == null) {
            throw new RecordNotFoundException("Asset with " + request.getBaseAssetId() + " not found!");
        }

        if (assetManager.getAssetById(request.getQuoteAssetId()) != null) {
            throw new RecordNotFoundException("Asset with " + request.getQuoteAssetId() + " not found!");
        }

        AddTradingPairEvent event = new AddTradingPairEvent();
        event.setEventType(EventType.ADD_TRADING_PAIR_EVENT);
        event.setBaseAssetId(request.getBaseAssetId());
        event.setQuoteAssetId(request.getQuoteAssetId());
        event.setEventTime(System.currentTimeMillis());

        orderMatchingService.processOrder(event);

        HashMap<String, TradingPair> tradingPairMap = assetManager.getTradingPairMap();
        Collection<TradingPair> tradingPairs = tradingPairMap.values();

        return tradingPairs.stream()
            .filter(tradingPair -> tradingPair.getBaseAssetId() == request.getBaseAssetId()
                && tradingPair.getQuoteAssetId() == request.getQuoteAssetId())
            .findFirst().orElseThrow(() -> new Exception("Something went wrong when adding a new trading pair"));
    }

    public Asset getAsset(int assetId) {
        return assetManager.getAssetById(assetId);
    }

    public TradingPair getTradingPair(String code) {
        return assetManager.getTradingPair(code);
    }
}
