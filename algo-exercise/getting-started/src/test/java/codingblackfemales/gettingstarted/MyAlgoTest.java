package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import messages.order.Side;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        // 2. Assert - The algorithm should decide to BUY
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

    }

    @Test
    public void testSellAction() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

        // 3. Send a SELL tick to trigger a SELL action
        send(createTick());

        // 4. Use Java Streams to verify a SELL order exists
        boolean sellOrderExists = container.getState().getActiveChildOrders().stream()
                .anyMatch(order -> order.getSide() == Side.SELL
                );

        // 5. Assert - The algorithm should decide to SELL
        assertTrue("SELL action when price is above VWAP and shares are owned!", sellOrderExists);
    }

    @Test
    public void testCancelAction() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals("Initial BUY orders count should be 3.", 3, container.getState().getActiveChildOrders().size());

        // 3. Capture the list of active order IDs before sending the CANCEL tick
        List<Long> beforeOrderIds = container.getState().getActiveChildOrders().stream()
                .map(ChildOrder::getOrderId)
                .toList();

        System.out.println("Before CANCEL: " + beforeOrderIds);

        // 4. Send a tick to trigger a CANCEL action
        send(createTick());

        // 5. List the active order IDs after sending the CANCEL tick
        List<Long> afterOrderIds = container.getState().getActiveChildOrders().stream()
                .map(ChildOrder::getOrderId)
                .toList();

        System.out.println("After CANCEL: " + afterOrderIds);

        // 6. Use Streams to check if any order ID from before is missing after (indicating a CANCEL)
        boolean cancelOccurred = beforeOrderIds.stream()
                .anyMatch(id -> !afterOrderIds.contains(id));

        // 7. Assert - The algorithm should decide to CANCEL
        assertTrue("A CANCEL action should have occurred, removing at least one order.", cancelOccurred);
    }

    @Test
    public void testHoldAction() throws Exception {
        // 1. Send a BUY tick to create initial BUY orders
        send(createTickBuy());

        // 2. Assert that 3 BUY orders have been created
        assertEquals("Initial BUY orders count should be 3.", 3, container.getState().getActiveChildOrders().size());

        // 3. Capture the list of active order IDs before sending the HOLD tick
        long buyOrdersBefore = container.getState().getActiveChildOrders().stream()
                .filter(order -> order.getSide() == Side.BUY)
                .count();
        assertEquals("Initial BUY orders count should be 3.", 3, buyOrdersBefore);

        // 3. Capture the list of active order IDs before sending the HOLD-triggering tick
        List<Long> beforeOrderIds = container.getState().getActiveChildOrders().stream()
                .map(ChildOrder::getOrderId)
                .toList();

        System.out.println("Before HOLD: " + beforeOrderIds);

        // 4. Send a HOLD tick
        send(createTickHold());

        // 5. Capture the list of active order IDs after sending the HOLD tick
        List<Long> afterOrderIds = container.getState().getActiveChildOrders().stream()
                .map(ChildOrder::getOrderId)
                .toList();

        System.out.println("After HOLD: " + afterOrderIds);

        // 6. Use Streams to check that the lists before and after HOLD are identical
        boolean holdOccurred = afterOrderIds.containsAll(beforeOrderIds) &&
                beforeOrderIds.containsAll(afterOrderIds);

        // 7. Assert - The algorithm should decide to HOLD
        assertTrue("A HOLD action should have occurred, with no changes to active orders.", holdOccurred);
    }

}