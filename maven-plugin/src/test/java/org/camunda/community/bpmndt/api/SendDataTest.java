package org.camunda.community.bpmndt.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests;
import org.camunda.bpm.engine.test.assertions.bpmn.ProcessInstanceAssert;
import org.camunda.community.bpmndt.api.TestExecutionData.Record;
import org.camunda.community.bpmndt.api.TestExecutionData.RecordType;
import org.camunda.community.bpmndt.test.ServerSocketRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.mockito.Mockito;

public class SendDataTest {

  @Rule
  public ServerSocketRule serverSocket = new ServerSocketRule();

  private TestCase tc;

  @Before
  public void setUp() {
    tc = new TestCase("localhost", 8000);
  }

  @Test
  public void testSendData() {
    Description description = Mockito.mock(Description.class);
    when(description.getMethodName()).thenReturn("testSendData");
    when(description.getTestClass()).then((it) -> this.getClass());

    tc.starting(description);

    tc.createExecutor()
      .withBean("doA", new DoA())
      .withBean("doB", new DoB())
      .verify(pi -> {
        pi.isEnded();
      })
      .execute();

    tc.succeeded(description);

    TestExecutionData data = tc.instance.getData();
    assertThat(data, notNullValue());

    List<Record> records = data.getRecords();
    assertThat(records, notNullValue());
    assertThat(records, hasSize(11));
    assertThat(records.get(0).getType(), is(RecordType.PROTOCOL));
    assertThat(records.get(1).getType(), is(RecordType.TEST));
    assertThat(records.get(2).getType(), is(RecordType.ACTIVITY_START));
    assertThat(records.get(3).getType(), is(RecordType.ACTIVITY_END));
    assertThat(records.get(4).getType(), is(RecordType.ACTIVITY_START));
    assertThat(records.get(5).getType(), is(RecordType.ACTIVITY_END));
    assertThat(records.get(6).getType(), is(RecordType.ACTIVITY_START));
    assertThat(records.get(7).getType(), is(RecordType.ACTIVITY_END));
    assertThat(records.get(8).getType(), is(RecordType.ACTIVITY_START));
    assertThat(records.get(9).getType(), is(RecordType.ACTIVITY_END));
    assertThat(records.get(10).getType(), is(RecordType.TEST_RESULT));

    tc.finished(description);

    String sentData = serverSocket.getData();

    String[] r = sentData.split("\036");
    assertThat(r.length, is(11));

    String[] v;

    v = r[0].split("\000");
    assertThat(v.length, is(3));
    assertThat(v[0], equalTo("PROTOCOL"));
    assertThat(v[1], equalTo("1"));
    assertThat(v[2], equalTo(String.valueOf(ManagementFactory.getRuntimeMXBean().getStartTime())));

    v = r[1].split("\000");
    assertThat(v.length, is(4));
    assertThat(v[0], equalTo("TEST"));
    assertThat(v[1], equalTo("org.camunda.community.bpmndt.api.SendDataTest"));
    assertThat(v[2], equalTo("testSendData"));
    assertThat(v[3], equalTo("org.camunda.community.bpmndt.api.SendDataTest$TestCase"));

    v = r[2].split("\000");
    assertThat(v.length, is(2));
    assertThat(v[0], equalTo("ACTIVITY_START"));
    assertThat(v[1], equalTo("startEvent"));

    v = r[3].split("\000");
    assertThat(v.length, is(2));
    assertThat(v[0], equalTo("ACTIVITY_END"));
    assertThat(v[1], equalTo("startEvent"));

    v = r[10].split("\000");
    assertThat(v.length, is(2));
    assertThat(v[0], equalTo("TEST_RESULT"));
    assertThat(v[1], equalTo("true"));
  }

  private class TestCase extends AbstractJUnit4TestCase {

    private String host;
    private int port;

    private TestCase(String host, int port) {
      this.host = host;
      this.port = port;
    }

    @Override
    protected void execute(ProcessInstance pi) {
      assertThat(pi, notNullValue());

      ProcessInstanceAssert piAssert = ProcessEngineTests.assertThat(pi);

      piAssert.hasPassed("startEvent", "doA", "doB", "endEvent").isEnded();
    }

    @Override
    protected InputStream getBpmnResource() {
      try {
        return Files.newInputStream(Paths.get("./src/test/it/advanced/src/main/resources/serviceTask.bpmn"));
      } catch (IOException e) {
        return null;
      }
    }

    @Override
    protected String getListenerHost() {
      return host;
    }

    @Override
    protected int getListenerPort() {
      return port;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "serviceTask";
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

  private static class DoA implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      // TODO Auto-generated method stub

    }
  }

  private static class DoB implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      // TODO Auto-generated method stub

    }
  }
}
