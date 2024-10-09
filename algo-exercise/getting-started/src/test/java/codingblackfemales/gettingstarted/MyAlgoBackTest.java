package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.Test;

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
        send(createTickSell3());
        send(createTickBuy4());

        // with the above ticks current ROI is ~30.61%

        //ADD asserts when you have implemented your algo logic
        //assertEquals(container.getState().getChildOrders().size(), 3);

        //when: market data moves towards us

        //then: get the state
        var state = container.getState();

        //Check things like filled quantity, cancelled order count etc....
        //long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
        //assertEquals(225, filledQuantity);
    }

}
