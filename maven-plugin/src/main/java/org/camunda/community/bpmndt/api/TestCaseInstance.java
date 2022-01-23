package org.camunda.community.bpmndt.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.community.bpmndt.api.cfg.BpmndtProcessEnginePlugin;

/**
 * Generic test case instance, which performs the deployment of the related process definition and
 * applies handlers during execution.
 */
public class TestCaseInstance {

  /** Name of the process engine to use. */
  public static final String PROCESS_ENGINE_NAME = "bpmndt";

  private final Map<String, CallActivityHandler> callActivityHandlerMap;

  private final TestExecutionData data;

  private ProcessEngine processEngine;

  /** Key of the test case related process definition. */
  private String processDefinitionKey;

  private String start;
  private String end;

  private boolean processEnd;

  /** ID of BPMN resource deployment. */
  private String deploymentId;

  private ProcessInstance pi;

  public TestCaseInstance() {
    callActivityHandlerMap = new HashMap<>(4);

    data = new TestExecutionData();
  }

  /**
   * Announces the test case instance by providing a reference to the
   * {@link BpmndtProcessEnginePlugin}, which makes the reference available when the BPMN model is
   * parsed.
   */
  protected void announce() {
    findProcessEnginePlugin().ifPresent((parseListener) -> parseListener.setInstance(this));
  }

  public void apply(EventHandler handler) {
    handler.apply(pi);
  }

  public void apply(ExternalTaskHandler handler) {
    handler.apply(pi);
  }

  public void apply(JobHandler handler) {
    handler.apply(pi);
  }

  public void apply(MultiInstanceHandler<?, ?> handler) {
    handler.apply(pi);
  }

  public void apply(UserTaskHandler handler) {
    handler.apply(pi);
  }

  protected void deploy(String deploymentName, InputStream bpmnResource) {
    this.announce();

    RepositoryService repositoryService = processEngine.getRepositoryService();

    Deployment deployment = repositoryService.createDeployment()
        .name(deploymentName)
        .addInputStream(String.format("%s.bpmn", getProcessDefinitionKey()), bpmnResource)
        .enableDuplicateFiltering(false)
        .deploy();

    deploymentId = deployment.getId();
  }

  protected void deploy(String deploymentName, String bpmnResourceName) {
    this.announce();

    RepositoryService repositoryService = processEngine.getRepositoryService();

    Deployment deployment = repositoryService.createDeployment()
        .name(deploymentName)
        .addClasspathResource(bpmnResourceName)
        .enableDuplicateFiltering(false)
        .deploy();

    deploymentId = deployment.getId();
  }

  /**
   * Executes a stubbed call activity using a {@link CallActivityHandler} that was registered for the
   * given activity.
   * 
   * @param execution The current execution.
   * 
   * @param behavior The call activity's original behavior.
   * 
   * @return {@code true}, if the execution should leave the call activity. {@code false}, if the
   *         execution should wait at the call activity.
   * 
   * @throws Exception If the occurrence of an error end event is simulated and the error propagation
   *         fails.
   * 
   * @see CallActivityHandler#simulateBpmnError(String, String)
   */
  public boolean execute(ActivityExecution execution, CallActivityBehavior behavior) throws Exception {
    String activityId = execution.getCurrentActivityId();

    CallActivityHandler handler = callActivityHandlerMap.get(activityId);
    if (handler == null) {
      return true;
    } else {
      return handler.execute(pi, execution, behavior);
    }
  }

  private Optional<BpmndtProcessEnginePlugin> findProcessEnginePlugin() {
    ProcessEngineConfigurationImpl processEngineConfiguration =
        (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    if (processEngineConfiguration.getProcessEnginePlugins() == null) {
      return Optional.empty();
    }

    return processEngineConfiguration.getProcessEnginePlugins().stream()
        .filter((processEnginePlugin) -> (processEnginePlugin instanceof BpmndtProcessEnginePlugin))
        .map(BpmndtProcessEnginePlugin.class::cast)
        .findFirst();
  }

  /**
   * Returns the object, used to collect data about the test execution.
   * 
   * @return The test execution data.
   */
  public TestExecutionData getData() {
    return data;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public String getEnd() {
    return end;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public ProcessEngine getProcessEngine() {
    return processEngine;
  }

  public String getStart() {
    return start;
  }

  public boolean isProcessEnd() {
    return processEnd;
  }

  protected void registerCallActivityHandler(String activityId, CallActivityHandler handler) {
    callActivityHandlerMap.put(activityId, handler);
  }

  /**
   * Sends the collected test execution data via TCP to a listener.
   * 
   * @param host The host of the test execution listener.
   * 
   * @param port The port of the test execution listener.
   * 
   * @see TestExecutionData
   */
  protected void sendData(String host, int port) {
    Socket socket;
    try {
      socket = new Socket(host, port);
    } catch (UnknownHostException e) {
      throw new RuntimeException(String.format("Data host '%s' could not be resolved", host), e);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Socket connection to '%s:%d' could not be established", host, port), e);
    }

    try (OutputStreamWriter w = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
      socket.setSoTimeout(100);

      data.write(w);
    } catch (SocketException e) {
      throw new RuntimeException("Socket connection timeout could not be set", e);
    } catch (IOException e) {
      throw new RuntimeException("Data could not be written to socket connection", e);
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        // can be ignored
      }
    }
  }

  protected void setEnd(String end) {
    this.end = end;
  }

  protected void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  protected void setProcessEnd(boolean processEnd) {
    this.processEnd = processEnd;
  }

  protected void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  protected void setProcessInstance(ProcessInstance pi) {
    this.pi = pi;
  }

  protected void setStart(String start) {
    this.start = start;
  }

  protected void undeploy() {
    callActivityHandlerMap.clear();
    data.clear();

    if (deploymentId == null) {
      return;
    }

    processEngine.getRepositoryService().deleteDeployment(deploymentId, true, true, true);
  }
}
