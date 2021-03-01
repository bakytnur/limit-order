package com.example.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.order.exception.AmountTooLowException;
import com.example.order.exception.InsufficientBalanceException;
import com.example.order.model.Asset;
import com.example.order.model.OrderBookEntry;
import com.example.order.model.TradingPair;
import com.example.order.model.User;
import com.example.order.model.request.AskRequest;
import com.example.order.model.request.BidRequest;
import com.example.order.model.request.DepositRequest;
import com.example.order.service.AssetManager;
import com.example.order.service.EventManager;
import com.example.order.service.OrderMatchingService;
import com.example.order.utils.Helper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
public class EventManagerTest {
    private AssetManager assetManager;
    private EventManager eventManager;

    Logger LOGGER = LoggerFactory.getLogger(EventManagerTest.class);

    private static final long USER_ID_1 = 1;
    private static final long USER_ID_2 = 2;
    private static final long USER_ID_3 = 3;
    private static final long USER_ID_4 = 4;
    private static final long USER_ID_5 = 5;
    private static final long USER_ID_6 = 6;

    private static final String USD = "USD";
    private static final int USD_ASSET_ID = 2;

    private static final String XYZ = "XYZ";
    private static final int XYZ_ASSET_ID = 1;
    private static String TRADING_PAIR;

    @BeforeEach
    public void init () {
        LOGGER.info("init");
        assetManager = new AssetManager();
        OrderMatchingService orderMatchingService = new OrderMatchingService(assetManager);
        eventManager = new EventManager(assetManager, orderMatchingService);
        HashMap<Integer, Asset> assetMap = assetManager.getAssetMap();
        assetMap.put(USD_ASSET_ID, new Asset(USD_ASSET_ID,  USD));
        assetMap.put(XYZ_ASSET_ID, new Asset(XYZ_ASSET_ID,  XYZ));

        HashMap<String, Integer> assetCodeMap = assetManager.getAssetDenomMap();
        assetCodeMap.put(USD, USD_ASSET_ID);
        assetCodeMap.put(XYZ, XYZ_ASSET_ID);

        // initialize users
        HashMap<Long, User> userMap = assetManager.getUserMap();
        userMap.put(USER_ID_1, new User(USER_ID_1));
        userMap.put(USER_ID_2, new User(USER_ID_2));
        userMap.put(USER_ID_3, new User(USER_ID_3));
        userMap.put(USER_ID_4, new User(USER_ID_4));
        userMap.put(USER_ID_5, new User(USER_ID_5));
        userMap.put(USER_ID_6, new User(USER_ID_6));

        // initialize trading pairs
        HashMap<String, TradingPair> tradingPairMap = assetManager.getTradingPairMap();
        TRADING_PAIR = XYZ + "-" + USD;
        tradingPairMap.put(TRADING_PAIR,
            new TradingPair(XYZ_ASSET_ID, USD_ASSET_ID, TRADING_PAIR));
    }

    @Test
    public void singleDepositTest() throws AmountTooLowException {
        // Deposit 1000 USD
        double usdAmount = 1000;
        double xyzAmount = 75;
        DepositRequest depositUSD = new DepositRequest();
        depositUSD.setUserId(USER_ID_1);
        depositUSD.setAsset(USD);
        depositUSD.setAmount(usdAmount);
        eventManager.depositOrder(depositUSD);

        // Deposit 75 XYZ
        DepositRequest depositXYZ = new DepositRequest();
        depositXYZ.setUserId(USER_ID_1);
        depositXYZ.setAsset(XYZ);
        depositXYZ.setAmount(xyzAmount);
        eventManager.depositOrder(depositXYZ);

        User user = eventManager.displayBalance(USER_ID_1);
        Map<Integer, Long> availableBalance = user.getAvailableBalance();

        Long usdBalance = availableBalance.get(USD_ASSET_ID);
        Long xyzBalance = availableBalance.get(XYZ_ASSET_ID);

        assertThat(usdBalance).isEqualTo(Helper.lengthen(usdAmount));
        assertThat(xyzBalance).isEqualTo(Helper.lengthen(xyzAmount));
    }

