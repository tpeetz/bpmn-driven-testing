package org.camunda.community.bpmndt;

import org.camunda.community.bpmndt.api.JobHandler;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Strategy, used per activity when generating a test case.
 */
public interface GeneratorStrategy {

  /**
   * Adds a handler field to the class, if the activity is handled by a handler - e.g.:
   * 
   * <pre>
   * private UserTaskHandler approveUserTask;
   * </pre>
   * 
   * Otherwise no field is added.
   * 
   * @param classBuilder The class builder to use.
   */
  void addHandlerField(TypeSpec.Builder classBuilder);

  /**
   * Adds a {@link JobHandler} field to the class, if an asynchronous continuation is configured after
   * the activity - e.g.:
   * 
   * <pre>
   * private JobHandler sendMailServiceTaskAfter;
   * </pre>
   * 
   * @param classBuilder The class builder to use.
   */
  void addHandlerFieldAfter(TypeSpec.Builder classBuilder);

  /**
   * Adds a {@link JobHandler} field to the class, if an asynchronous continuation is configured
   * before the activity - e.g.:
   * 
   * <pre>
   * private JobHandler sendMailServiceTaskBefore;
   * </pre>
   * 
   * @param classBuilder The class builder to use.
   */
  void addHandlerFieldBefore(TypeSpec.Builder classBuilder);

  /**
   * Adds a "handle" method to the class, if the activity is handled by a handler - e.g.:
   * 
   * <pre>
   * public UserTaskHandler handleApproveUserTask() {
   *   return approveUserTask;
   * }
   * </pre>
   * 
   * Otherwise no field is added.
   * 
   * @param classBuilder The class builder to use.
   */
  void addHandlerMethod(TypeSpec.Builder classBuilder);

  /**
   * Adds a method to the class, which provides a {@link JobHandler}, if an asynchronous continuation
   * is configured after the activity - e.g.:
   * 
   * <pre>
   * public JobHandler handleSendMailServiceTaskAfter() {
   *   return sendMailServiceTaskAfter;
   * }
   * </pre>
   * 
   * @param classBuilder The class builder to use.
   */
  void addHandlerMethodAfter(TypeSpec.Builder classBuilder);

  /**
   * Adds a method to the class, which provides a {@link JobHandler}, if an asynchronous continuation
   * is configured before the activity - e.g.:
   * 
   * <pre>
   * public JobHandler handleSendMailServiceTaskBefore() {
   *   return sendMailServiceTaskBefore;
   * }
   * </pre>
   * 
   * @param classBuilder The class builder to use.
   */
  void addHandlerMethodBefore(TypeSpec.Builder classBuilder);

  /**
   * Adds code to the execute method, if the activity is handled by a handler and a wait state - e.g.:
   * 
   * <pre>
   * assertThat(pi).isWaitingAt("placeOrderExternalTask");
   * instance.apply(placeOrderExternalTask)
   * </pre>
   * 
   * or the previous activity is an event based gateway.
   * 
   * @param methodBuilder The method builder to use.
   */
  void applyHandler(MethodSpec.Builder methodBuilder);

  void applyHandlerAfter(MethodSpec.Builder methodBuilder);

  void applyHandlerBefore(MethodSpec.Builder methodBuilder);

  CodeBlock getHandler();

  CodeBlock getHandlerAfter();

  CodeBlock getHandlerBefore();

  /**
   * Returns the type name of the related handler or {@code Void}, if the activity is not handled by a
   * specific handler.
   * 
   * @return The handler type name e.g. {@code TypeName.get(UserTaskHandler.class)}.
   */
  TypeName getHandlerType();

  /**
   * Adds code, which asserts that the process instance has passed an activity.
   * 
   * @param methodBuilder The method builder to use.
   */
  void hasPassed(MethodSpec.Builder methodBuilder);

  void initHandler(MethodSpec.Builder methodBuilder);

  void initHandlerAfter(MethodSpec.Builder methodBuilder);

  CodeBlock initHandlerAfterStatement();

  void initHandlerBefore(MethodSpec.Builder methodBuilder);

  CodeBlock initHandlerBeforeStatement();

  CodeBlock initHandlerStatement();

  /**
   * Adds code, which asserts that the process instance is waiting at an activity.
   * 
   * @param methodBuilder The method builder to use.
   */
  void isWaitingAt(MethodSpec.Builder methodBuilder);

  /**
   * Determines if an asynchronous continuation after the activity should be handled or not.
   * 
   * @return {@code true}, if it should be handled. Otherwise {@code false}.
   */
  boolean shouldHandleAfter();

  /**
   * Determines if an asynchronous continuation before the activity should be handled or not.
   * 
   * @return {@code true}, if it should be handled. Otherwise {@code false}.
   */
  boolean shouldHandleBefore();
}
