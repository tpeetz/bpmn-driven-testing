import React from "react";

import { Modal } from "camunda-modeler-plugin-helpers/components";

import Button from "./component/Button";

export default class ConfigurePluginModal extends React.Component {
  _handleHostChanged = (e) => {
    const { onChange, pluginConfig } = this.props;
    pluginConfig.testExecutionListener.host = e.target.value;
    onChange(pluginConfig);
  }
  _handlePortChanged = (e) => {
    const { onChange, pluginConfig } = this.props;
    pluginConfig.testExecutionListener.port = e.target.value;
    onChange(pluginConfig);
  }
  _handleEnabled = () => {
    const { onChange, pluginConfig } = this.props;
    pluginConfig.testExecutionListener.enabled = !pluginConfig.testExecutionListener.enabled;
    onChange(pluginConfig);
  }

  
  render() {
    const { onHide, pluginConfig } = this.props;
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
                  {this.renderTestExecutionListenerEnabled(testExecutionListener.enabled)}
                  <span className="input-label" style={{display: "inline-block"}}>Enabled?</span>
                </div>
              </div>
            </div>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <div class="bpmndt">
            <button
              class="btn-sm btn-secondary"
              onClick={onHide}
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

  renderTestExecutionListenerEnabled(enabled) {
    return (
      <Button
        onClick={this._handleEnabled}
        small
        style={enabled ? "primary" : "secondary"}
        title={`${enabled ? "Disable" : "Enable"} test execution listener`}
      >
        <i className="fas fa-check" style={enabled ? {} : {color: "#f8f8f8"}}></i>
      </Button>
    )
  }
}