    @Test
    public void multipleDepositTest() throws AmountTooLowException {
        // Deposit 1000 USD
        double usdAmount1 = 1000;
        double usdAmount2 = 233;
        double xyzAmount1 = 75;
        double xyzAmount2 = 156;
        DepositRequest depositUSD1 = new DepositRequest();
        depositUSD1.setUserId(USER_ID_1);
        depositUSD1.setAsset(USD);
        depositUSD1.setAmount(usdAmount1);
        eventManager.depositOrder(depositUSD1);

        DepositRequest depositUSD2 = new DepositRequest();
        depositUSD2.setUserId(USER_ID_1);
        depositUSD2.setAsset(USD);
        depositUSD2.setAmount(usdAmount2);
        eventManager.depositOrder(depositUSD2);

        // Deposit 75 XYZ
        DepositRequest depositXYZ1 = new DepositRequest();
        depositXYZ1.setUserId(USER_ID_1);
        depositXYZ1.setAsset(XYZ);
        depositXYZ1.setAmount(xyzAmount1);
        eventManager.depositOrder(depositXYZ1);

        DepositRequest depositXYZ2 = new DepositRequest();
        depositXYZ2.setUserId(USER_ID_1);
        depositXYZ2.setAsset(XYZ);
        depositXYZ2.setAmount(xyzAmount2);
        eventManager.depositOrder(depositXYZ2);

        // display the balance of user 1
        User user = eventManager.displayBalance(USER_ID_1);
        Map<Integer, Long> availableBalance = user.getAvailableBalance();

        Long usdBalance = availableBalance.get(USD_ASSET_ID);
        Long xyzBalance = availableBalance.get(XYZ_ASSET_ID);

        // result should be USDAmount1 + USDAmount2
        assertThat(usdBalance).isEqualTo(Helper.lengthen(usdAmount1 + usdAmount2));
        // XYZAmount1 + XYZAmount2
        assertThat(xyzBalance).isEqualTo(Helper.lengthen(xyzAmount1 + xyzAmount2));
    }

    @Test
    public void limitBuyOrderTest() throws AmountTooLowException, InsufficientBalanceException {
        // Deposit 1000 USD
        double usdAmount = 1000;
        DepositRequest depositUSD = new DepositRequest();
        depositUSD.setUserId(USER_ID_1);
        depositUSD.setAsset(USD);
        depositUSD.setAmount(usdAmount);
        eventManager.depositOrder(depositUSD);

        // bid 10 'XYZ' for price: 60
        double xyzPrice = 60;
        double quantity = 10;
        BidRequest bidRequest = new BidRequest();
        bidRequest.setUserId(USER_ID_1);
        bidRequest.setTradingPair(TRADING_PAIR);
        bidRequest.setPrice(xyzPrice);
        bidRequest.setQuantity(quantity);

        eventManager.limitBuyOrder(bidRequest);

        // since there is no asking order for XYZ, user's order goes to bidBook
        // user's biddingBalance will be 60 * 10 = 600 or equivalent XYZ
        // user's available balance will be 1000 - 600 = 400USD

        User user = eventManager.displayBalance(USER_ID_1);
        Map<Integer, Long> availableBalance = user.getAvailableBalance();
        Long totalBidOrderAmount = user.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        assertThat(availableBalance.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(400));
        assertThat(totalBidOrderAmount).isEqualTo(Helper.lengthen(600));

    }

    @Test
    public void limitSellOrderTest() throws AmountTooLowException, InsufficientBalanceException {
        // Deposit 75 XYZ
        long userId = 1;
        double xyzAmount = 75;

        DepositRequest depositXYZ = new DepositRequest();
        depositXYZ.setUserId(userId);
        depositXYZ.setAsset(XYZ);
        depositXYZ.setAmount(xyzAmount);
        eventManager.depositOrder(depositXYZ);

        // ask 10 'XYZ' for price: 60
        double xyzPrice = 60;
        double quantity = 10;
        AskRequest askRequest = new AskRequest();
        askRequest.setUserId(userId);
        askRequest.setTradingPair(TRADING_PAIR);
        askRequest.setPrice(xyzPrice);
        askRequest.setQuantity(quantity);

        eventManager.limitSellOrder(askRequest);

        // since there is no bidding order for XYZ, user's order goes to askBook
        // user's askingBalance will be 10 XYZ
        // user's available balance will be 75 - 10 = 65XYZ

        User user = eventManager.displayBalance(userId);
        Map<Integer, Long> availableBalance = user.getAvailableBalance();
        Long totalAskOrderAmount = user.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        assertThat(availableBalance.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(65));
        assertThat(totalAskOrderAmount).isEqualTo(Helper.lengthen(10));
    }

