const { ipcMain } = require("electron");
const net = require("net");

const CHANNEL_CONFIG_CHANGE = "bpmndt-config-change";
const CHANNEL_TEST_EXECUTION_DATA_SYNC = "bpmndt-test-execution-data-sync";

// data received from the test execution,
// which is yet not synced with the renderer process
const receivedData = [];

let server;

function log(message) {
  console.log(`bpmndt: ${message}`);
}

/**
 * Starts the execution listener (TCP server).
 * 
 * @param {object} parameters providing host and port, used for the TCP server to start. 
 */
function start({ host, port }) {
  if (server) {
    return;
  }

  server = new net.Server();

  server.on("connection", (socket) => {
    socket.setEncoding("utf8");

    socket.on("data", (data) => {
      log(`data received - length ${data.length}`);

      receivedData.push(data);
    });
  });

  server.listen(port, host, () => {
    log(`TCP server listen on ${host}:${port}`);
  });
}

/**
 * Stops the execution listener (TCP server).
 */
function stop() {
  if (!server) {
    return;
  }

  server.close(() => {
    log("TCP server stopped");
  });

  server = null;
}

ipcMain.on(CHANNEL_CONFIG_CHANGE, (event, pluginConfig) => {
  const { enabled, host, port } = pluginConfig.testExecutionListener;

  if (enabled) {
    start({ host, port });
  } else {
    stop();
  }

  event.returnValue = { server: enabled ? "started": "stopped" };
});

ipcMain.on(CHANNEL_TEST_EXECUTION_DATA_SYNC, (event) => {
  event.returnValue = receivedData.splice(0, receivedData.length);
});
