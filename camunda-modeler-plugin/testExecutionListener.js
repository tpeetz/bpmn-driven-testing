const { ipcMain } = require("electron");
const net = require("net");

let server;

function startServer({ host, port }) {
  server = new net.Server();

  server.on("connection", (socket) => {
    socket.setEncoding("utf8");

    socket.on("data", (data) => {
      console.log(data);
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

  event.returnValue = { server: enabled ? "startted": "stopped" };
});