    @Test
    public void limitSellAndBuyOrderTest() throws AmountTooLowException, InsufficientBalanceException {
        // Deposit 75 XYZ to user 1
        double xyzAmount = 75;
        DepositRequest depositXYZ = new DepositRequest();
        depositXYZ.setUserId(USER_ID_1);
        depositXYZ.setAsset(XYZ);
        depositXYZ.setAmount(xyzAmount);
        eventManager.depositOrder(depositXYZ);

        // ask 11 'XYZ' for price: 89
        double xyzPriceAsk = 89;
        double quantityAsk = 11;
        AskRequest askRequest = new AskRequest();
        askRequest.setUserId(USER_ID_1);
        askRequest.setTradingPair(TRADING_PAIR);
        askRequest.setPrice(xyzPriceAsk);
        askRequest.setQuantity(quantityAsk);

        // Deposit 1000 USD to user 2
        double usdAmount = 1000;
        DepositRequest depositUSD = new DepositRequest();
        depositUSD.setUserId(USER_ID_2);
        depositUSD.setAsset(USD);
        depositUSD.setAmount(usdAmount);
        eventManager.depositOrder(depositUSD);

        // bid 11 'XYZ' for price: 90
        double xyzPriceBid = 90;
        double quantityBid = 11; // all 11
        BidRequest bidRequest = new BidRequest();
        bidRequest.setUserId(USER_ID_2);
        bidRequest.setTradingPair(TRADING_PAIR);
        bidRequest.setPrice(xyzPriceBid);
        bidRequest.setQuantity(quantityBid);

        eventManager.limitSellOrder(askRequest);
        eventManager.limitBuyOrder(bidRequest);

        User user1 = eventManager.displayBalance(USER_ID_1);
        Map<Integer, Long> availableBalance1 = user1.getAvailableBalance();
        Long totalAskOrderAmount1 = user1.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount1 = user1.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // matching will be done with the seller's price
        assertThat(availableBalance1.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(11 * 89));
        assertThat(totalAskOrderAmount1).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount1).isEqualTo(Helper.lengthen(0));
        // had 75, sold 11, remained 64
        assertThat(availableBalance1.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(64));

