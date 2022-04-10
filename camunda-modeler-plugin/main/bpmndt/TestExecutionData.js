const RECORD_SEPARATOR = '\u001E';
const VALUE_SEPARATOR = '\u0000';

export default class TestExecutionData {
  constructor(data) {
    this.activities = [];

    this._decodeData(data);
  }

  _decodeData(data) {
    for (const record of data.split(RECORD_SEPARATOR)) {
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
        const index = this.activities.findIndex(activity => activity.instanceId === values[2]);
        if (index !== -1) {
          this.activities[index].ended = true;
        }
        break;
      case "ACTIVITY_START":
        this.activities.push({id: values[1], instanceId: values[2], ended: false});
        break;
      case "BUSINESS_KEY":
        this.businessKey = values[1];
        break;
      case "PROTOCOL":
        this.version = parseInt(values[1]);
        this.sessionTimestamp = parseInt(values[2]);
        break;
      case "TEST":
        this.testCaseId = values[1];
        this.testName = values[2];
        this.testNameShortend = this._shortenTestName(values[2]);
        this.testMethodName = values[3];
        break;
      case "TEST_RESULT":
        this.testResultStatus = values[1];
        break;
      default:
        // ignore unknown record types
    }
  }

  _shortenTestName(testName) {
    const split = testName.split(".");

    for (let i = 0; i < split.length - 1; i++) {
      const l = split.reduce((sum, s) => sum + s.length, split.length - 1);
      if (l <= 50) {
        break;
      }
      split[i] = split[i].charAt(0);
    }

    return split.join(".");
  }
}
