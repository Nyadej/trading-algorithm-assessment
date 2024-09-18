package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
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

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        if (state.getChildOrders().size() < 3) {
            return new CreateChildOrder(Side.BUY, quantity, price);
        } else {
            return NoAction.NoAction;
        }
    }
}
