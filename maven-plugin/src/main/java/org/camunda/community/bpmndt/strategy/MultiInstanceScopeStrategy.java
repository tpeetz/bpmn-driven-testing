package org.camunda.community.bpmndt.strategy;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;

public class MultiInstanceScopeStrategy extends DefaultHandlerStrategy {

  private final ClassName typeName;

  public MultiInstanceScopeStrategy(ClassName typeName) {
    this.typeName = typeName;
  }

  @Override
  public void applyHandler(Builder methodBuilder) {
    methodBuilder.addStatement("instance.apply($L)", activity.getLiteral());
  }

  @Override
  protected CodeBlock buildHandlerMethodJavadoc() {
    return CodeBlock.builder().add("Returns the handler for multi instance $L: $L", activity.getTypeName(), activity.getId()).build();
  }

  @Override
  public TypeName getHandlerType() {
    return typeName;
  }
}
