package codingblackfemales.ui.module;

import codingblackfemales.sequencer.event.MarketDataEventListener;
import codingblackfemales.sequencer.net.Consumer;
import codingblackfemales.sequencer.net.Network;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.marketdata.AskBookUpdateDecoder;
import messages.marketdata.BidBookUpdateDecoder;
import messages.marketdata.BookUpdateDecoder;
import org.agrona.DirectBuffer;
import org.finos.toolbox.time.Clock;
import org.finos.vuu.core.table.DataTable;
import org.finos.vuu.core.table.InMemDataTable;
import org.finos.vuu.core.table.RowData;
import org.finos.vuu.core.table.RowWithData;
import org.finos.vuu.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AlgoProvider extends MarketDataEventListener implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(AlgoProvider.class);

    private final DataTable table;
    private final TestNetwork network;

    private final Clock clock;

    private int bidLength = 0;
    private int askLength = 0;

    private long instrumentId;
    private final BidLevel[] bidBook = new BidLevel[15];
    private final AskLevel[] askBook = new AskLevel[15];

    public AlgoProvider(final DataTable table, final TestNetwork network, final Clock clock){
        this.table = table;
        this.network = network;
        this.clock = clock;
    }

    private static void empty(BidLevel[] levels){
        for (int i=0; i<levels.length; i++) {
            levels[i] = null;
        }
    }

    private static void emptyLevelsTo(int startLevel, int toLevel, BidLevel[] levels){
        for (int i=startLevel; i<toLevel; i++) {
            levels[i] = null;
        }
    }

    private static void empty(AskLevel[] levels){
        for (int i=0; i<levels.length; i++) {
            levels[i] = null;
        }
    }

    private static void emptyLevelsTo(int startLevel, int toLevel, AskLevel[] levels){
        for (int i=startLevel; i<toLevel; i++) {
            levels[i] = null;
        }
    }



    @Override
    public void onBookUpdate(final BookUpdateDecoder bookUpdate) {
        int bookLevel = 0;

        //empty(askBook);

        instrumentId = bookUpdate.instrumentId();

        for(BookUpdateDecoder.AskBookDecoder decoder : bookUpdate.askBook()){
            final long price = decoder.price();
            final long quantity = decoder.size();

            askBook[bookLevel] = new AskLevel();
            askBook[bookLevel].setPrice(price);
            askBook[bookLevel].setQuantity(quantity);

            logger.debug("[ALGO] ASK: price:" + price + " quantity:" + quantity);

            bookLevel+= 1;
            askLength = bookLevel;
        }
        //empty the rest of the levels where no data is supplied
        if(bookLevel > 0 ){
            emptyLevelsTo(bookLevel, 15, askBook);
        }

        bookLevel = 0;
        //empty(bidBook);

        for(BookUpdateDecoder.BidBookDecoder decoder : bookUpdate.bidBook()){
            final long price = decoder.price();
            final long quantity = decoder.size();
            bidBook[bookLevel] = new BidLevel();
            bidBook[bookLevel].setPrice(price);
            bidBook[bookLevel].setQuantity(quantity);
            logger.debug("[ALGO] BID: price:" + price + " quantity:" + quantity);

            bookLevel+= 1;
            bidLength = bookLevel;

        }

        //empty the rest of the levels where no data is supplied
        if(bookLevel > 0){
            emptyLevelsTo(bookLevel, 15, bidBook);
        }

        updateBookTable();

        debugBidOfferCross();
    }

    private void updateBookTable(){
        for (int j=0; j<Math.max(bidBook.length, askBook.length); j++){

            final BidLevel bidlevel =  bidBook[j];
            final AskLevel askLevel =  askBook[j];

            final String key = String.valueOf(instrumentId) + j;

            final Map<String, Object> update = mkUpdate(key, instrumentId, j, bidlevel, askLevel);

            //logger.info("TABLE UPDATE" + update);

            table.processUpdate(key, new RowWithData(key, update), clock.now());
        }
    }


    private void updateBidTable(){
        for (int j=0; j<bidBook.length; j++){

            final BidLevel bidlevel =  bidBook[j];

            final String key = String.valueOf(instrumentId) + j;

            final Map<String, Object> update = mkUpdate(key, instrumentId, j, bidlevel, null);

            //logger.info("TABLE UPDATE" + update);

            table.processUpdate(key, new RowWithData(key, update), clock.now());
        }
    }

    private void updateAskTable(){
        for (int j=0; j<askBook.length; j++){

            final AskLevel asklevel =  askBook[j];

            final String key = String.valueOf(instrumentId) + j;

            final Map<String, Object> update = mkUpdate(key, instrumentId, j, null, asklevel);

            //logger.info("TABLE UPDATE" + update);

            table.processUpdate(key, new RowWithData(key, update), clock.now());
        }

    }

    private void debugBidOfferCross(){
        InMemDataTable inMem = (InMemDataTable) table.asTable();
        RowData data =  inMem.pullRow("1230");
        Object bidObj = data.get("bid");
        Object offerObj = data.get("offer");

        if(offerObj != null && bidObj != null){
            long bid = (long) bidObj;
            long offer = (long) offerObj;

            if(bid != 0 && offer != 0){
                if(offer < bid){
                    System.out.println("ERROR<<<><><><");
                }
            }

            System.out.println("CheckBook: bid=" + bidObj + " offer=" + offerObj);
        }


    }

    private Map<String, Object> mkUpdate(final String key, final long instrumentId, final int level, final BidLevel bid, final AskLevel ask){

        final long bidValue = bid == null ? 0 : bid.price;
        final long bidQty = bid == null ? 0 : bid.quantity;
        final long askValue = ask == null ? 0 : ask.price;
        final long askQty = ask == null ? 0 : ask.quantity;
        return Map.of("symbolLevel", key, "level", level, "symbol", instrumentId,
                "bid", bidValue, "offer", askValue,
                "bidQuantity", bidQty, "offerQuantity", askQty);
    }

    @Override
    public void onAskBook(final AskBookUpdateDecoder askBookDec) {


        int bookLevel = 0;

        instrumentId = askBookDec.instrumentId();

        bookLevel = 0;

        for(AskBookUpdateDecoder.AskBookDecoder decoder : askBookDec.askBook()){
            final long price = decoder.price();
            final long quantity = decoder.size();

            askBook[bookLevel] = new AskLevel();
            askBook[bookLevel].setPrice(price);
            askBook[bookLevel].setQuantity(quantity);

            logger.debug("[ALGO] ASK: price:" + price + " quantity:" + quantity);

            bookLevel+= 1;
            askLength = bookLevel;
        }

        //empty the rest of the levels where no data is supplied
        if(bookLevel > 0 ) {
            emptyLevelsTo(bookLevel, 15, askBook);
        }

        updateBookTable();

        debugBidOfferCross();
    }

    @Override
    public void onBidBook(final BidBookUpdateDecoder bidBookDec) {

        empty(bidBook);

        int bookLevel = 0;

        for(BidBookUpdateDecoder.BidBookDecoder decoder : bidBookDec.bidBook()){
            final long price = decoder.price();
            final long quantity = decoder.size();
            bidBook[bookLevel] = new BidLevel();
            bidBook[bookLevel].setPrice(price);
            bidBook[bookLevel].setQuantity(quantity);
            logger.debug("[ALGO] BID: price:" + price + " quantity:" + quantity);

            bookLevel+= 1;
            bidLength = bookLevel;

        }

        //empty the rest of the levels where no data is supplied
        if(bookLevel > 0 ){
            emptyLevelsTo(bookLevel, 15, bidBook);
        }

        updateBookTable();

        debugBidOfferCross();
    }

    @Override
    public void doStart() {
        network.addConsumer(this);
    }

    @Override
    public void doStop() {

    }

    @Override
    public void doInitialize() {

    }

    @Override
    public void doDestroy() {

    }

    @Override
    public String lifecycleId() {
        return null;
    }

    @Override
    public String toString() {
        return Provider.super.toString();
    }

    @Override
    public void subscribe(String key) {

    }
}
