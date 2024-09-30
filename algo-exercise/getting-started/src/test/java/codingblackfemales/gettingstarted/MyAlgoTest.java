package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


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
    public void testBuy() throws Exception {

        //create a sample market data tick....
        send(createTick());
        send(createTick());
        send(createTick());

        //simple assert to check 3 orders are created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

    }

    @Test
    public void testCancel() throws Exception {

        // create 3 orders
        send(createTick());
        send(createTick());
        send(createTick());

        // check 3 orders were created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);

        // extra tick to trigger cancellation of THE EXTRA ORDER ONLY
        send(createTick());

        // check there are still 3 orders
        assertEquals(container.getState().getActiveChildOrders().size(), 3);
    }

    /*@Test
    public void testHold() throws Exception {

        // First, create 10 orders to reach the total order limit
        for (int i = 0; i < 10; i++) {
            send(createTick());
        }

        // Check 10 orders were created
        assertEquals(container.getState().getChildOrders().size(), 10);

        // extra tick to trigger cancellation of THE EXTRA ORDER ONLY
        send(createTick());

        // check there are still 10 orders
        assertEquals(container.getState().getChildOrders().size(), 10);
    }*/

}
