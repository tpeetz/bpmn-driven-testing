import React from "react";

import { MODE_EDIT } from "../constants";

export default class EditModal extends React.Component {
  render() {
    const { mode, state } = this.props.controller;

    if (!state.activeModes[MODE_EDIT]) {
      return null;
    }

    const { description, name } = mode.state.testCase;

    return (
      <div className="modal-wrapper">
        <div className="container">
          <div className="row">
            <div className="col-1"></div>
            <div className="col-10 box" style={{padding: "10px"}}>
              <div className="container">
                <div className="row" style={{"marginTop": "10px", "marginBottom": "10px"}}>
                  <div className="col">
                    <input
                      className="text-center"
                      onChange={mode.handleChangeName}
                      placeholder="Name"
                      type="text"
                      value={name || ""}
                    />
                  </div>
                </div>
                <div className="row" style={{"marginTop": "10px", "marginBottom": "10px"}}>
                  <div className="col">
                    <textarea
                      onChange={mode.handleChangeDescription}
                      placeholder="Description"
                      rows="3"
                      value={description || ""}
                    />
                  </div>
                </div>
              </div>
            </div>
            <div className="col-1"></div>
          </div>
        </div>
      </div>
    )
  }
}
