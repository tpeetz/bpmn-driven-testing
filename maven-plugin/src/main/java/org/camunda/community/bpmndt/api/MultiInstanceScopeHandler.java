package org.camunda.community.bpmndt.api;

import java.util.function.Consumer;

import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;

/**
 * Fluent API for multi instance scopes (sub processes or transactions).
 *
 * @param <T> The generated multi instance scope handler type.
 */
public class MultiInstanceScopeHandler<T extends MultiInstanceScopeHandler<?>> {

  private static final String MSG_LOOP_COUNT = "Expected multi instance '%s' to loop %dx, but was %dx";
  private static final String MSG_SEQUENTIAL = "Expected multi instance '%s' to be %s, but was %s";

  protected final TestCaseInstance instance;

  private final String activityId;

  /** ID of the multi instance scope. */
  private final String scopeId;

  private Integer loopCount;
  private Boolean sequential;

  public MultiInstanceScopeHandler(TestCaseInstance instance, String activityId) {
    this.instance = instance;
    this.activityId = activityId;

    scopeId = String.format("%s#%s", activityId, ActivityTypes.MULTI_INSTANCE_BODY);
  }

  protected void apply(ProcessInstance pi) {
    if (sequential != null && sequential != isSequential()) {
      throw new AssertionError(String.format(MSG_SEQUENTIAL, activityId, getText(sequential), getText(isSequential())));
    }

    int loopIndex = 0;
    while (!isEnded(pi)) {
      boolean shouldContinue = apply(pi, loopIndex);

      loopIndex++;

      if (!shouldContinue) {
        break;
      }
    }

    if (loopCount != null && loopCount != loopIndex) {
      throw new AssertionError(String.format(MSG_LOOP_COUNT, activityId, loopCount, loopIndex));
    }
  }

  /**
   * Applies the multi instance loop for the given index. Please note: This method will be overriden
   * by generated multi instance scope handler classes.
   * 
   * @param pi The process instance, used to execute the test case.
   * 
   * @param loopIndex The current loop index.
   * 
   * @return {@code true}, if the multi instance execution should be continued. Otherwise
   *         {@code false}.
   */
  protected boolean apply(ProcessInstance pi, int loopIndex) {
    return true;
  }

  /**
   * Customizes the handler, using the given {@link Consumer} function. This method can be used to
   * apply a common customization needed for different test cases.
   * 
   * <pre>
   * tc.handleMultiInstanceScope().customize(this::prepareMultiInstanceScope);
   * </pre>
   * 
   * @param customizer A function that accepts a suitable {@link MultiInstanceScopeHandler}.
   * 
   * @return The handler.
   */
  @SuppressWarnings("unchecked")
  public T customize(Consumer<MultiInstanceScopeHandler<T>> customizer) {
    if (customizer != null) {
      customizer.accept(this);
    }
    return (T) this;
  }

  protected ProcessEngine getProcessEngine() {
    return instance.getProcessEngine();
  }

  private String getText(boolean sequential) {
    return sequential ? "sequential" : "parallel";
  }

  /**
   * Checks if the multi instance scope is ended or not.
   * 
   * @param pi The related process instance.
   * 
   * @return {@code true}, if the multi instance scope is ended. Otherwise {@code false}.
   */
  protected boolean isEnded(ProcessInstance pi) {
    HistoryService historyService = getProcessEngine().getHistoryService();

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(pi.getId())
        .activityId(scopeId)
        .singleResult();

    if (historicActivityInstance == null) {
      throw new AssertionError(String.format("No historic activity instance found for multi instance scope '%s'", scopeId));
    }

    return historicActivityInstance.getEndTime() != null;
  }

  /**
   * Determines if the multi instance loop is sequentially executed or not. Please note: If the multi
   * instance loop is defined as parallel, this method will be overridden by generated multi instance
   * handler classes.
   * 
   * @return {@code true}, if execution is done sequentially. {@code false}, if execution is done in
   *         parallel.
   */
  protected boolean isSequential() {
    return true;
  }

  /**
   * Verifies that the multi instance loop is executed n-times.
   * 
   * @param loopCount The expected loop count at the point of time when the multi instance scope is
   *        left (finished or terminated by a boundary event).
   * 
   * @return The handler.
   */
  @SuppressWarnings("unchecked")
  public T verifyLoopCount(int loopCount) {
    this.loopCount = loopCount;
    return (T) this;
  }

  /**
   * Verifies that the multi instance loop execution is done in parallel.
   * 
   * @return The handler.
   */
  @SuppressWarnings("unchecked")
  public T verifyParallel() {
    this.sequential = Boolean.FALSE;
    return (T) this;
  }

  /**
   * Verifies that the multi instance loop is sequentially executed.
   * 
   * @return The handler.
   */
  @SuppressWarnings("unchecked")
  public T verifySequential() {
    this.sequential = Boolean.TRUE;
    return (T) this;
  }
}
