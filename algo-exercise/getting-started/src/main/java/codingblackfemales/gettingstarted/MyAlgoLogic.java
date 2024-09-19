package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private long shares = 1000L; // set initial amount of shares that I have to 1000
    private long profit = 0L; // to track the gains and losses from each trade
    private long totalSpent = 0L;
    private long totalEarned = 0L;

    enum TradeAction { // defining three possible actions my algo can take
        BUY, SELL, HOLD
    }

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        TradeAction action; // declaring a variable action of type TradeAction, which is an enum

        BidLevel level = state.getBidAt(0);
        final long price = level.price;
        final long quantity = level.quantity;

        // Get all active child orders
        final var activeOrders = state.getActiveChildOrders();

        // If there are less than 3 child orders, create one
        if (state.getChildOrders().size() < 3) {
            action = TradeAction.BUY;

            // If there are more than 3 orders, cancel one
        } else if (activeOrders.size() > 3) {
            action = TradeAction.SELL;

        } else {
            action = TradeAction.HOLD;
        }

        switch (action) {
            case BUY:
                logger.info("[DYNAMIC-PASSIVE-ALGO] You have: {} child orders. \nCreating a new order", state.getChildOrders().size());
                shares += quantity;
                totalSpent += price * quantity; // because in order books, the price = the price per unit of the quantity being traded
                profit = totalEarned - totalSpent;
                logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", shares, totalSpent, totalEarned, profit);
                return new CreateChildOrder(Side.BUY, quantity, price);

            case SELL:
                logger.info("[DYNAMIC-PASSIVE-ALGO] You have: {} child orders. \nCancelling an excess order", state.getChildOrders().size());
                shares -= quantity;
                totalEarned += price * quantity;
                profit += totalEarned - totalSpent;
                logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", shares, totalSpent, totalEarned, profit);
                var lastOrder = activeOrders.get(activeOrders.size() - 1);
                return new CancelChildOrder(lastOrder);

            default:
                logger.info("[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}.", shares);
                return NoAction.NoAction;
        }
    }
}
