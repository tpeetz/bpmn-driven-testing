import chai from "chai";

import TestExecutionData from "../main/bpmndt/TestExecutionData";

const expect = chai.expect;

describe("TestExecutionData", () => {
  it("decodes correctly", () => {
    const now = Date.now();

    const records = [];
    records.push(`PROTOCOL\u00001\u0000${now}`);
    records.push("TEST\u00002de2de15f09c2a3d7cfc38fd3341b7af\u0000org.camunda.community.bpmndt.api.MultiInstanceSequentialTest\u0000testHappyPath");
    records.push("BUSINESS_KEY\u0000bk123");
    records.push("ACTIVITY_START\u0000startEvent\u00001");
    records.push("ACTIVITY_START\u0000doA\u00002");
    records.push("ACTIVITY_END\u0000startEvent\u00001");
    records.push("TEST_RESULT\u0000SUCCESS");

    const data = new TestExecutionData(records.join("\u001E"));
    expect(data.version).to.equal(1);
    expect(data.sessionTimestamp).to.equal(now);

    expect(data.testCaseId).to.equal("2de2de15f09c2a3d7cfc38fd3341b7af");
    expect(data.testName).to.equal("org.camunda.community.bpmndt.api.MultiInstanceSequentialTest");
    expect(data.testNameShortend).to.equal("o.c.c.bpmndt.api.MultiInstanceSequentialTest");
    expect(data.testMethodName).to.equal("testHappyPath");

    expect(data.businessKey).to.equal("bk123");

    expect(data.activities).to.have.lengthOf(2);
    expect(data.activities[0].id).to.equal("startEvent");
    expect(data.activities[0].instanceId).to.equal("1");
    expect(data.activities[0].ended).to.be.true;
    expect(data.activities[1].id).to.equal("doA");
    expect(data.activities[1].instanceId).to.equal("2");
    expect(data.activities[1].ended).to.be.false;
  });
});