        User user2 = eventManager.displayBalance(USER_ID_2);
        Map<Integer, Long> availableBalance2 = user2.getAvailableBalance();
        Long totalAskOrderAmount2 = user2.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount2 = user2.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // bought all 11
        assertThat(availableBalance2.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(11));
        assertThat(totalAskOrderAmount2).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount2).isEqualTo(Helper.lengthen(0));
        // though user2 bids for 90x11 XYZ, actual matching was done with 89. (1USD * 11 was refunded)
        assertThat(availableBalance2.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(1000 - 11 * 89));
    }

    @Test
    public void limitBuyAndSellOrderTest() throws AmountTooLowException, InsufficientBalanceException {
        // Deposit 75 XYZ to user 1
        double xyzAmount = 75;

        DepositRequest depositXYZ = new DepositRequest();
        depositXYZ.setUserId(USER_ID_1);
        depositXYZ.setAsset(XYZ);
        depositXYZ.setAmount(xyzAmount);
        eventManager.depositOrder(depositXYZ);

        // ask 11 'XYZ' for price: 89
        double xyzPriceAsk = 89;
        double quantityAsk = 11;
        AskRequest askRequest = new AskRequest();
        askRequest.setUserId(USER_ID_1);
        askRequest.setTradingPair(TRADING_PAIR);
        askRequest.setPrice(xyzPriceAsk);
        askRequest.setQuantity(quantityAsk);

        // Deposit 1000 USD to user 2
        double usdAmount = 1000;
        DepositRequest depositUSD = new DepositRequest();
        depositUSD.setUserId(USER_ID_2);
        depositUSD.setAsset(USD);
        depositUSD.setAmount(usdAmount);
        eventManager.depositOrder(depositUSD);

        // bid 11 'XYZ' for price: 90
        double xyzPriceBid = 90;
        double quantityBid = 11; // all 11
        BidRequest bidRequest = new BidRequest();
        bidRequest.setUserId(USER_ID_2);
        bidRequest.setTradingPair(TRADING_PAIR);
        bidRequest.setPrice(xyzPriceBid);
        bidRequest.setQuantity(quantityBid);

        eventManager.limitBuyOrder(bidRequest);
        eventManager.limitSellOrder(askRequest);

        User user1 = eventManager.displayBalance(USER_ID_1);
        Map<Integer, Long> availableBalance1 = user1.getAvailableBalance();
        Long totalAskOrderAmount1 = user1.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount1 = user1.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // matching will be done with the seller's price
        assertThat(availableBalance1.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(11 * 89));
        assertThat(totalAskOrderAmount1).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount1).isEqualTo(Helper.lengthen(0));
        // had 75, sold 11, remained 64
        assertThat(availableBalance1.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(64));

        User user2 = eventManager.displayBalance(USER_ID_2);
        Map<Integer, Long> availableBalance2 = user2.getAvailableBalance();
        Long totalAskOrderAmount2 = user2.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount2 = user2.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // bought all 11
        assertThat(availableBalance2.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(11));
        assertThat(totalAskOrderAmount2).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount2).isEqualTo(Helper.lengthen(0));
        // though user2 bids for 90x11 XYZ, actual matching was done with 89. (1USD * 11 was refunded)
        assertThat(availableBalance2.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(1000 - 11 * 89));
    }

    @Test
    public void limitMultipleBuyAndSellOrderTest()
        throws AmountTooLowException, InsufficientBalanceException, InterruptedException {
        // Deposit 87 XYZ to user 1
        double xyzAmount = 87;
        DepositRequest depositXYZ = new DepositRequest();
        depositXYZ.setUserId(USER_ID_1);
        depositXYZ.setAsset(XYZ);
        depositXYZ.setAmount(xyzAmount);
        eventManager.depositOrder(depositXYZ);

        // ask 14 'XYZ' for price: 89
        double xyzPriceAsk = 89;
        double quantityAsk = 14;
        AskRequest askRequest = new AskRequest();
        askRequest.setUserId(USER_ID_1);
        askRequest.setTradingPair(TRADING_PAIR);
        askRequest.setPrice(xyzPriceAsk);
        askRequest.setQuantity(quantityAsk);

        // Deposit 100 USD to user 2
        double usdAmount1 = 100;
        DepositRequest depositUSD1 = new DepositRequest();
        depositUSD1.setUserId(USER_ID_2);
        depositUSD1.setAsset(USD);
        depositUSD1.setAmount(usdAmount1);
        eventManager.depositOrder(depositUSD1);

        // bid 1 'XYZ' for price: 90
        double xyzPriceBid = 90;
        double quantityBid = 1;
        BidRequest bidRequest1 = new BidRequest();
        bidRequest1.setUserId(USER_ID_2);
        bidRequest1.setTradingPair(TRADING_PAIR);
        bidRequest1.setPrice(xyzPriceBid);
        bidRequest1.setQuantity(quantityBid);

        // Deposit 300 USD to user 3
        double usdAmount2 = 300;
        DepositRequest depositUSD2 = new DepositRequest();
        depositUSD2.setUserId(USER_ID_3);
        depositUSD2.setAsset(USD);
        depositUSD2.setAmount(usdAmount2);
        eventManager.depositOrder(depositUSD2);

        // bid 3 'XYZ' for price: 89
        double xyzPriceBid2 = 89;
        double quantityBid2 = 3;
        BidRequest bidRequest2 = new BidRequest();
        bidRequest2.setUserId(USER_ID_3);
        bidRequest2.setTradingPair(TRADING_PAIR);
        bidRequest2.setPrice(xyzPriceBid2);
        bidRequest2.setQuantity(quantityBid2);

        // Deposit 400 USD to user 4
        double usdAmount3 = 400;
        DepositRequest depositUSD3 = new DepositRequest();
        depositUSD3.setUserId(USER_ID_4);
        depositUSD3.setAsset(USD);
        depositUSD3.setAmount(usdAmount3);
        eventManager.depositOrder(depositUSD3);

        // bid 4 'XYZ' for price: 88
        // This won't be completed as it is less than asking price
        double xyzPriceBid3 = 88;
        double quantityBid3 = 4;
        BidRequest bidRequest3 = new BidRequest();
        bidRequest3.setUserId(USER_ID_4);
        bidRequest3.setTradingPair(TRADING_PAIR);
        bidRequest3.setPrice(xyzPriceBid3);
        bidRequest3.setQuantity(quantityBid3);

        // Deposit 800 USD to user 5
        double usdAmount4 = 800;
        DepositRequest depositUSD4 = new DepositRequest();
        depositUSD4.setUserId(USER_ID_5);
        depositUSD4.setAsset(USD);
        depositUSD4.setAmount(usdAmount4);
        eventManager.depositOrder(depositUSD4);

        // bid 8 'XYZ' for price: 90
        double xyzPriceBid4 = 90;
        double quantityBid4 = 8;
        BidRequest bidRequest4 = new BidRequest();
        bidRequest4.setUserId(USER_ID_5);
        bidRequest4.setTradingPair(TRADING_PAIR);
        bidRequest4.setPrice(xyzPriceBid4);
        bidRequest4.setQuantity(quantityBid4);

        eventManager.limitBuyOrder(bidRequest1);
        eventManager.limitBuyOrder(bidRequest2);
        eventManager.limitBuyOrder(bidRequest3);
        eventManager.limitBuyOrder(bidRequest4);
        eventManager.limitSellOrder(askRequest);

        User user1 = eventManager.displayBalance(USER_ID_1);
        Map<Integer, Long> availableBalance1 = user1.getAvailableBalance();
        Long totalAskOrderAmount1 = user1.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount1 = user1.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // matching will be done with the seller's price
        assertThat(availableBalance1.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(1 * 89 + 3 * 89 + 8 * 89));
        assertThat(totalAskOrderAmount1).isEqualTo(Helper.lengthen(14 - (1 + 3 + 8)));
        assertThat(totalBidOrderAmount1).isEqualTo(Helper.lengthen(0));
        // had 87, sold: 12, ask: 2, remaining: 87-14
        assertThat(availableBalance1.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(87-14));

        // User 2
        User user2 = eventManager.displayBalance(USER_ID_2);
        Map<Integer, Long> availableBalance2 = user2.getAvailableBalance();
        Long totalAskOrderAmount2 = user2.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount2 = user2.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // had 100 USD, bought 1 XYZ for 89
        assertThat(availableBalance2.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(1));
        assertThat(totalAskOrderAmount2).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount2).isEqualTo(Helper.lengthen(0));
        assertThat(availableBalance2.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(100 - 1 * 89));

        // User 3
        User user3 = eventManager.displayBalance(USER_ID_3);
        Map<Integer, Long> availableBalance3 = user3.getAvailableBalance();
        Long totalAskOrderAmount3 = user3.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount3 = user3.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // had 300 USD, bought 3 XYZ for 89
        assertThat(availableBalance3.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(3));
        assertThat(totalAskOrderAmount3).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount3).isEqualTo(Helper.lengthen(0));
        assertThat(availableBalance3.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(300 - 3 * 89));

        // User 4
        User user4 = eventManager.displayBalance(USER_ID_4);
        Map<Integer, Long> availableBalance4 = user4.getAvailableBalance();
        Long totalAskOrderAmount4 = user4.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        Long totalBidOrderAmount4 = user4.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // had 400 USD, bid for 4 XYZ for 88
        // as 88 is less than asking price: 89, user4 couldn't buy any XYZ
        assertThat(availableBalance4.get(XYZ_ASSET_ID)).isEqualTo(null);
        assertThat(totalAskOrderAmount4).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount4).isEqualTo(Helper.lengthen(4 * 88));
        // as 4 * 88 USD moved to bidding order, remaining 400 - 4 * 88
        assertThat(availableBalance4.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(400 - 4 * 88));

        /**
         * As user4 still has bidding amount, let user1 ask more amount for 87
         */
        // ask 10 'XYZ' for price: 87
        double xyzPriceAsk2 = 87;
        double quantityAsk2 = 10;
        AskRequest askRequest2 = new AskRequest();
        askRequest2.setUserId(USER_ID_1);
        askRequest2.setTradingPair(TRADING_PAIR);
        askRequest2.setPrice(xyzPriceAsk2);
        askRequest2.setQuantity(quantityAsk2);

        eventManager.limitSellOrder(askRequest2);

        user1 = eventManager.displayBalance(USER_ID_1);
        availableBalance1 = user1.getAvailableBalance();
        totalAskOrderAmount1 = user1.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        totalBidOrderAmount1 = user1.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // matching will be done with the seller's price
        assertThat(availableBalance1.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(1 * 89 + 3 * 89 + 8 * 89 + 4 * 87));
        assertThat(totalAskOrderAmount1).isEqualTo(Helper.lengthen(14 - (1 + 3 + 8) + 10 - 4));
        assertThat(totalBidOrderAmount1).isEqualTo(Helper.lengthen(0));
        // had 87, sold: 12 + 4, ask: 2 + 6, remaining: 87-14 - 10
        assertThat(availableBalance1.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(87-14 - 10));

        // user4 balance
        eventManager.displayBalance(USER_ID_4);
        availableBalance4 = user4.getAvailableBalance();
        totalAskOrderAmount4 = user4.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        totalBidOrderAmount4 = user4.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // had 400 USD, bid for 4 XYZ for 88
        assertThat(availableBalance4.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(4));
        assertThat(totalAskOrderAmount4).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount4).isEqualTo(Helper.lengthen(0));
        // as order is completed with 87 per XYZ. 4 * 87 USD moved to bidding order, remaining 400 - 4 * 87
        assertThat(availableBalance4.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(400 - 4 * 87));

        /**
         * As user1 still has 8 XYZ in asking amount, let user3 bid all the remaining amount
         */
        // Deposit 1000 USD to user 3
        double usdAmount5 = 1000;
        DepositRequest depositUSD5 = new DepositRequest();
        depositUSD5.setUserId(USER_ID_3);
        depositUSD5.setAsset(USD);
        depositUSD5.setAmount(usdAmount5);
        eventManager.depositOrder(depositUSD5);

        // bid 8 'XYZ' for price: 91
        double xyzPriceBid5 = 91;
        double quantityBid5 = 8;
        BidRequest bidRequest5 = new BidRequest();
        bidRequest5.setUserId(USER_ID_3);
        bidRequest5.setTradingPair(TRADING_PAIR);
        bidRequest5.setPrice(xyzPriceBid5);
        bidRequest5.setQuantity(quantityBid5);

        eventManager.limitBuyOrder(bidRequest5);

        // user 1 balance
        user1 = eventManager.displayBalance(USER_ID_1);
        availableBalance1 = user1.getAvailableBalance();
        totalAskOrderAmount1 = user1.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        totalBidOrderAmount1 = user1.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        assertThat(availableBalance1.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen(14 * 89 + 10 * 87));
        assertThat(totalAskOrderAmount1).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount1).isEqualTo(Helper.lengthen(0));
        // sold 14+10 XYZ out of 87
        assertThat(availableBalance1.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(87 - (14 + 10)));

        // user 3 balance
        user3 = eventManager.displayBalance(USER_ID_3);
        availableBalance3 = user3.getAvailableBalance();
        totalAskOrderAmount3 = user3.getTotalAskOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);
        totalBidOrderAmount3 = user3.getTotalBidOrderAmount(XYZ_ASSET_ID, USD_ASSET_ID);

        // 1000 + 300 USD = total 1300USD deposit
        // bought 3 XYZ x 89, in the first attempt
        // bought remaining 2 XYZ x 87, 6 XYZ x 87 in the second attempt
        assertThat(availableBalance3.get(USD_ASSET_ID)).isEqualTo(Helper.lengthen((1000 + 300) - 3 * 89 - 2 * 89 - 6 * 87));
        assertThat(totalAskOrderAmount3).isEqualTo(Helper.lengthen(0));
        assertThat(totalBidOrderAmount3).isEqualTo(Helper.lengthen(0));
        // bought total 11 XYZ
        assertThat(availableBalance3.get(XYZ_ASSET_ID)).isEqualTo(Helper.lengthen(3 + 8));

        /**
         * Finally check all ask & bid order should be empty
         */

        HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> askOrderBook = assetManager
            .getAskOrderBook();
        HashMap<Integer, ConcurrentSkipListSet<OrderBookEntry>> bidOrderBook = assetManager
            .getBidOrderBook();

        ConcurrentSkipListSet<OrderBookEntry> xyzAskOrders = askOrderBook.get(XYZ_ASSET_ID);
        ConcurrentSkipListSet<OrderBookEntry> xyzBidOrders = bidOrderBook.get(XYZ_ASSET_ID);
        assertThat(xyzAskOrders.size()).isEqualTo(0);
        assertThat(xyzBidOrders.size()).isEqualTo(0);
    }
}
