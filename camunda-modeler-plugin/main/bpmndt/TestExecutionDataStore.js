import TestExecutionData from "./TestExecutionData";

export default class TestExecutionDataStore {
  constructor() {
    this.store = {};
  }

  addAll(rawData) {
    for (const element of rawData) {
      const data = new TestExecutionData(element);
      if (data.testCaseId === undefined) {
        continue;
      }

      const item = this.getByTestCaseId(data.testCaseId);
      if (data.sessionTimestamp < item.sessionTimestamp) {
        continue;
      }

      if (data.sessionTimestamp > item.sessionTimestamp) {
        // remove old and add new, when it is a new session
        item.sessionTimestamp = data.sessionTimestamp;
        item.values = [];
        item.values.push(data);
      } else {
        // append new, when it is the same session
        item.values.push(data);
      }
    }
  }

  getByTestCaseId(testCaseId) {
    const { store } = this;

    let item = store[testCaseId];
    if (item === undefined) {
      item = { sessionTimestamp: -1, values: [] };
      store[testCaseId] = item;
    }
    return item;
  }
}
