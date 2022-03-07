const RECORD_SEPARATOR = '\u001E';
const VALUE_SEPARATOR = '\u0000';

export default class TestExecutionData {
  constructor(data) {
    this.activities = [];

    this._decodeData(data);
  }

  _decodeData(data) {
    for (const record of data.split(RECORD_SEPARATOR)) {
      console.log(record);
      this._decodeRecord(record);
    }
  }

  _decodeRecord(record) {
    const values = record.split(VALUE_SEPARATOR);

    if (values.length === 0) {
      return;
    }

    const recordType = values[0];
    switch (recordType) {
      case "ACTIVITY_END":
        this.activities[this.activities.length - 1].ended = true;
        break;
      case "ACTIVITY_START":
        this.activities.push({id: values[1], ended: false});
        break;
      case "PROTOCOL":
        this.version = parseInt(values[1]);
        this.sessionTimestamp = parseInt(values[2]);
        break;
      case "TEST":
        this.testCaseId = values[1];
        this.testName = values[2];
        this.testMethodName = values[3];
        break;
      case "TEST_RESULT":
        this.success = values[1] === "true";
        break;
      default:
        // ignore unknown record types
    }
  }
}
