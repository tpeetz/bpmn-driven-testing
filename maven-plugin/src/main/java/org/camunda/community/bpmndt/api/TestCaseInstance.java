package org.camunda.community.bpmndt.api;

import java.io.InputStream;
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
import org.camunda.community.bpmndt.api.cfg.BpmndtParseListener;

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

    findParseListener(processEngine).ifPresent((parseListener) -> parseListener.setInstance(this));
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

  protected void clear() {
    findParseListener(processEngine).ifPresent((parseListener) -> parseListener.setInstance(null));

    callActivityHandlerMap.clear();
  }

  protected String deploy(String deploymentName, InputStream bpmnResource) {
    RepositoryService repositoryService = processEngine.getRepositoryService();

    Deployment deployment = repositoryService.createDeployment()
        .name(deploymentName)
        .addInputStream("test.bpmn", bpmnResource)
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

  public boolean execute(ActivityExecution execution, CallActivityBehavior behavior) throws Exception {
    String activityId = execution.getCurrentActivityId();

    CallActivityHandler handler = callActivityHandlerMap.get(activityId);
    if (handler == null) {
      return true;
    } else {
      return handler.execute(pi, execution, behavior);
    }
  }

  protected Optional<BpmndtParseListener> findParseListener(ProcessEngine processEngine) {
    ProcessEngineConfigurationImpl processEngineConfiguration =
        (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    return processEngineConfiguration.getCustomPostBPMNParseListeners().stream()
        .filter((parseListener) -> (parseListener instanceof BpmndtParseListener))
        .map(BpmndtParseListener.class::cast)
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

  protected void setProcessInstance(ProcessInstance pi) {
    this.pi = pi;
  }

  protected void undeploy(String deploymentId) {
    if (deploymentId == null) {
      return;
    }

    processEngine.getRepositoryService().deleteDeployment(deploymentId, true, true, true);
  }
}
