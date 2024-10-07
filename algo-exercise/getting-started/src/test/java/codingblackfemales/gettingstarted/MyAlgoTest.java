package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import messages.order.Side;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * This test is designed to check your algo behavior in isolation of the order book.
 *
 * You can tick in market data messages by creating new versions of createTick() (ex. createTick2, createTickMore etc.)
 *
 * You should then add behaviour to your algo to respond to that market data by creating or cancelling child orders.
 *
 * When you are comfortable you algo does what you expect, then you can move on to creating the MyAlgoBackTest.
 *
 */

public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyAlgoLogic();
    }

    @Test
    public void testBuyAction() throws Exception {
        // 1. Send a BUY tick to create BUY orders
        send(createTickBuy());

        // 2. The algorithm should decide to BUY shares
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

    }

    @Test
    public void testSellAction() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

        // 3. Send a SELL tick to trigger a SELL action
        send(createTickSell());

        // 4. Use Java Streams to verify a SELL order exists
        boolean sellOrderExists = container.getState().getActiveChildOrders().stream()
                .anyMatch(order -> order.getSide() == Side.SELL
                );

        // 5. The algorithm should decide to SELL shares
        assertTrue("SELL action when price is above VWAP and shares are owned!", sellOrderExists);
    }

    @Test
    public void testCancelAction() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

        // 3. Send a tick to trigger a cancel action
        send(createTickSell());

        // 4. Use Java Streams to verify a CANCEL order exists
        boolean cancelOrderExists = container.getState().getActiveChildOrders().stream()
                .anyMatch(order -> order.getSide() == Side.BUY
                );

        // 5. The algorithm should decide to CANCEL the oldest active order
        assertTrue("CANCEL action when the VWAP is out of the acceptable range, and there are active orders!", cancelOrderExists);
    }

    @Test
    public void testHoldAction() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

        // 3. Send a tick that can trigger a hold action
        send(createTickSell());

        // 4. Use Java Streams to verify a HOLD order exists
        boolean holdOrderExists = container.getState().getActiveChildOrders().stream()
                .anyMatch(order -> order.getSide() == Side.BUY
                );

        // 5. The algorithm should decide to HOLD and take no action
        assertTrue("HOLD action when no conditions are met!", holdOrderExists);
    }

    @Test
    public void testFinalStateLogging() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

        // 3. Send a tick to check if the algorithm reaches the total order limit
        send(createTickSell());

        // 4. Use Java Streams to verify logFinalState() outputs the final profit/loss and shares owned at the right time
        boolean finalStateLogExists = container.getState().getActiveChildOrders().stream()
                .anyMatch(order -> order.getSide() == Side.BUY
                );

        // 5. logFinalState() method is called, logging the final profit, loss and shares owned
        assertTrue("Final state logging when order limit is reached!", finalStateLogExists);
    }

    @Test
    public void testSharesOwned() throws Exception {


    }
}