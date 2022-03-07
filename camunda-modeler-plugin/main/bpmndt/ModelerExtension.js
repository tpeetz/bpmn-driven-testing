import React from "react";

import pluginTabState from "./PluginTabState";
import TestExecutionData from "./TestExecutionData";
import ConfigurePluginModal from "./ui/ConfigurePluginModal";

const CHANNEL_CONFIG_CHANGE = "bpmndt-config-change";
const CHANNEL_TEST_EXECUTION_DATA_SYNC = "bpmndt-test-execution-data-sync";

/**
 * Modeler extension that runs outside of BPMN JS.
 */
export default class ModelerExtension extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      pluginConfig: null,
      pluginConfigModalShown: false,
    }

    this.defaultPluginConfig = {
      testExecutionListener: {
        host: "localhost",
        port: 8000,
        enabled: false
      }
    };

    const { subscribe } = props;

    // subscribe events
    subscribe("app.activeTabChanged", (event) => {
      const { activeTab } = event;

      if (activeTab.type === "bpmn") {
        pluginTabState.setActiveTab(activeTab.id);
      } else {
        pluginTabState.disablePlugin();
      }
    });

    subscribe("bpmn.modeler.created", (event) => {
      const { modeler } = event;

      modeler.get("bpmndt").modelerExtension = this;
    });
  }

  componentDidMount() {
    // load plugin configuration
    this.props.config.getForPlugin("bpmndt", "config").then(config => {
      const pluginConfig = config || this.defaultPluginConfig;

      // test execution listener should be disabled on start
      pluginConfig.testExecutionListener.enabled = false;

      this.setState({pluginConfig: pluginConfig});
      this._notifyPluginConfigChanged(pluginConfig);
    });
  }

  componentWillUnmount() {
    this._stopTestExecutionDataSync();
  }

  showPluginConfigModal() {
    this.setState({pluginConfigModalShown: true});
  }

  _handlePluginConfigChanged = (pluginConfig) => {
    this.setState({pluginConfig: pluginConfig});

    const pluginConfigToSave = {
      testExecutionListener: {
        host: pluginConfig.testExecutionListener.host,
        port: pluginConfig.testExecutionListener.port
      }
    };

    // save plugin configuration
    this.props.config.setForPlugin("bpmndt", "config", pluginConfigToSave).then(() => {
      this._notifyPluginConfigChanged(pluginConfig);
    });

    if (pluginConfig.testExecutionListener.enabled) {
      this._startTestExecutionDataSync();
    } else {
      this._stopTestExecutionDataSync();
    }
  }

  _hidePluginConfigModal = () => {
    this.setState({pluginConfigModalShown: false});
  }

  /**
   * Notifies the test execution listener, running in the main process, about the changed plugin
   * configuration.
   * 
   * @param {object} pluginConfig The current plugin configuration.
   */
  _notifyPluginConfigChanged(pluginConfig) {
    const { ipcRenderer } = this.props.config.backend;

    ipcRenderer.sendSync(CHANNEL_CONFIG_CHANGE, pluginConfig);
  }

  _startTestExecutionDataSync() {
    const { ipcRenderer } = this.props.config.backend;

    this.getTestExecutionData = setInterval(() => {
      const rawData = ipcRenderer.sendSync(CHANNEL_TEST_EXECUTION_DATA_SYNC);
      if (rawData.length === 0) {
        return;
      }

      // TODO

      const tab = pluginTabState.getActiveTab();
      if (tab === undefined) {
        return;
      }

      tab.plugin.controller.setTestExecutionData();
    }, 2000);
  }

  _stopTestExecutionDataSync() {
    if (this.getTestExecutionData) {
      clearInterval(this.getTestExecutionData);
    }
  }

  render() {
    const { pluginConfig, pluginConfigModalShown } = this.state;

    if (!pluginConfigModalShown) {
      return null;
    }

    return <ConfigurePluginModal
      onHide={this._hidePluginConfigModal}
      onChange={this._handlePluginConfigChanged}
      pluginConfig={pluginConfig}
    />;
  }
}