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
    private long profit = 0L; // to track profit/loss based on trades
    private long totalSpent = 0L;
    private long totalEarned = 0L;

    enum TradeAction { // defining three possible actions my algo can take
        BUY, SELL, HOLD
    }

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        TradeAction action; // declaring a variable action of type TradeAction, which is an enum

        BidLevel level = state.getBidAt(0); // to get the first bid from the market data
        final long price = level.price;
        final long quantity = level.quantity;

        // CONSTANTS
        final int DESIRED_ACTIVE_ORDERS = 3;
        final int TOTAL_ORDER_LIMIT = 10; // exit statement

        // Get all active child orders
        final var activeOrders = state.getActiveChildOrders(); // currently active (unfilled or un-cancelled) orders
        final var totalOrders = state.getChildOrders().size(); // total number of orders, active or inactive

        // the total number of orders should not exceed 10
        if (totalOrders < TOTAL_ORDER_LIMIT) {
            // If there are less than 3 child orders, BUY (more)
            if (state.getActiveChildOrders().size() < DESIRED_ACTIVE_ORDERS) {
                action = TradeAction.BUY;

                // If there are more than 3 orders, SELL
            } else if (state.getActiveChildOrders().size() >= DESIRED_ACTIVE_ORDERS) {
                action = TradeAction.SELL;

                // For anything else, hold
                // TODO THINK OF HOW THIS CAN BE USED WHEN PROFIT IS NOT MADE
            } else {
                action = TradeAction.HOLD;
            }

            switch (action) {
                case BUY:
                    logger.info("[DYNAMIC-PASSIVE-ALGO] You have: {} child orders. \nCreating a new order", activeOrders.size());
                    sharesOwned += quantity;
                    totalSpent += price * quantity; // because in order books, the price = the price per unit of the quantity being traded
                    profit = totalEarned - totalSpent;
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
                    return new CreateChildOrder(Side.BUY, quantity, price);

                case SELL:
                    var lastOrder = activeOrders.get(activeOrders.size() - 1);
                    final var oldestOrder = activeOrders.stream().findFirst();
                    var childOrder = oldestOrder.get();

                    long oldestOrderQuantity = childOrder.getQuantity();
                    long oldestOrderPrice = childOrder.getPrice();
                    logger.info("Active orders: {} ", activeOrders);
                    logger.info("[DYNAMIC-PASSIVE-ALGO] You have: {} child orders. \nCancelling an excess order", activeOrders.size());
                    sharesOwned -= oldestOrderQuantity;
                    totalEarned += oldestOrderPrice * oldestOrderQuantity;
                    profit = totalEarned - totalSpent;
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);

                    return new CancelChildOrder(lastOrder);

                default:
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}.", sharesOwned);
                    return NoAction.NoAction;

                // TODO MAKE ALGO PRINT OUT ALL ORDERS THAT HAVE NOT BEEN FILLED?
            }
        }
        return NoAction.NoAction; // once order limit is reached, the algorithm should stop
    }
}

