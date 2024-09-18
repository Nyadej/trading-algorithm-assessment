import { Table, VuuModule } from "@vuu-ui/vuu-data-test";
import { AlgoTableName, schemas } from "./algo-schemas";
import { pricesTable } from "./pricesTable";

const tables: Record<AlgoTableName, Table> = {
  prices: pricesTable,
};

export const algoModule = new VuuModule<AlgoTableName>({
  name: "ALGOS",
  schemas,
  tables,
});
