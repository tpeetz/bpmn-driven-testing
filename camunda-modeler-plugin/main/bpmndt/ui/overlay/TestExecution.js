import React from "react";

export default class TestExecution extends React.Component {
  render() {
    const { testCase, testExecutionData } = this.props;

    return (
      <table>
        <tbody>
          <tr>
            <td><b>Test Case</b></td>
            <td>{testCase.name || `Length: ${testCase.path.length} flow nodes`}</td>
          </tr>
          <tr>
            <td><b>Test Class</b></td>
            <td>{testExecutionData.testNameShortend}</td>
          </tr>
          <tr>
            <td><b>Test Method</b></td>
            <td>{testExecutionData.testMethodName}</td>
          </tr>
          <tr>
            <td><b>Test Result</b></td>
            <td className={testExecutionData.testResultStatus.toLowerCase()}><b>{testExecutionData.testResultStatus}</b></td>
          </tr>
          <tr>
            <td><b>Test Executed At</b></td>
            <td>{new Date(testExecutionData.sessionTimestamp).toTimeString()}</td>
          </tr>
          {testExecutionData.businessKey ? <tr>
            <td><b>Business Key</b></td>
            <td>{testExecutionData.businessKey}</td>
          </tr> : null}
        </tbody>
      </table>
    )
  }
}
