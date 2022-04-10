import React from "react";

import {
  MARKER_END,
  MARKER_ERROR,
  MARKER_START,
  MODE_SHOW_TEST_EXECUTION
} from "../constants";

import BaseMode from "./BaseMode";

import TestExecution from "./overlay/TestExecution";

export default class ShowTestExecutionMode extends BaseMode {
  constructor(controller) {
    super(controller);

    this.id = MODE_SHOW_TEST_EXECUTION;

    this.next = {onClick: this._handleClickNext, title: "Next test execution"};
    this.prev = {onClick: this._handleClickPrev, title: "Previous test execution"};
  }

  computeInitialState(ctx) {
    // see ViewMode#_handleClickShowTestExecution
    const { testCase } = ctx;

    const { elementRegistry, testExecutionDataStore } = this.controller;

    testCase.enrich(elementRegistry);

    // get related data store item
    const item = testExecutionDataStore.getByTestCaseId(testCase.id);

    let testExecutionIndex = this.state.testExecutionIndex || -1;
    if (item.values.length !== 0 && testExecutionIndex === -1) {
      testExecutionIndex = 0;
    }

    let markers;
    if (testExecutionIndex !== -1) {
      markers = this._getMarkers(testCase, item.values[testExecutionIndex]);
    } else {
      markers = [];
    }

    let overlays;
    if (testExecutionIndex !== -1) {
      overlays = this._getOverlays(testCase, item.values[testExecutionIndex]);
    } else {
      overlays = [];
    }

    return { item, markers, overlays, testCase, testExecutionIndex };
  }

  computeViewModel() {
    const { item, testCase, testExecutionIndex } = this.state;

    if (testExecutionIndex === -1) {
      // return undefined to indicate that nothing should be rendered
      return;
    }

    const testExecutionData = item.values[testExecutionIndex];

    return {
      next: item.values.length > 1 ? this.next : undefined,
      prev: item.values.length > 1 ? this.prev : undefined,
      content: {
        centerTop: `Test execution ${testExecutionIndex + 1} / ${item.values.length}`,
        centerBottom: `${testExecutionData.testNameShortend}#${testExecutionData.testMethodName}`,
        leftTop: testCase.start,
        leftBottom: testCase.startType,
        rightTop: testCase.end,
        rightBottom: testCase.endType
      }
    }
  }

  _getMarkers(testCase, testExecutionData) {
    const { end } = testCase;

    const markers = [];

    for (const activity of testExecutionData.activities) {
      if (!activity.ended) {
        markers.push({id: activity.id, style: MARKER_ERROR});
      } else {
        markers.push({id: activity.id, style: MARKER_START});
      }
    }

    const endIndex = testExecutionData.activities.findIndex(activity => activity.id === end);
    if (endIndex === -1) {
      markers.push({id: end, style: MARKER_END});
    }

    return markers;
  }

  _getOverlays(testCase, testExecutionData) {
    const overlays = [];

    overlays.push({
      data: { testCase, testExecutionData },
      component: <TestExecution testCase={testCase} testExecutionData={testExecutionData} />,
      position: {
        top: -100,
        left: 150
      },
      type: "test-execution"
    });

    return overlays;
  }

  _handleClickNext = () => {
    this._setTestExecutionIndex(this.state.testExecutionIndex + 1);
  }
  _handleClickPrev = () => {
    this._setTestExecutionIndex(this.state.testExecutionIndex - 1);
  }

  _setTestExecutionIndex(testExecutionIndex) {
    const { item, testCase } = this.state;

    let newIndex = testExecutionIndex;
    if (testExecutionIndex > item.values.length - 1) {
      newIndex = 0;
    }
    if (testExecutionIndex < 0) {
      newIndex = item.values.length - 1;
    }

    const testExecutionData = item.values[newIndex];

    this.setState({
      markers: this._getMarkers(testCase, testExecutionData),
      overlays: this._getOverlays(testCase, testExecutionData),
      testExecutionIndex: newIndex
    });
  }
}
