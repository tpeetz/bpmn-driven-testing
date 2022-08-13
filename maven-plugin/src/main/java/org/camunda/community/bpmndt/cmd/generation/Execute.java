package org.camunda.community.bpmndt.cmd.generation;

import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.community.bpmndt.GeneratorStrategy;
import org.camunda.community.bpmndt.TestCaseActivity;
import org.camunda.community.bpmndt.TestCaseActivityType;
import org.camunda.community.bpmndt.TestCaseContext;

import com.squareup.javapoet.MethodSpec;

/**
 * Function that builds the method, which executes the actual test case.
 */
public class Execute implements Function<TestCaseContext, MethodSpec> {

  @Override
  public MethodSpec apply(TestCaseContext ctx) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("execute")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ProcessInstance.class, "pi");

    // handle possible test case errors
    if (!ctx.isValid()) {
      new HandleTestCaseErrors().accept(ctx, builder);
      return builder.build();
    }

    for (int i = 0; i < ctx.getActivities().size(); i++) {
      TestCaseActivity activity = ctx.getActivities().get(i);

      if (i != 0) {
        builder.addCode("\n");
      }

      builder.addCode("// $L: $L\n", activity.getTypeName(), activity.getId());

      GeneratorStrategy strategy = activity.getStrategy();

      if (strategy.shouldHandleBefore()) {
        strategy.applyHandlerBefore(builder);
      }

      strategy.applyHandler(builder);

      if (strategy.shouldHandleAfter()) {
        strategy.applyHandlerAfter(builder);
      }

      if (activity.hasPrev(TestCaseActivityType.EVENT_BASED_GATEWAY)) {
        // assert that event based gateway has been passed
        activity.getPrev().getStrategy().hasPassed(builder);
      }

      if (activity.getType() == TestCaseActivityType.EVENT_BASED_GATEWAY) {
        activity.getStrategy().isWaitingAt(builder);
      } else if (activity.hasNext() || activity.isProcessEnd()) {
        activity.getStrategy().hasPassed(builder);
      } else {
        // assert that process instance is waiting at the test case's last activity
        // which is not the process end
        // see BpmndtParseListener#instrumentEndActivity
        activity.getStrategy().isWaitingAt(builder);
      }
    }

    return builder.build();
  }
}
