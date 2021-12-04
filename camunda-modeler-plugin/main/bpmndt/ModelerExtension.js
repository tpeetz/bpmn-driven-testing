import React from "react";

import { Modal } from "camunda-modeler-plugin-helpers/components";

import Button from "./ui/component/Button";
import pluginTabState from "./PluginTabState";

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
  }

  componentWillUnmount() {

  }

  showPluginConfig() {
    this.setState({pluginConfigShown: true});
  }

  _handleHostChanged = (e) => {
    const { pluginConfig } = this.state;
    pluginConfig.testExecutionListener.host = e.target.value;
    this.setState({pluginConfig: pluginConfig});
  }
  _handlePortChanged = (e) => {
    const { pluginConfig } = this.state;
    pluginConfig.testExecutionListener.port = e.target.value;
    this.setState({pluginConfig: pluginConfig});
  }
  _handleEnabled = () => {
    const { pluginConfig } = this.state;
    pluginConfig.testExecutionListener.enabled = !pluginConfig.testExecutionListener.enabled;
    this.setState({pluginConfig: pluginConfig});
  }

  _hidePluginConfig = () => {
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

    const response = ipcRenderer.sendSync("bpmndt-config-changed", pluginConfig);

    console.log(response);
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
    const { pluginConfig, pluginConfigShown } = this.state;

    if (!pluginConfigShown) {
      return null;
    }

    const { testExecutionListener } = pluginConfig;

    return (
      <Modal onClose={this._hidePluginConfig}>
        <Modal.Title>
          Configure BPMN Driven Testing Plugin
        </Modal.Title>
        <Modal.Body>
          <div class="bpmndt">
            <div className="container">
              <div className="row">
                <div className="col">
                  <span>
                    Test execution listener - a TCP server that receives data from JUnit test case executions
                  </span>
                </div>
              </div>
              <div className="row">
                <div className="col">
                  <span className="input-label">Host</span>
                  <input
                    onChange={this._handleHostChanged}
                    placeholder="Host"
                    type="text"
                    value={testExecutionListener.host || ""}
                  />
                </div>
                <div className="col">
                  <span className="input-label">Port</span>
                  <input
                    onChange={this._handlePortChanged}
                    placeholder="Port"
                    type="text"
                    value={testExecutionListener.port || ""}
                  />
                </div>
              </div>
              <div className="row">
                <div className="col">
                  {this._renderTestExecutionListenerEnabled(testExecutionListener.enabled)}
                  <span className="input-label" style={{display: "inline-block"}}>Enabled?</span>
                </div>
              </div>
            </div>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <div class="bpmndt">
            <button
              class="btn-sm btn-success"
              onClick={this._savePluginConfig}
              style={{width: "auto", marginRight: "0.5rem"}}
              type="button"
            >
              Save
            </button>
            <button
              class="btn-sm btn-secondary"
              onClick={this._hidePluginConfig}
              style={{width: "auto"}}
              type="button"
            >
              Close
            </button>
          </div>
        </Modal.Footer>
      </Modal>
    );
  }

  _renderTestExecutionListenerEnabled(enabled) {
    if (enabled) {
      return (
        <Button
          onClick={this._handleEnabled}
          small
          style="primary"
          title="Disable test execution listener"
        >
          <i className="fas fa-check"></i>
        </Button>
      )
    } else {
      return (
        <Button
          onClick={this._handleEnabled}
          small
          style="secondary"
          title="Enable test execution listener"
        >
          <i className="fas fa-check" style={{color: "#f8f8f8"}}></i>
        </Button>
      )
    }
  }
}