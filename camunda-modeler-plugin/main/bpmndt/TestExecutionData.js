const RECORD_SEPARATOR = '\036';
const VALUE_SEPARATOR = '\000';

export default class TestExecutionData {
  constructor(data) {
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
      case "PROTOCOL":
        this.protocol = {
          timestamp: parseInt(values[2]),
          version: parseInt(values[1])
        };
        break;
    }
  }
}
