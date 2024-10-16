package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyAlgoBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testExampleBackTest() throws Exception {
        //create a sample market data tick....

        send(createTickBuy1());
        send(createTickSell1());
        send(createTickBuy2());
        send(createTickSell2());
        send(createTickCancel1());
        send(createTickBuy3());
        send(createTickBuy4());

        //when: market data moves towards us

        //then: get the state
        var state = container.getState();

        // Check the total filled quantity
        long totalFilledQuantity = state.getChildOrders().stream()
                .mapToLong(ChildOrder::getFilledQuantity)
                .sum();

        assertEquals("Total filled quantity is 3500", 3500, totalFilledQuantity);

        // Ensure no more than the total order limit is reached
        assertTrue("Total orders should be 5", state.getChildOrders().size() <= 5);
    }

}
