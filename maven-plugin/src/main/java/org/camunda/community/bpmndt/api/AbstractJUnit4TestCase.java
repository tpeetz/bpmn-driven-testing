package org.camunda.community.bpmndt.api;

import static org.camunda.bpm.engine.impl.test.TestHelper.annotationDeploymentSetUp;
import static org.camunda.bpm.engine.impl.test.TestHelper.annotationDeploymentTearDown;
import static org.camunda.community.bpmndt.api.TestCaseInstance.PROCESS_ENGINE_NAME;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.community.bpmndt.api.cfg.BpmndtProcessEnginePlugin;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Abstract superclass for JUnit 4 based test cases. When a test is started, this class builds the
 * process engine, creates the test case instance and deploys the related BPMN resource.
 */
public abstract class AbstractJUnit4TestCase extends TestWatcher {

  protected TestCaseInstance instance;

  /**
   * ID of optional annotation based deployment ({@link org.camunda.bpm.engine.test.Deployment}).
   */
  private String annotationDeploymentId;

  @Override
  protected void starting(Description description) {
    ProcessEngine processEngine = ProcessEngines.getProcessEngine(PROCESS_ENGINE_NAME);

    if (processEngine == null && isSpringEnabled()) {
      String message = String.format("Spring application context must provide a process engine with name '%s'", PROCESS_ENGINE_NAME);
      throw new IllegalStateException(message);
    }
    if (processEngine == null) {
      processEngine = buildProcessEngine();
    }

    ProcessEngineTests.init(processEngine);

    instance = new TestCaseInstance();
    instance.setEnd(getEnd());
    instance.setProcessDefinitionKey(getProcessDefinitionKey());
    instance.setProcessEnd(isProcessEnd());
    instance.setProcessEngine(processEngine);
    instance.setStart(getStart());

    Class<?> testClass = description.getTestClass();

    // e.g. org.example.it.SimpleTest
    String testName = testClass.getName();
    // e.g. testExecute
    String testMethodName = description.getMethodName();
    // e.g. generated.simple.TC_startEvent__endEvent
    String testCaseName = this.getClass().getName();

    // deploy BPMN resource
    if (getBpmnResourceName() != null) {
      instance.deploy(testCaseName, getBpmnResourceName());
    } else {
      instance.deploy(testCaseName, getBpmnResource());
    }

    // e.g. SimpleTest.testExecute
    String annotationDeploymentName = String.format("%s.%s", testClass.getSimpleName(), testMethodName);

    Deployment annotationDeployment = processEngine.getRepositoryService().createDeploymentQuery()
        .deploymentName(annotationDeploymentName)
        .singleResult();

    if (annotationDeployment == null) {
      // perform optional annotation based deployment (via @Deployment) for DMN files and other resources
      annotationDeploymentId = annotationDeploymentSetUp(processEngine, testClass, testMethodName);
    }

    instance.getData().recordTest(getId(), testName, testMethodName);
  }

  @Override
  protected void succeeded(Description description) {
    instance.getData().recordTestSuccess();
  }

  @Override
  protected void failed(Throwable t, Description description) {
    if (instance == null) {
      // skip data recrod, if test setup failed
      return;
    }

    instance.getData().recordTestFailure(t);
  }

  @Override
  protected void finished(Description description) {
    Mocks.reset();

    if (instance == null) {
      // skip undeployment, if test setup failed
      return;
    }

    if (getListenerHost() != null && getListenerPort() > 0) {
      instance.sendData(getListenerHost(), getListenerPort());
    }

    // undeploy BPMN resource
    instance.undeploy();

    Class<?> testClass = description.getTestClass();

    // e.g. testExecute
    String testMethodName = description.getMethodName();

    if (annotationDeploymentId != null) {
      // undeploy annotation based deployment
      annotationDeploymentTearDown(getProcessEngine(), annotationDeploymentId, testClass, testMethodName);
    }
  }

