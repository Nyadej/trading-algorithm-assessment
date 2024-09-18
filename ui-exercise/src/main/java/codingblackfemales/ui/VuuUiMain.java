package codingblackfemales.ui;

import codingblackfemales.algo.PassiveAlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.marketdata.api.MarketDataEncoder;
import codingblackfemales.marketdata.api.MarketDataMessage;
import codingblackfemales.marketdata.gen.RandomMarketDataGenerator;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import codingblackfemales.ui.module.AlgoModule;
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;
import org.finos.toolbox.jmx.MetricsProvider;
import org.finos.toolbox.jmx.MetricsProviderImpl;
import org.finos.toolbox.lifecycle.LifecycleContainer;
import org.finos.toolbox.time.Clock;
import org.finos.toolbox.time.DefaultClock;
import org.finos.vuu.core.*;
import org.finos.vuu.core.module.TableDefContainer;
import org.finos.vuu.core.module.ViewServerModule;
import org.finos.vuu.net.AlwaysHappyLoginValidator;
import org.finos.vuu.net.Authenticator;
import org.finos.vuu.net.LoggedInTokenValidator;
import org.finos.vuu.net.auth.AlwaysHappyAuthenticator;
import org.finos.vuu.net.http.VuuHttp2ServerOptions;
import org.finos.vuu.plugin.Plugin;
import org.finos.vuu.state.MemoryBackedVuiStateStore;
import org.finos.vuu.state.VuiStateStore;
import scala.Option;

import java.nio.ByteBuffer;

public class VuuUiMain {

    private static final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private static final BookUpdateEncoder encoder = new BookUpdateEncoder();

    private static final MarketDataEncoder marketDataEncoder = new MarketDataEncoder();

    private static void tick(final Sequencer sequencer) {

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.bidBookCount(3)
                .next().price(98L).size(100L)
                .next().price(95L).size(200L)
                .next().price(91L).size(300L);

        encoder.askBookCount(3)
                .next().price(100L).size(101L)
                .next().price(110L).size(200L)
                .next().price(115L).size(5000L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        //send it
        sequencer.onCommand(directBuffer);
    }

    private static void processMarketDataMessage(final Sequencer sequencer, final MarketDataMessage message) {
        System.out.println(message);
        final UnsafeBuffer buffer = marketDataEncoder.encode(message);
        sequencer.onCommand(buffer);
    }


    public static void main(String[] args) {

        //Vuu implicits
        final MetricsProvider metrics = new MetricsProviderImpl();
        final Clock clock = new DefaultClock();
        final LifecycleContainer lifecycle = new LifecycleContainer(clock);
        final TableDefContainer tableDefContainer = new TableDefContainer();

        //Algo Stuff
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        final AlgoContainer container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        container.setLogic(new PassiveAlgoLogic());

        network.addConsumer(new LoggingConsumer());

        //Vuu Stuff
        final VuiStateStore store = new MemoryBackedVuiStateStore(100);

        lifecycle.autoShutdownHook();

        final Authenticator authenticator = new AlwaysHappyAuthenticator();
        final LoggedInTokenValidator loginTokenValidator = new LoggedInTokenValidator();

        //final String webRoot = "";
        final String certPath = "ui-exercise/src/main/resources/certs/cert.pem";
        final String keyPath = "ui-exercise/src/main/resources/certs/key.pem";

        final VuuServerConfig config = new VuuServerConfig(
                VuuHttp2ServerOptions.apply()
                        //.withWebRoot(webRoot)
                        //if we specify a web root, it means we will serve the files from a directory on the file system
                        //if we don't the files will be served from the vuu-ui jar directly.
                        //.withWebRoot("/Users/chris/Code/vuu/vuu-ui/deployed_apps/app-vuu-example")
                        .withSsl(certPath, keyPath)
                        .withDirectoryListings(true)
                        .withPort(8443),
                VuuWebSocketOptions.apply()
                        .withUri("websocket")
                        .withWsPort(8090)
                        .withWss(certPath, keyPath, Option.apply(null))
                        .withBindAddress("0.0.0.0"),
                VuuSecurityOptions.apply()
                        .withAuthenticator(authenticator)
                        .withLoginValidator(new AlwaysHappyLoginValidator()),
                VuuThreadingOptions.apply()
                        .withTreeThreads(4)
                        .withViewPortThreads(4),
                new scala.collection.mutable.ListBuffer<ViewServerModule>().toList(),
                new scala.collection.mutable.ListBuffer<Plugin>().toList()
        ).withModule(new AlgoModule().create(tableDefContainer, network, clock));

        final VuuServer vuuServer = new VuuServer(config, lifecycle, clock, metrics);

        lifecycle.start();

        final RandomMarketDataGenerator generator = new RandomMarketDataGenerator(123L, Venue.XLON, 1_000, 100, 15);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(600);
                        final MarketDataMessage update = generator.updateBook();
                        processMarketDataMessage(sequencer, update);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).run();

        //tick(sequencer);

        vuuServer.join();
    }
}
