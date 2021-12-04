const { ipcMain } = require("electron");

ipcMain.on("bpmndt-config-changed", (event, pluginConfig) => {
  event.returnValue = pluginConfig;
});