  /**
   * Builds the process engine, used to execute the test case. The method registers custom
   * {@link ProcessEnginePlugin}s as well as the {@link BpmndtProcessEnginePlugin}, which is required
   * to configure a conform process engine.
   * 
   * @return The built process engine.
   */
  protected ProcessEngine buildProcessEngine() {
    // must be added to a new list, since the provided list may not allow modifications
    List<ProcessEnginePlugin> processEnginePlugins = new LinkedList<>(getProcessEnginePlugins());
    // BPMN Driven Testing plugin must be added at last
    processEnginePlugins.add(new BpmndtProcessEnginePlugin());

    ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
    processEngineConfiguration.setProcessEngineName(PROCESS_ENGINE_NAME);
    processEngineConfiguration.setProcessEnginePlugins(processEnginePlugins);

    return processEngineConfiguration.buildProcessEngine();
  }

  /**
   * Creates a new executor, used to specify variables, business key and/or mocks that are considered
   * during test case execution. After the specification, {@link TestCaseExecutor#execute()} is called
   * to create a new {@link ProcessInstance} and execute the test case.
   * 
   * @return The newly created executor.
   */
  public TestCaseExecutor createExecutor() {
    return new TestCaseExecutor(instance, this::execute);
  }

  /**
   * Executes the test case.
   * 
   * @param pi A process instance, created especially for the test case.
   */
  protected abstract void execute(ProcessInstance pi);

  /**
   * Returns an input stream that provides the BPMN resource with the process definition to be tested
   * - either this method or {@link #getBpmnResourceName()} must be overridden!
   * 
   * @return The BPMN resource as stream.
   */
  protected InputStream getBpmnResource() {
    return null;
  }

  /**
   * Returns the name of the BPMN resource, that provides the process definition to be tested - either
   * this method or {@link #getBpmnResource()} must be overridden!
   * 
   * @return The BPMN resource name, within {@code src/main/resources}.
   */
  protected String getBpmnResourceName() {
    return null;
  }

  /**
   * Returns the ID of the process definition deployment.
   * 
   * @return The deployment ID.
   */
  public String getDeploymentId() {
    return instance.getDeploymentId();
  }

  /**
   * Returns the ID of the test case's end activity.
   * 
   * @return The end activity ID.
   */
  public abstract String getEnd();

  /**
   * Returns the ID of the test case, which is built by concating and hashing the IDs of all
   * activities that are passed by the test case.
   * 
   * @return The test case ID.
   */
  protected String getId() {
    return null;
  }

  /**
   * Provides the hostname of the test execution listener - a TCP server.
   * 
   * @return The test execution listener host or {@code null}, if not configured.
   */
  protected String getListenerHost() {
    return null;
  }

  /**
   * Provides the port of the test execution listener - a TCP server.
   * 
   * @return The test execution listener port or {@code -1}, if not configured.
   */
  protected int getListenerPort() {
    return -1;
  }


  /**
   * Returns the key of the process definition that is tested.
   * 
   * @return The process definition key.
   */
  public abstract String getProcessDefinitionKey();

  /**
   * Returns the process engine, used to execute the test case.
   * 
   * @return The process engine.
   */
  public ProcessEngine getProcessEngine() {
    return instance.getProcessEngine();
  }

  /**
   * Provides custom {@link ProcessEnginePlugin}s to be registered when the process engine is built.
   * By default, this method return an empty list. It can be overridden by any extending class.
   * 
   * @return A list of process engine plugins to register.
   * 
   * @see #buildProcessEngine()
   */
  protected List<ProcessEnginePlugin> getProcessEnginePlugins() {
    return Collections.emptyList();
  }

  /**
   * Returns the ID of the test case's start activity.
   * 
   * @return The start activity ID.
   */
  public abstract String getStart();

  /**
   * Determines if Spring based testing is enabled or not. By default this method returns
   * {@code false}.
   * 
   * @return {@code true}, if the testing is Spring based. Otherwise {@code false}.
   */
  protected boolean isSpringEnabled() {
    return false;
  }

  /**
   * Determines if the test case's end activity ends the process or not. This is the case if the
   * activity is an end event and if the activity's parent scope is the process. By default this
   * method returns {@code true}.
   * 
   * @return {@code true}, if the test case's end activity ends the process. Otherwise {@code false}.
   */
  protected boolean isProcessEnd() {
    return true;
  }
}
