package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.orderbook.OrderBook;
import codingblackfemales.orderbook.channel.MarketDataChannel;
import codingblackfemales.orderbook.channel.OrderChannel;
import codingblackfemales.orderbook.consumer.OrderBookInboundOrderConsumer;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractAlgoBackTest extends SequencerTestCase {


    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        final MarketDataChannel marketDataChannel = new MarketDataChannel(sequencer);
        final OrderChannel orderChannel = new OrderChannel(sequencer);
        final OrderBook book = new OrderBook(marketDataChannel, orderChannel);

        final OrderBookInboundOrderConsumer orderConsumer = new OrderBookInboundOrderConsumer(book);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        //set my algo logic
        container.setLogic(createAlgoLogic());

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(book);
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        return sequencer;
    }

    public abstract AlgoLogic createAlgoLogic();

    protected UnsafeBuffer createTickBuy1(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();


        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Adjusted bid book to trigger a buy
        encoder.bidBookCount(3)
                .next().price(70L).size(500L) // Price lower than VWAP (70 < 75), Quantity: 500
                .next().price(74L).size(200L)
                .next().price(75L).size(300L);

        encoder.askBookCount(3)
                .next().price(76L).size(100L)
                .next().price(80L).size(200L)
                .next().price(85L).size(500L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // Tick 2: Encourage Sell
    protected UnsafeBuffer createTickSell1(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Bid prices above VWAP
        encoder.bidBookCount(3)
                .next().price(80L).size(500L) // 80 > VWAP
                .next().price(82L).size(300L)
                .next().price(85L).size(200L);

        // Ask prices below VWAP
        encoder.askBookCount(3)
                .next().price(75L).size(400L)
                .next().price(77L).size(600L)
                .next().price(79L).size(800L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // Tick 3: Encourage Buy
    protected UnsafeBuffer createTickBuy2(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Bid prices below VWAP to encourage BUY
        encoder.bidBookCount(3)
                .next().price(68L).size(600L) // 68 < VWAP
                .next().price(72L).size(300L)
                .next().price(74L).size(200L);

        // Ask prices above VWAP
        encoder.askBookCount(3)
                .next().price(75L).size(400L)
                .next().price(77L).size(600L)
                .next().price(79L).size(800L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // Tick 4: Encourage Sell
    protected UnsafeBuffer createTickSell2(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Bid prices above VWAP to encourage SELL
        encoder.bidBookCount(3)
                .next().price(85L).size(600L) // 85 > VWAP
                .next().price(88L).size(400L)
                .next().price(90L).size(300L);

        // Ask prices below VWAP
        encoder.askBookCount(3)
                .next().price(80L).size(500L)
                .next().price(82L).size(700L)
                .next().price(84L).size(900L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // Tick 5: Trigger Cancel (VWAP below acceptable range)
    protected UnsafeBuffer createTickCancel1(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Extremely low bid prices to lower VWAP and trigger CANCEL
        encoder.bidBookCount(3)
                .next().price(50L).size(1000L) // 50 < VWAP (triggers cancel)
                .next().price(52L).size(800L)
                .next().price(55L).size(600L);

        // Ask prices
        encoder.askBookCount(3)
                .next().price(60L).size(500L)
                .next().price(62L).size(700L)
                .next().price(64L).size(900L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // Tick 6: Encourage Buy
    protected UnsafeBuffer createTickBuy3(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Bid prices below VWAP to encourage BUY
        encoder.bidBookCount(3)
                .next().price(65L).size(700L) // 65 < VWAP
                .next().price(68L).size(400L)
                .next().price(70L).size(300L);

        // Ask prices above VWAP
        encoder.askBookCount(3)
                .next().price(72L).size(500L)
                .next().price(74L).size(600L)
                .next().price(76L).size(800L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // Tick 8: Encourage Buy
    protected UnsafeBuffer createTickBuy4(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        // Bid prices below VWAP to encourage BUY
        encoder.bidBookCount(3)
                .next().price(60L).size(800L) // 60 < VWAP
                .next().price(63L).size(500L)
                .next().price(65L).size(400L);

        // Ask prices above VWAP
        encoder.askBookCount(3)
                .next().price(67L).size(700L)
                .next().price(69L).size(800L)
                .next().price(71L).size(1000L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

}
