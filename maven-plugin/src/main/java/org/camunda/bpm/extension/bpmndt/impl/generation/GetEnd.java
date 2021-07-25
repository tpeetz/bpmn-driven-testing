package org.camunda.bpm.extension.bpmndt.impl.generation;

import java.util.function.BiFunction;

import javax.lang.model.element.Modifier;

import org.camunda.bpm.extension.bpmndt.GeneratorContext;
import org.camunda.bpm.extension.bpmndt.impl.GeneratorConstants;
import org.camunda.bpm.extension.bpmndt.type.TestCase;

import com.squareup.javapoet.MethodSpec;

public class GetEnd implements BiFunction<GeneratorContext, TestCase, MethodSpec> {

  @Override
  public MethodSpec apply(GeneratorContext context, TestCase testCase) {
    return MethodSpec.methodBuilder(GeneratorConstants.GET_END)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(String.class)
        .addStatement("return $S", testCase.getPath().getEnd())
        .build();
  }
}
