package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {
        BidLevel level = state.getBidAt(0);
        final long price = level.price;
        final long quantity = level.quantity;

        // Get all active child orders
        final var activeOrders = state.getActiveChildOrders();

        // If there are less than 3 child orders, create one
        if (state.getChildOrders().size() < 3) {
            logger.info("Order created");
            return new CreateChildOrder(Side.BUY, quantity, price);

            // If there are more than 3 orders, cancel one
        } else if (activeOrders.size() > 3) {
            logger.info("Canceling excess order.");
            var lastOrder = activeOrders.get(activeOrders.size() - 1);
            logger.info("Child order to cancel: " + lastOrder);
            return new CancelChildOrder(lastOrder);

        } else {
            return NoAction.NoAction;
        }
    }
}
