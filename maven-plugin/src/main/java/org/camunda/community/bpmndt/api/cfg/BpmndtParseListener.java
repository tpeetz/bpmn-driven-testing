package org.camunda.community.bpmndt.api.cfg;

import java.util.List;

import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.community.bpmndt.api.TestCaseInstance;
import org.camunda.community.bpmndt.api.TestCaseInstanceData;

/**
 * Custom BPMN parse listener that:
 * 
 * 1. Overrides {@link CallActivityBehavior}s to make test cases independent of sub processes.
 * 
 * 2. Enables asynchronous continuation for multi instance activities.
 */
public class BpmndtParseListener extends AbstractBpmnParseListener {

  /** Activity ID suffix of multi instance scopes. */
  private static final String MULTI_INSTANCE_SCOPE_SUFFIX = "#" + ActivityTypes.MULTI_INSTANCE_BODY;

  private final BpmndtProcessEnginePlugin plugin;

  public BpmndtParseListener(BpmndtProcessEnginePlugin plugin) {
    this.plugin = plugin;
  }

  protected String extractActivityId(String activityId) {
    if (activityId.endsWith(MULTI_INSTANCE_SCOPE_SUFFIX)) {
      return activityId.substring(0, activityId.length() - MULTI_INSTANCE_SCOPE_SUFFIX.length());
    } else {
      return activityId;
    }
  }

  private ActivityImpl findActivity(List<ActivityImpl> activities, String activityId) {
    for (ActivityImpl activity : activities) {
      String id = extractActivityId(activity.getId());

      if (id.equals(activityId)) {
        return activity;
      }

      ActivityImpl found = findActivity(activity.getActivities(), activityId);

      if (found != null) {
        return found;
      }
    }

    return null;
  }

  /**
   * Instruments the end activity, if it does not end the process.
   * 
   * @param activities A list of activities, which should also contain the end activity.
   */
  protected void instrumentEndActivity(List<ActivityImpl> activities) {
    if (plugin.getInstance().isProcessEnd()) {
      return;
    }

    ActivityImpl endActivity = findActivity(activities, plugin.getInstance().getEnd());
    if (endActivity != null) {
      endActivity.setAsyncAfter(true);
    }
  }

  protected void instrumentExecution(List<ActivityImpl> activities, ExecutionListener l) {
    for (ActivityImpl activity : activities) {
      activity.addListener(ExecutionListener.EVENTNAME_START, l);
      activity.addListener(ExecutionListener.EVENTNAME_END, l);

      instrumentExecution(activity.getActivities(), l);
    }
  }

  /**
   * Instruments the given activity, if it is a multi instance activity.
   * 
   * @param scope The surrounding scope.
   * 
   * @param activity The current activity.
   */
  protected void instrumentMultiInstanceActivity(ScopeImpl scope, ActivityImpl activity) {
    if (!scope.isSubProcessScope() && scope.getId().endsWith(MULTI_INSTANCE_SCOPE_SUFFIX)) {
      activity.setAsyncBefore(true);
      activity.setAsyncAfter(true);
    }
  }

  @Override
  public void parseCallActivity(Element callActivityElement, ScopeImpl scope, ActivityImpl activity) {
    CallActivityBehavior behavior = (CallActivityBehavior) activity.getActivityBehavior();

    activity.setActivityBehavior(new CustomCallActivityBehavior(plugin.getInstance(), behavior));

    // needed to verify the state before the call activity is executed
    // otherwise the process instance may not be available yet
    activity.setAsyncBefore(true);

    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseManualTask(Element manualTaskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition) {
    List<ActivityImpl> activities = processDefinition.getActivities();

    instrumentEndActivity(activities);
    instrumentExecution(activities, new CustomExecutionListener(plugin.getInstance().getData()));
  }
  
  @Override
  public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseSendTask(Element sendTaskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
    instrumentMultiInstanceActivity(scope, activity);
  }

  /**
   * Custom behavior to stub call activities for isolated testing.
   */
  private static class CustomCallActivityBehavior extends CallActivityBehavior {

    private final TestCaseInstance instance;

    /** The activity's original behavior. */
    private final CallActivityBehavior behavior;

    private CustomCallActivityBehavior(TestCaseInstance instance, CallActivityBehavior behavior) {
      this.instance = instance;
      this.behavior = behavior;
    }

    @Override
    public void execute(ActivityExecution execution) throws Exception {
      boolean shouldLeave = instance != null ? instance.execute(execution, behavior) : true;

      if (shouldLeave) {
        leave(execution);
      }
    }
  }

  private static class CustomExecutionListener implements ExecutionListener {

    private final TestCaseInstanceData data;

    private CustomExecutionListener(TestCaseInstanceData data) {
      this.data = data;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      if (execution.getEventName().equals(ExecutionListener.EVENTNAME_START)) {
        data.addActivityStart(execution.getCurrentActivityId());
      }

      if (execution.getEventName().equals(ExecutionListener.EVENTNAME_END)) {
        data.addActivityEnd(execution.getCurrentActivityId());
      }
    }
  }
}
