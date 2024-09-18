package codingblackfemales.ui.module;

import codingblackfemales.sequencer.net.Network;
import codingblackfemales.sequencer.net.TestNetwork;
import org.finos.toolbox.time.Clock;
import org.finos.vuu.api.TableDef;
import org.finos.vuu.core.module.DefaultModule;
import org.finos.vuu.core.module.ModuleFactory;
import org.finos.vuu.core.module.TableDefContainer;
import org.finos.vuu.core.module.ViewServerModule;
import org.finos.vuu.core.table.Columns;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static scala.jdk.javaapi.CollectionConverters.asScala;

public class AlgoModule extends DefaultModule {

    public static final String NAME = "ALGO";

    public ViewServerModule create(final TableDefContainer tableDefContainer, final TestNetwork network, final Clock clock){
        return ModuleFactory.withNamespace(NAME, tableDefContainer)
                .addTable(TableDef.apply(
                                "prices",
                                "symbolLevel",
                                Columns.fromNames(asScala(asList("symbolLevel:String", "level:Int", "symbol:Long", "bid:Long", "bidQuantity:Long",  "offer:Long", "offerQuantity: Long")).toSeq()),
                                asScala(new ArrayList<String>()).toSeq()
                        ),
                        (table, vs) -> new AlgoProvider(table, network, clock)
                ).asModule();
    }
}
