import { buildDataColumnMap, Table } from "@vuu-ui/vuu-data-test";
import { AlgoTableName, schemas } from "./algo-schemas";
import { generateMarketDepth } from "./data-utils";
import { MarketDataGenerator } from "./MarketDataGenerator";

type bid = number;
type bidQuantity = number;
type level = number;
type offer = number;
type offerQuantity = number;
type symbolId = string;
type symbolLevel = string;

export type pricesDataRow = [
  bid,
  bidQuantity,
  level,
  offer,
  offerQuantity,
  symbolId,
  symbolLevel
];

const pricesData: pricesDataRow[] = generateMarketDepth("VOD.L");

const { bid, bidQuantity, offer, offerQuantity } = buildDataColumnMap(
  schemas,
  "prices"
);
const updateGenerator = new MarketDataGenerator();

export const pricesTable = new Table(
  schemas.prices,
  pricesData,
  buildDataColumnMap<AlgoTableName>(schemas, "prices"),
  updateGenerator
);
