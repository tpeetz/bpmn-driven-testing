import chai from "chai";

import TestCase from "../main/bpmndt/TestCase";

const expect = chai.expect;

describe("TestCase", () => {
  let testCase;

  beforeEach(() => {
    testCase = new TestCase({path: ["a", "b", "c"]});
  });

  it("get ID as MD5 hash of the path's flow node IDs", () => {
    expect(testCase.id).to.equal("900150983cd24fb0d6963f7d28e17f72");
  });

  it("should update flow node ID when it is in the path", () => {
    testCase.updateFlowNodeId("a", "x");
    testCase.updateFlowNodeId("b", "y");
    testCase.updateFlowNodeId("c", "z");

    expect(testCase.path[0]).to.equal("x");
    expect(testCase.path[1]).to.equal("y");
    expect(testCase.path[2]).to.equal("z");
  });

  it("should not update flow node ID when it is not in the path", () => {
    testCase.updateFlowNodeId("x", "y");

    expect(testCase.path[0]).to.equal("a");
    expect(testCase.path[1]).to.equal("b");
    expect(testCase.path[2]).to.equal("c");
  });
});
