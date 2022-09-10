package org.camunda.community.bpmndt.cmd;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.community.bpmndt.GeneratorResult;
import org.camunda.community.bpmndt.GeneratorStrategy;
import org.camunda.community.bpmndt.TestCaseActivity;
import org.camunda.community.bpmndt.TestCaseActivityScope;
import org.camunda.community.bpmndt.TestCaseActivityType;
import org.camunda.community.bpmndt.api.JobHandler;
import org.camunda.community.bpmndt.api.MultiInstanceScopeHandler;
import org.camunda.community.bpmndt.api.TestCaseInstance;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class GenerateMultiInstanceScopeHandler implements Consumer<TestCaseActivity> {

  private static final String SUFFIX_AFTER = "After";
  private static final String SUFFIX_BEFORE = "Before";

  private final GeneratorResult result;

  private final List<HandlerMethod> handlerMethods;

  public GenerateMultiInstanceScopeHandler(GeneratorResult result) {
    this.result = result;

    handlerMethods = new LinkedList<>();

    add(Modifier.PROTECTED, "create%sHandler%s", "$L", this::createHandler, this::addLoopIndex);
    add(Modifier.PROTECTED, "get%sHandler%s", "$LHandlers$L.getOrDefault(loopIndex, handle$L())", this::getHandler, this::addLoopIndex);
    add(Modifier.PUBLIC, "handle%s%s", "handle$L(-1)", this::handleDefault, null);
    add(Modifier.PUBLIC, "handle%s%s", "$LHandlers$L.computeIfAbsent(loopIndex, this::create$LHandler$L)", this::handle, this::addLoopIndex);
  }

  @Override
  public void accept(TestCaseActivity activity) {
    TestCaseActivityScope scope = (TestCaseActivityScope) activity;

    ClassName className = (ClassName) scope.getStrategy().getHandlerType();

    // e.g. MyScopeHandler extends MultiInstanceScopeHandler<MyScopeHandler>
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
        .addJavadoc("Multi instance scope handler for $L: $L", activity.getTypeName(), activity.getId())
        .superclass(getSuperClass(scope))
        .addModifiers(Modifier.PUBLIC);

    addHandlerFields(scope, classBuilder);

    classBuilder.addMethod(buildConstructor(scope));
    classBuilder.addMethod(buildApply(scope));

    addHandlerMethods(scope, classBuilder);

    if (!activity.getMultiInstance().isSequential()) {
      // override to return false, because it is parallel
      classBuilder.addMethod(buildIsSequential());
    }

    JavaFile javaFile = JavaFile.builder(className.packageName(), classBuilder.build())
        .skipJavaLangImports(true)
        .build();

    result.addFile(javaFile);
  }

  private void add(Modifier modifier, String name, String statement, BiFunction<TestCaseActivity, String, Object[]> statementArgs,
      Consumer<MethodSpec.Builder> customizer) {
    HandlerMethod handlerMethod = new HandlerMethod();
    handlerMethod.modifier = modifier;
    handlerMethod.name = name;
    handlerMethod.statement = statement;
    handlerMethod.statementArgs = statementArgs;
    handlerMethod.customizer = customizer;

    handlerMethods.add(handlerMethod);
  }

  protected void addHandlerFields(TestCaseActivityScope scope, TypeSpec.Builder classBuilder) {
    for (TestCaseActivity activity : scope.getActivities()) {
      if (activity.getType() == TestCaseActivityType.OTHER) {
        continue;
      }

      if (activity.getStrategy().shouldHandleBefore()) {
        addHandlerField(classBuilder, String.format("%sHandlersBefore", activity.getLiteral()), TypeName.get(JobHandler.class));
      }

      addHandlerField(classBuilder, String.format("%sHandlers", activity.getLiteral()), activity.getStrategy().getHandlerType());

      if (activity.getStrategy().shouldHandleAfter()) {
        addHandlerField(classBuilder, String.format("%sHandlersAfter", activity.getLiteral()), TypeName.get(JobHandler.class));
      }
    }
  }

  private void addHandlerField(TypeSpec.Builder classBuilder, String name, TypeName typeName) {
    classBuilder.addField(ParameterizedTypeName.get(ClassName.get(Map.class), typeName), name, Modifier.PRIVATE, Modifier.FINAL);
  }

  protected void addHandlerMethods(TestCaseActivityScope scope, TypeSpec.Builder classBuilder) {
    // createHandler
    for (TestCaseActivity activity : scope.getActivities()) {
      if (activity.getStrategy().shouldHandleBefore()) {
        classBuilder.addMethod(buildCreateHandler(activity, SUFFIX_BEFORE));
      }

      if (activity.getType() != TestCaseActivityType.OTHER) {
        classBuilder.addMethod(buildCreateHandler(activity, StringUtils.EMPTY));
      }

      if (activity.getStrategy().shouldHandleAfter()) {
        classBuilder.addMethod(buildCreateHandler(activity, SUFFIX_AFTER));
      }
    }

    // getHandler

    // handle
  }

  private void addLoopIndex(MethodSpec.Builder builder) {
    builder.addParameter(TypeName.INT, "loopIndex");
  }

  protected MethodSpec buildApply(TestCaseActivityScope scope) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("apply")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(TypeName.BOOLEAN)
        .addParameter(ProcessInstance.class, "pi")
        .addParameter(TypeName.INT, "loopIndex");

    new BuildTestCaseExecution().accept(scope.getActivities(), builder);

    return builder.build();
  }

  protected MethodSpec buildCreateHandler(TestCaseActivity activity, String suffix) {
    GeneratorStrategy strategy = activity.getStrategy();

    CodeBlock initHandlerStatement;
    if (suffix.isEmpty()) {
      initHandlerStatement = strategy.initHandlerStatement();
    } else if (SUFFIX_AFTER.equals(suffix)) {
      initHandlerStatement = strategy.initHandlerAfterStatement();
    } else {
      initHandlerStatement = strategy.initHandlerBeforeStatement();
    }

    return MethodSpec.methodBuilder(String.format("create%sHandler%s", StringUtils.capitalize(activity.getLiteral()), suffix))
        .addModifiers(Modifier.PROTECTED)
        .returns(suffix.isEmpty() ? strategy.getHandlerType() : TypeName.get(JobHandler.class))
        .addParameter(TypeName.INT, "loopIndex")
        .addStatement("return $L", initHandlerStatement)
        .build();
  }

  protected MethodSpec buildConstructor(TestCaseActivityScope scope) {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(TestCaseInstance.class, "instance")
        .addParameter(String.class, "activityId")
        .addStatement("super(instance, activityId)");

    for (TestCaseActivity activity : scope.getActivities()) {
      if (activity.getType() == TestCaseActivityType.OTHER) {
        continue;
      }

      GeneratorStrategy strategy = activity.getStrategy();

      builder.addCode("\n// $L: $L\n", activity.getTypeName(), activity.getId());

      if (strategy.shouldHandleBefore()) {
        builder.addStatement("$LHandlersBefore = new $T<>()", activity.getLiteral(), HashMap.class);
      }

      builder.addStatement("$LHandlers = new $T<>()", activity.getLiteral(), HashMap.class);

      if (strategy.shouldHandleAfter()) {
        builder.addStatement("$LHandlersAfter = new $T<>()", activity.getLiteral(), HashMap.class);
      }
    }

    return builder.build();
  }

  protected MethodSpec buildIsSequential() {
    return MethodSpec.methodBuilder("isSequential")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(TypeName.BOOLEAN)
        .addStatement("return false")
        .build();
  }

  private Object[] createHandler(TestCaseActivity activity, String suffix) {
    if (suffix.isEmpty()) {
      return ArrayUtils.toArray(activity.getStrategy().initHandlerStatement());
    } else {
      return ArrayUtils.toArray(activity.getStrategy().initHandlerAfterStatement());
    }
  }

  private Object[] getHandler(TestCaseActivity activity, String suffix) {
    return ArrayUtils.toArray(activity.getLiteral(), suffix, StringUtils.capitalize(activity.getLiteral()));
  }

  protected TypeName getSuperClass(TestCaseActivityScope scope) {
    // e.g. MultiInstanceScopeHandler<MyScopeHandler>
    return ParameterizedTypeName.get(ClassName.get(MultiInstanceScopeHandler.class), scope.getStrategy().getHandlerType());
  }

  private Object[] handleDefault(TestCaseActivity activity, String suffix) {
    return ArrayUtils.toArray(String.format("%s%s", StringUtils.capitalize(activity.getLiteral()), suffix));
  }

  private Object[] handle(TestCaseActivity activity, String suffix) {
    return ArrayUtils.toArray(activity.getLiteral(), suffix, StringUtils.capitalize(activity.getLiteral()), suffix);
  }

  private static class HandlerMethod {

    private Modifier modifier;
    private String name;
    private String statement;
    private BiFunction<TestCaseActivity, String, Object[]> statementArgs;
    private Consumer<MethodSpec.Builder> customizer;

    private void build(TestCaseActivity activity, TypeSpec.Builder classBuilder) {
      GeneratorStrategy strategy = activity.getStrategy();

      if (strategy.shouldHandleBefore()) {
        classBuilder.addMethod(build(activity, "Before"));
      }

      classBuilder.addMethod(build(activity, ""));

      if (strategy.shouldHandleAfter()) {
        classBuilder.addMethod(build(activity, "After"));
      }
    }

    private MethodSpec build(TestCaseActivity activity, String suffix) {
      GeneratorStrategy strategy = activity.getStrategy();

      String literal = StringUtils.capitalize(activity.getLiteral());

      MethodSpec.Builder builder = MethodSpec.methodBuilder(String.format(name, literal, suffix))
          .addModifiers(modifier)
          .returns(suffix.isEmpty() ? TypeName.get(JobHandler.class) : strategy.getHandlerType())
          .addStatement(String.format("return %s", statement), statementArgs.apply(activity, suffix));

      if (customizer != null) {
        customizer.accept(builder);
      }

      return builder.build();
    }
  }
}
