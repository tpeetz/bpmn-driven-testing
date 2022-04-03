import {
  MARKER_END,
  MARKER_ERROR,
  MARKER_START,
  MODE_SHOW_TEST_EXECUTION
} from "../constants";

import BaseMode from "./BaseMode";

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

    let testExecutionIndex;
    if (item.values.length !== 0) {
      testExecutionIndex = 0;
    } else {
      testExecutionIndex = -1;
    }

    let markers;
    if (testExecutionIndex !== -1) {
      markers = this._getMarkers(testCase, item.values[testExecutionIndex]);
    } else {
      markers = [];
    }

    return {item: item, markers: markers, testCase: testCase, testExecutionIndex: testExecutionIndex};
  }

  computeViewModel() {
    const { item, testCase, testExecutionIndex } = this.state;

    if (testExecutionIndex === -1) {
      // return undefined to indicate that nothing should be rendered
      return;
    }

    const data = item.values[testExecutionIndex];

    return {
      next: item.values.length > 1 ? this.next : undefined,
      prev: item.values.length > 1 ? this.prev : undefined,
      content: {
        centerTop: `Test execution ${testExecutionIndex + 1} / ${item.values.length}`,
        centerBottom: `${data.testName}#${data.testMethodName}`,
        leftTop: testCase.start,
        leftBottom: testCase.startType,
        rightTop: testCase.end,
        rightBottom: testCase.endType
      }
    }
  }

  _getMarkers(testCase, data) {
    const { end, start } = testCase;

    const markers = [];

    for (const activity of data.activities) {
      if (!activity.ended) {
        markers.push({id: activity.id, style: MARKER_ERROR});
      } else {
        markers.push({id: start, style: MARKER_START});
      }
    }

    const endIndex = data.activities.findIndex(activity => activity.id === end);
    if (endIndex === -1) {
      markers.push({id: end, style: MARKER_END});
    }

    return markers;
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

    this.setState({markers: this._getMarkers(testCase, item.values[newIndex]), testExecutionIndex: newIndex});
  }
}
