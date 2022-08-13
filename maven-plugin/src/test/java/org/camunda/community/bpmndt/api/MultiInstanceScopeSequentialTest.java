package org.camunda.community.bpmndt.api;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests;
import org.camunda.bpm.engine.test.assertions.bpmn.ProcessInstanceAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MultiInstanceScopeSequentialTest {

  @Rule
  public TestCase tc = new TestCase();

  private MultiInstanceScopeHandler<?> handler;

  @Before
  public void setUp() {
    handler = new Handler(tc.instance, "multiInstanceScope");
  }

  @Test
  public void testExecute() {
    tc.createExecutor().execute();
  }

  @Test
  public void testVerify() {
    handler.verifyLoopCount(3).verifySequential();

    tc.createExecutor().execute();
  }

  private class TestCase extends AbstractJUnit4TestCase<TestCase> {

    @Override
    protected void execute(ProcessInstance pi) {
      assertThat(pi, notNullValue());

      instance.apply(handler);

      ProcessInstanceAssert piAssert = ProcessEngineTests.assertThat(pi);

      piAssert.hasPassed("startEvent", "multiInstanceScope#multiInstanceBody", "endEvent").isEnded();
    }

    @Override
    protected InputStream getBpmnResource() {
      try {
        return Files.newInputStream(Paths.get("./src/test/it/advanced-multi-instance/src/main/resources/scopeSequential.bpmn"));
      } catch (IOException e) {
        return null;
      }
    }

    @Override
    public String getProcessDefinitionKey() {
      return "scopeSequential";
    }

    @Override
    public String getStart() {
      return "startEvent";
    }

    @Override
    public String getEnd() {
      return "endEvent";
    }
  }

  private static class Handler extends MultiInstanceScopeHandler<Handler> {

    private final Map<Integer, UserTaskHandler> userTaskHandlers;
    private final Map<Integer, EventHandler> messageCatchEventHandlers;
    private final Map<Integer, CallActivityHandler> callActivityHandlers;

    public Handler(TestCaseInstance instance, String activityId) {
      super(instance, activityId);

      userTaskHandlers = new HashMap<>();
      messageCatchEventHandlers = new HashMap<>();
      callActivityHandlers = new HashMap<>();
    }

    @Override
    protected boolean apply(ProcessInstance pi, int loopIndex) {
      CallActivityHandler callActivityHandler = getCallActivityHandler(loopIndex);
      instance.registerCallActivityHandler("callActivity", callActivityHandler);

      instance.apply(getUserTaskHandler(loopIndex));
      instance.apply(getMessageCatchEventHandler(loopIndex));

      // automatic wait state before call activity
      instance.apply(new JobHandler(getProcessEngine(), "callActivity"));

      return true;
    }

    protected UserTaskHandler createUserTaskHandler(int loopIndex) {
      return new UserTaskHandler(getProcessEngine(), "userTask");
    }

    protected EventHandler createMessageCatchEventHandler(int loopIndex) {
      return new EventHandler(getProcessEngine(), "messageCatchEvent", "advancedMessage");
    }

    protected CallActivityHandler createCallActivityHandler(int loopIndex) {
      return new CallActivityHandler(instance, "callActivity");
    }

    protected UserTaskHandler getUserTaskHandler(int loopIndex) {
      return userTaskHandlers.getOrDefault(loopIndex, handleUserTask());
    }

    protected EventHandler getMessageCatchEventHandler(int loopIndex) {
      return messageCatchEventHandlers.getOrDefault(loopIndex, handleMessageCatchEvent());
    }

    protected CallActivityHandler getCallActivityHandler(int loopIndex) {
      return callActivityHandlers.getOrDefault(loopIndex, handleCallActivity());
    }

    public UserTaskHandler handleUserTask() {
      return handleUserTask(-1);
    }

    public UserTaskHandler handleUserTask(int loopIndex) {
      return userTaskHandlers.computeIfAbsent(loopIndex, this::createUserTaskHandler);
    }

    public EventHandler handleMessageCatchEvent() {
      return handleMessageCatchEvent(-1);
    }

    public EventHandler handleMessageCatchEvent(int loopIndex) {
      return messageCatchEventHandlers.computeIfAbsent(loopIndex, this::createMessageCatchEventHandler);
    }

    public CallActivityHandler handleCallActivity() {
      return handleCallActivity(-1);
    }

    public CallActivityHandler handleCallActivity(int loopIndex) {
      return callActivityHandlers.computeIfAbsent(loopIndex, this::createCallActivityHandler);
    }
  }
}
