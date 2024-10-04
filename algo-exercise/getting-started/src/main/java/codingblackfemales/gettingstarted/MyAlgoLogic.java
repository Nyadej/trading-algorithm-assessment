package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.action.Action;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyAlgoLogic implements AlgoLogic {

    private long sharesOwned = 1000L; // Setting initial sharesOwned to 1000
    private long totalSpent = 0L; // tack the total money spent on buying shares
    private long totalEarned = 0L; // track the total money earned from selling shares
    private long profit = 0L; // tracks the overall profit/loss

    enum TradeAction { // defining three possible actions my algo can take
        BUY, SELL, CANCEL, HOLD
    }

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        TradeAction action; // declaring a variable action that will hold the decisions (BUY,SELL,HOLD) made by the algo

        BidLevel level = state.getBidAt(0); // to get the top bid (price + quantity) in the market to compare against the VWAP to make decisions
        final long price = level.price;
        final long quantity = level.quantity;

        // CONSTANTS
        final int DESIRED_ACTIVE_ORDERS = 3;
        final int TOTAL_ORDER_LIMIT = 6; // exit statement

        // Get all active child orders
        final var activeOrders = state.getActiveChildOrders(); // currently active (unfilled or un-cancelled) orders
        final var totalOrders = state.getChildOrders().size(); // total number of orders, active or inactive

        double totalMarketValue = 0.0; // store the total number of all active orders
        long totalQuantity = 0; // stores the total number of shares (quantity) from all active orders

        // Consider the top 3 bid and ask levels
        int orderBookLevels = 3;

        // looping through active orders
        for (int i = 0; i < orderBookLevels; i++) {
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel !=null) {
                totalMarketValue += bidLevel.price * bidLevel.quantity;
                totalQuantity += bidLevel.quantity;
        }

            // Get ask level
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel != null) {
                totalMarketValue += askLevel.price * askLevel.quantity;
                totalQuantity += askLevel.quantity;
            }
        }

        // calculating VWAP (volume weighted average price)
        double vWAP;
        if (totalQuantity <= 0) {
            // default VWAP if there are no active orders
            vWAP = 90;
            logger.info("No active orders, using hardcoded initial VWAP: {}", vWAP);

            // if there are active order, calculate VWAP
        } else {
            vWAP = totalMarketValue / (double) totalQuantity;
            logger.info("Current total market value = {}", totalMarketValue);
            logger.info("Current total quantity = {}", totalQuantity);
        }

        logger.info("Current VWAP value = {}", vWAP);

        // Debugging***
        logger.info("Checking if price: {} < VWAP: {}", price, vWAP);

        // the total number of orders should not exceed 10
        if (totalOrders >= TOTAL_ORDER_LIMIT) {
            logger.info("[DYNAMIC-PASSIVE-ALGO] Total order limit reached. No new orders will be created.");
            logFinalState();
            return NoAction.NoAction;
        }

        // If there are less than 3 child orders, BUY (more)
        if (price < vWAP && state.getActiveChildOrders().size() < DESIRED_ACTIVE_ORDERS) {
            action = TradeAction.BUY;

            // If the price is higher than VWAP and there are active orders to sell, SELL
        } else if (price > vWAP && state.getActiveChildOrders().size() <= DESIRED_ACTIVE_ORDERS && sharesOwned > 0) {
            action = TradeAction.SELL;

            // If the cancel condition is met (VWAP too low or too high), CANCEL the oldest active order
        } else if ((vWAP <= 60 || vWAP >= 90) && !state.getActiveChildOrders().isEmpty()) {
                    logger.info("Cancel condition triggered: VWAP is: {} .", vWAP);
                    logger.info("Number of active orders: {}", activeOrders.size());
                    action = TradeAction.CANCEL;

            // Otherwise, HOLD position
                } else {
                    action = TradeAction.HOLD;
                }

                switch (action) {
                    case BUY:
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is less than VWAP: {}, buying shares", price, vWAP);
                        sharesOwned += quantity;
                        totalSpent += price * quantity; // because in order books, the price = the price per unit of the quantity being traded
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {}", sharesOwned, totalSpent);
                        return new CreateChildOrder(Side.BUY, quantity, price);

                    case SELL:
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is higher than VWAP: {}, selling shares", price, vWAP);

                        // Ensure you own enough shares to sell
                        if (sharesOwned > 0) {
                            sharesOwned -= quantity;
                            totalEarned += price * quantity;

                            // Include market value of remaining shares in profit calculation
                            long marketValueOfSharesOwned = sharesOwned * price;
                            profit = (totalEarned + marketValueOfSharesOwned) - totalSpent;

                            logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
                            return new CreateChildOrder(Side.SELL, quantity, price);
                        } else {
                            return new CreateChildOrder(Side.SELL, sharesOwned, price);
                        }

                    case CANCEL:
                        // 1. find the oldest active order
                        final var oldestOrder = activeOrders.stream().findFirst().orElse(null);
                        //var childOrder = oldestOrder.get();

                        // 2. check if the oldest order exists
                        if (oldestOrder != null) {
                            // 3. extract the quantity and price of the oldest order
                            long oldestOrderQuantity = oldestOrder.getQuantity();
                            long oldestOrderPrice = oldestOrder.getPrice();

                            logger.info("Cancelling oldest order: Price: {}, Quantity: {}", oldestOrder.getPrice(), oldestOrder.getQuantity());
                            logger.info("Current VWAP: {} (out of the acceptable range)", vWAP);

                            return new CancelChildOrder(oldestOrder);
                        } else {
                            logger.info("No valid order found to cancel.");
                            return NoAction.NoAction;
                        }

                    default:
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}.", sharesOwned);
                        return NoAction.NoAction;

                }

            }

            private void logFinalState() {
                logger.info("[FINAL STATE REPORT \uD83C\uDFE6] Shares Owned: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
            }
        }