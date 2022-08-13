package org.camunda.community.bpmndt.cmd;

import java.util.function.Consumer;

import org.camunda.community.bpmndt.GeneratorContext;
import org.camunda.community.bpmndt.GeneratorResult;
import org.camunda.community.bpmndt.TestCaseActivity;
import org.camunda.community.bpmndt.TestCaseContext;

public class GenerateMultiInstanceScopeHandler implements Consumer<TestCaseActivity> {

  private final GeneratorContext gCtx;
  private final TestCaseContext ctx;
  private final GeneratorResult result;

  public GenerateMultiInstanceScopeHandler(GeneratorContext gCtx, TestCaseContext ctx, GeneratorResult result) {
    this.gCtx = gCtx;
    this.ctx = ctx;
    this.result = result;
  }

  @Override
  public void accept(TestCaseActivity activity) {

  }
}
