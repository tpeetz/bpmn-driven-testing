"use strict";

// run test execution listener within Electron's main process
require("./testExecutionListener");

module.exports = function(electronApp, menuState) {
  return [{
    label: "Show / Hide",
    accelerator: "CommandOrControl+T",
    enabled: function() {
      return menuState.bpmn;
    },
    action: function() {
      electronApp.emit("menu:action", "bpmndtToggle");
    }
  }, {
    label: "Configure",
    enabled: function() {
      return menuState.bpmn;
    },
    action: function() {
      electronApp.emit("menu:action", "bpmndtConfigure");
    }
  }];
};
