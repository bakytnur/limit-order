package com.example.order.controller;

import com.example.order.exception.RecordAlreadyExistsException;
import com.example.order.model.Asset;
import com.example.order.model.TradingPair;
import com.example.order.model.User;
import com.example.order.model.request.AddAssetRequest;
import com.example.order.model.request.AddTradingPairRequest;
import com.example.order.model.request.AddUserRequest;
import com.example.order.model.request.AskRequest;
import com.example.order.model.request.BidRequest;
import com.example.order.model.request.DepositRequest;
import com.example.order.service.EventManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class OrderMatchingController {
    private final EventManager eventManager;

    @PostMapping("/user")
    public User addUser(@RequestBody AddUserRequest request) throws RecordAlreadyExistsException {
        return eventManager.addUser(request);
    }

    @GetMapping("/balance")
    public User getBalance(@RequestParam long userId) {
        return eventManager.displayBalance(userId);
    }

    @PostMapping("/asset")
    public Asset addAsset(@RequestBody AddAssetRequest request) throws RecordAlreadyExistsException {
        return eventManager.addAsset(request);
    }

    @GetMapping("/asset")
    public Asset getAsset(@RequestBody int assetId) throws RecordAlreadyExistsException {
        return eventManager.getAsset(assetId);
    }

    @PostMapping("/tradingPair")
    public TradingPair addTradingPair(@RequestBody AddTradingPairRequest request) throws Exception {
        return eventManager.addTradingPair(request);
    }

    @GetMapping("/tradingPair")
    public TradingPair getTradingPair(@RequestParam String tradingPairCode) {
        return eventManager.getTradingPair(tradingPairCode);
    }

    @PostMapping("/deposit")
    public Long depositEvent(@RequestBody DepositRequest request) throws Exception {
        return eventManager.depositOrder(request);
    }

    @PostMapping("/limit/bid")
    public ResponseEntity limitBid(@RequestBody BidRequest request) throws Exception {
        eventManager.limitBuyOrder(request);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/limit/ask")
    public ResponseEntity limitAsk(@RequestBody AskRequest request) throws Exception {
        eventManager.limitSellOrder(request);

        return ResponseEntity.ok(HttpStatus.OK);
    }
}
