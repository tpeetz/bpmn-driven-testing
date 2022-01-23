const { ipcMain } = require("electron");
const net = require("net");

const receivedData = [];

let server;

function startServer({ host, port }) {
  if (server) {
    return;
  }

  server = new net.Server();

  server.on("connection", (socket) => {
    socket.setEncoding("utf8");

    socket.on("data", (data) => {
      console.log(`bpmndt: Data received - length ${data.length}`);

      receivedData.push(data);
    });
  });

  server.listen(port, host, () => {
    console.log(`bpmndt: TCP server listen on ${host}:${port}`);
  });
}

function stopServer() {
  if (!server) {
    return;
  }

  server.close(() => {
    console.log("bpmndt: TCP server stopped");
  });
}

ipcMain.on("bpmndt-config-changed", (event, pluginConfig) => {
  const { enabled, host, port } = pluginConfig.testExecutionListener;

  if (enabled) {
    startServer({ host, port });
  } else {
    stopServer();
  }

  event.returnValue = { server: enabled ? "started": "stopped" };
});

ipcMain.on("bpmndt-data", (event) => {
  event.returnValue = receivedData.splice(0, receivedData.length);
});
