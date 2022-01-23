import React from "react";

import pluginTabState from "./PluginTabState";
import ConfigurePluginModal from "./ui/ConfigurePluginModal";

/**
 * Modeler extension that runs outside of BPMN JS.
 */
export default class ModelerExtension extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      pluginConfigShown: false,
    }

    this.defaultPluginConfig = {
      testExecutionListener: {
        enabled: false,
        host: "localhost",
        port: 8000
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

      this.setState({pluginConfig: pluginConfig});
      this._notifyPluginConfigChanged(pluginConfig);
    });

    this.getTestExecutionData = setInterval(() => {
      const { ipcRenderer } = this.props.config.backend;

      const data = ipcRenderer.sendSync("bpmndt-data");
    }, 1000);
  }

  componentWillUnmount() {
    clearTimeout(this.getTestExecutionData);
  }

  showPluginConfigModal() {
    this.setState({pluginConfigShown: true});
  }

  _handlePluginConfigChanged = (pluginConfig) => {
    const { pluginConfig } = this.state;
    pluginConfig.testExecutionListener.host = e.target.value;
    this.setState({pluginConfig: pluginConfig});
  }

  _hidePluginConfigModal = () => {
    this.setState({pluginConfigShown: false});
  }

  /**
   * Notifies the test execution listener, running in the main process, about the changed plugin
   * configuration.
   * 
   * @param {object} pluginConfig The current plugin configuration.
   */
  _notifyPluginConfigChanged(pluginConfig) {
    const { ipcRenderer } = this.props.config.backend;

    ipcRenderer.sendSync("bpmndt-config-changed", pluginConfig);
  }

  _savePluginConfig =() => {
    const { pluginConfig } = this.state;

    // save plugin configuration
    this.props.config.setForPlugin("bpmndt", "config", pluginConfig).then(() => {
      this._notifyPluginConfigChanged(pluginConfig);
    });

    this._hidePluginConfig();
  }

  render() {
    const { pluginConfig, pluginConfigModalShown } = this.state;

    if (!pluginConfigModalShown) {
      return null;
    }

    return <ConfigurePluginModal
      onHide={this._hidePluginConfigModal}
      onChange={this._handlePluginConfigChanged}
      onSave={this._savePluginConfig}
      pluginConfig={pluginConfig}
    />;
  }
}