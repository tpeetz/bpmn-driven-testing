package org.camunda.community.bpmndt.api;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
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

public class TestCaseInstance {

  /** Name of the process engine to use. */
  public static final String PROCESS_ENGINE_NAME = "bpmndt";

  private final ProcessEngine processEngine;

  /** Key of the test case related process definition. */
  private final String processDefinitionKey;

  private final String start;
  private final String end;

  private final Map<String, CallActivityHandler> callActivityHandlerMap;

  private ProcessInstance pi;

  public TestCaseInstance(ProcessEngine processEngine, String processDefinitionKey, String start, String end) {
    this.processEngine = processEngine;
    this.processDefinitionKey = processDefinitionKey;
    this.start = start;
    this.end = end;

    callActivityHandlerMap = new HashMap<>(4);
  }

  protected void announce(ProcessInstance pi) {
    this.pi = pi;

    // announce test case instance for custom call activity behavior
    findProcessEnginePlugin().ifPresent((processEnginePlugin) -> processEnginePlugin.setInstance(this));
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

  protected String deploy(String deploymentName, InputStream bpmnResource) {
    RepositoryService repositoryService = processEngine.getRepositoryService();

    Deployment deployment = repositoryService.createDeployment()
        .name(deploymentName)
        .addInputStream(String.format("%s.bpmn", getProcessDefinitionKey()), bpmnResource)
        .deploy();

    return deployment.getId();
  }

  protected String deploy(String deploymentName, String bpmnResourceName) {
    RepositoryService repositoryService = processEngine.getRepositoryService();

    Deployment deployment = repositoryService.createDeployment()
        .name(deploymentName)
        .addClasspathResource(bpmnResourceName)
        .deploy();

    return deployment.getId();
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

  protected void registerCallActivityHandler(String activityId, CallActivityHandler handler) {
    callActivityHandlerMap.put(activityId, handler);
  }

  protected void sendExecutionListenerData() {
    Optional<BpmndtProcessEnginePlugin> processEnginePlugin = findProcessEnginePlugin();
    if (!processEnginePlugin.isPresent()) {
      return;
    }

    List<String> data = processEnginePlugin.get().getExecutionListenerData();
    // TODO
  }

  protected void undeploy(String deploymentId) {
    callActivityHandlerMap.clear();

    if (deploymentId == null) {
      return;
    }

    processEngine.getRepositoryService().deleteDeployment(deploymentId, true, true, true);
  }
}
