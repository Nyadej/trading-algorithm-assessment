import { TableSchema } from "@vuu-ui/vuu-data-types";
import { VuuTable } from "@vuu-ui/vuu-protocol-types";

export const schemas: Readonly<Record<AlgoTableName, Readonly<TableSchema>>> = {
  prices: {
    table: {
      table: "prices",
      module: "ALGO",
    },
    columns: [
      {
        name: "bid",
        serverDataType: "long",
      },
      {
        name: "bidQuantity",
        serverDataType: "long",
      },
      {
        name: "level",
        serverDataType: "int",
      },
      {
        name: "offer",
        serverDataType: "long",
      },
      {
        name: "offerQuantity",
        serverDataType: "long",
      },
      {
        name: "symbol",
        serverDataType: "long",
      },
      {
        name: "symbolLevel",
        serverDataType: "string",
      },
    ],
    key: "symbolLevel",
  },
};

export type AlgoTableName = "prices";
export type AlgoVuuTable = {
  module: "ALGO";
  table: AlgoTableName;
};

export const isAlgoTable = (vuuTable: VuuTable): vuuTable is AlgoVuuTable =>
  vuuTable.module === "ALGO" && vuuTable.table === "prices";
