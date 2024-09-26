package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
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
        BUY, SELL, HOLD
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
        final int TOTAL_ORDER_LIMIT = 10; // exit statement

        // Get all active child orders
        final var activeOrders = state.getActiveChildOrders(); // currently active (unfilled or un-cancelled) orders
        final var totalOrders = state.getChildOrders().size(); // total number of orders, active or inactive

        long totalMarketValue = 0; // store the total number of all active orders
        long totalQuantity = 0; // stores the total number of shares (quantity) from all active orders

        // looping through active orders
        for (int i = 0; i < activeOrders.size(); i++) {
            totalMarketValue += activeOrders.get(i).getQuantity() * activeOrders.get(i).getPrice();
            totalQuantity += activeOrders.get(i).getQuantity();
        }

        // calculating VWAP (volume weighted average price)
        double vWAP;
        if (totalQuantity > 0) { // if there are active order, calculate VWAP
            vWAP = (double) totalMarketValue / (double) totalQuantity;
            logger.info("Current total marker value = {}", totalMarketValue);
            logger.info("Current total quantity = {}", totalQuantity);
        } else {
            // default VWAP if there are no active orders
            vWAP = 100.0;
            logger.info("No active orders, using hardcoded initial VWAP: {}", vWAP);
        }

        logger.info("Current VWAP value = {}", vWAP);

        // Debugging***
        logger.info("Checking if price < VWAP: {} < {}", price, vWAP);

        // TODO IMPLEMENT SELL LOGIC BASED ON VWAP

        // the total number of orders should not exceed 10
        if (totalOrders < TOTAL_ORDER_LIMIT) {
            // If there are less than 3 child orders, BUY (more)
            if (price < vWAP && activeOrders.size() < DESIRED_ACTIVE_ORDERS) {
                logger.info("Comparing price {} with VWAP {} for a potential BUY", price, vWAP);
                action = TradeAction.BUY;

                // If there are more than 3 orders, SELL
            } else if (price > vWAP && state.getActiveChildOrders().size() >= DESIRED_ACTIVE_ORDERS) {
                action = TradeAction.SELL;

                // For anything else, hold
                // TODO THINK OF HOW THIS CAN BE USED WHEN PROFIT IS NOT MADE
            } else {
                action = TradeAction.HOLD;
            }
        } else {
               return NoAction.NoAction; // once order limit is reached, the algorithm should stop
            }

            switch (action) {
                case BUY:
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is less than VWAP: {}, buying shares", price, vWAP);
                    sharesOwned += quantity;
                    totalSpent += price * quantity; // because in order books, the price = the price per unit of the quantity being traded
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {}", sharesOwned, totalSpent);
                    return new CreateChildOrder(Side.BUY, quantity, price);

                case SELL:
                    // 1. get the last active order
                    var lastOrder = activeOrders.get(activeOrders.size() - 1);

                    // 2. find the oldest active order
                    final var oldestOrder = activeOrders.stream().findFirst().orElse(null);
                    //var childOrder = oldestOrder.get();

                    // 3. check if the oldest order exists
                    if (oldestOrder !=null) {
                        // 4. extract the quantity and price of the oldest order
                        long oldestOrderQuantity = oldestOrder.getQuantity();
                        long oldestOrderPrice = oldestOrder.getPrice();

                        logger.info("Active orders: {} ", activeOrders);
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is higher than VWAP: {}, selling shares", price, vWAP);
                        sharesOwned -= oldestOrderQuantity;
                        totalEarned += oldestOrderPrice * oldestOrderQuantity;
                        profit = totalEarned - totalSpent;
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);

                        return new CancelChildOrder(lastOrder);
                    }
                    return NoAction.NoAction;

                default:
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}.", sharesOwned);
                    return NoAction.NoAction;

                    // TODO MAKE ALGO PRINT OUT ALL ORDERS THAT HAVE NOT BEEN FILLED?
            }
        }
    }

