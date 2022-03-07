import TestExecutionData from "./TestExecutionData";

export default class TestExecutionDataStore {
  constructor() {
    this.store = {};
    this.sessionTimestamp = -1;
  }

  addAll(rawData) {
    for (const d of rawData) {
      const data = new TestExecutionData(d);
    }
  }
}
