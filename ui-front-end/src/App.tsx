import { Shell } from "@vuu-ui/vuu-shell";
import { layoutJSON } from "./layoutJSON";
import { AppHeader } from "./components/app-header";

import "./App.css";

function App() {
  console.log("render App");
  return (
    <Shell
      shellLayoutProps={{
        appHeader: <AppHeader />,
      }}
      workspaceProps={{
        layoutJSON,
        showTabs: false,
      }}
      className="App"
      serverUrl="ws://localhost:8090/websocket"
      user={{ token: "test", username: "guest" }}
    />
  );
}

export default App;
