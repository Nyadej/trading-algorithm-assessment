import { DataSourceProvider } from "@vuu-ui/vuu-shell";
import { ReactNode } from "react";
import {
  DataSourceConstructorProps,
  TableSchema,
} from "@vuu-ui/vuu-data-types";
import { algoModule } from "./algo-module";
import { isAlgoTable, schemas } from "./algo-schemas";
import { VuuTable, VuuTableList } from "@vuu-ui/vuu-protocol-types";

class VuuDataSource {
  constructor({ table }: DataSourceConstructorProps) {
    if (isAlgoTable(table)) {
      return algoModule.createDataSource(table.table);
    } else {
      throw Error("invalid table");
    }
  }
}

const getServerAPI = async () => ({
  getTableList: async (): Promise<VuuTableList> => {
    return {
      tables: Object.values(schemas).map((schema) => schema.table),
    };
  },
  getTableSchema: async (table: VuuTable): Promise<TableSchema> => {
    if (isAlgoTable(table)) {
      return schemas[table.table];
    } else {
      throw Error("invalid table");
    }
  },
});

export const TestDataProvider = ({ children }: { children: ReactNode }) => {
  return (
    <DataSourceProvider
      getServerAPI={getServerAPI}
      VuuDataSource={VuuDataSource as any}
    >
      {children}
    </DataSourceProvider>
  );
};
