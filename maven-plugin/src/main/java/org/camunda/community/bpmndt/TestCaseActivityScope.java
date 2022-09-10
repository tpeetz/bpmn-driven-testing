package org.camunda.community.bpmndt;

import java.util.LinkedList;
import java.util.List;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;

/**
 * Used to gather activities under their parent scope, if the scope is a multi instance.
 */
public class TestCaseActivityScope extends TestCaseActivity {

  private final LinkedList<TestCaseActivity> activities;

  public TestCaseActivityScope(FlowNode flowNode, MultiInstanceLoopCharacteristics multiInstance) {
    super(flowNode, multiInstance);

    activities = new LinkedList<>();
  }

  @Override
  public boolean isScope() {
    return true;
  }

  public void addActivity(TestCaseActivity next) {
    next.setParent(this);

    if (!activities.isEmpty()) {
      TestCaseActivity prev = activities.getLast();

      prev.setNext(next);
      next.setPrev(prev);
    } else {
      next.setPrev(getPrev());
    }

    activities.add(next);
  }

  public List<TestCaseActivity> getActivities() {
    return activities;
  }
}
