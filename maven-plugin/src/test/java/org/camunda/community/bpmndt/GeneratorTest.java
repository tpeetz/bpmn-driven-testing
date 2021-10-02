package org.camunda.community.bpmndt;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.camunda.community.bpmndt.api.AbstractJUnit4TestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class GeneratorTest {

  @Rule
  public TestName testName = new TestName();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder(new File("./target"));

  private GeneratorContext ctx;
  private GeneratorResult result;
  private Generator generator;

  private Path bpmnFile;

  @Before
  public void setUp() {
    generator = new Generator(Mockito.mock(Log.class));

    ctx = new GeneratorContext();
    ctx.setBasePath(Paths.get("."));
    ctx.setMainResourcePath(Paths.get("./src/test/resources"));
    ctx.setTestSourcePath(temporaryFolder.getRoot().toPath());

    ctx.setPackageName("org.example");

    result = new GeneratorResult();

    String fileName = testName.getMethodName().replace("test", "") + ".bpmn";
    bpmnFile = ctx.getMainResourcePath().resolve("bpmn").resolve(StringUtils.uncapitalize(fileName));
  }

  /**
   * Should generate the first test case and skip the second. Since the second test case has the same
   * name as the first, which is not allowed.
   */
  @Test
  public void testDuplicateTestCaseNames() {
    generator.generateTestCases(ctx, result, bpmnFile);
    assertThat(result.getFiles(), hasSize(1));

    TypeSpec typeSpec = result.getFiles().get(0).typeSpec;
    assertThat(typeSpec.name, equalTo("TC_duplicateTestCaseNames__startEvent__endEvent"));
  }

  @Test
  public void testEmpty() {
    generator.generateTestCases(ctx, result, bpmnFile);
    assertThat(result.getFiles(), hasSize(1));

    TypeSpec typeSpec = result.getFiles().get(0).typeSpec;
    assertThat(typeSpec.name, equalTo("TC_empty__empty"));

    TypeName superclass = ClassName.get(AbstractJUnit4TestRule.class);
    assertThat(typeSpec.superclass, equalTo(superclass));
    assertThat(typeSpec.fieldSpecs, hasSize(0));
    assertThat(typeSpec.methodSpecs, hasSize(7));
    assertThat(typeSpec.methodSpecs.get(0).name, equalTo("starting"));
    assertThat(typeSpec.methodSpecs.get(1).name, equalTo("execute"));

    String code;

    code = typeSpec.methodSpecs.get(0).code.toString();
    assertThat(code, containsString("throw new java.lang.RuntimeException(\"Path is empty\");"));

    code = typeSpec.methodSpecs.get(1).code.toString();
    assertThat(code, containsString("throw new java.lang.RuntimeException(\"Path is empty\");"));
  }
  
  @Test
  public void testIncomplete() {
    generator.generateTestCases(ctx, result, bpmnFile);
    assertThat(result.getFiles(), hasSize(1));

    TypeSpec typeSpec = result.getFiles().get(0).typeSpec;
    assertThat(typeSpec.name, equalTo("TC_incomplete__incomplete"));

    TypeName superclass = ClassName.get(AbstractJUnit4TestRule.class);
    assertThat(typeSpec.superclass, equalTo(superclass));
    assertThat(typeSpec.fieldSpecs, hasSize(0));
    assertThat(typeSpec.methodSpecs, hasSize(7));
    assertThat(typeSpec.methodSpecs.get(0).name, equalTo("starting"));
    assertThat(typeSpec.methodSpecs.get(1).name, equalTo("execute"));
    
    String code;
    
    code = typeSpec.methodSpecs.get(0).code.toString();
    assertThat(code, containsString("throw new java.lang.RuntimeException(\"Path is incomplete\");"));

    code = typeSpec.methodSpecs.get(1).code.toString();
    assertThat(code, containsString("throw new java.lang.RuntimeException(\"Path is incomplete\");"));
  }

  @Test
  public void testInvalid() {
    generator.generateTestCases(ctx, result, bpmnFile);
    assertThat(result.getFiles(), hasSize(1));

    TypeSpec typeSpec = result.getFiles().get(0).typeSpec;
    assertThat(typeSpec.name, equalTo("TC_invalid__startEvent__endEvent"));

    TypeName superclass = ClassName.get(AbstractJUnit4TestRule.class);
    assertThat(typeSpec.superclass, equalTo(superclass));
    assertThat(typeSpec.fieldSpecs, hasSize(0));
    assertThat(typeSpec.methodSpecs, hasSize(7));
    assertThat(typeSpec.methodSpecs.get(0).name, equalTo("starting"));
    assertThat(typeSpec.methodSpecs.get(1).name, equalTo("execute"));

    String code;

    code = typeSpec.methodSpecs.get(0).code.toString();
    assertThat(code, containsString("// Not existing flow nodes"));
    assertThat(code, containsString("// a"));
    assertThat(code, containsString("// b"));
    assertThat(code, containsString("throw new java.lang.RuntimeException(\"Path is invalid\");"));

    code = typeSpec.methodSpecs.get(1).code.toString();
    assertThat(code, containsString("// Not existing flow nodes"));
    assertThat(code, containsString("// a"));
    assertThat(code, containsString("// b"));
    assertThat(code, containsString("throw new java.lang.RuntimeException(\"Path is invalid\");"));
  }

  @Test
  public void testHappyPath() {
    generator.generateTestCases(ctx, result, bpmnFile);
    assertThat(result.getFiles(), hasSize(1));

    TypeSpec typeSpec = result.getFiles().get(0).typeSpec;
    assertThat(typeSpec.name, equalTo("TC_happy_path__Happy_Path"));
  }

  @Test
  public void testNoTestCases() {
    generator.generateTestCases(ctx, result, bpmnFile);
    assertThat(result.getFiles(), hasSize(0));
  }

  /**
   * Tests the complete task execution.
   */
  @Test
  public void testExecute() {
    generator.generate(ctx);
    
    Predicate<String> isFile = (className) -> {
      return Files.isRegularFile(ctx.getTestSourcePath().resolve(className));
    };
    
    // test cases
    assertThat(isFile.test("org/example/TC_duplicateTestCaseNames__startEvent__endEvent.java"), is(true));
    assertThat(isFile.test("org/example/TC_empty__empty.java"), is(true));
    assertThat(isFile.test("org/example/TC_happy_path__Happy_Path.java"), is(true));
    assertThat(isFile.test("org/example/TC_incomplete__incomplete.java"), is(true));
    assertThat(isFile.test("org/example/TC_invalid__startEvent__endEvent.java"), is(true));

    // should not exist, since the BPMN process provides no test cases
    assertThat(isFile.test("org/example/TC_noTestCases__startEvent__endEvent.java"), is(false));

    // API classes
    assertThat(isFile.test("org/camunda/community/bpmndt/api/AbstractJUnit4TestRule.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/CallActivityDefinition.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/CallActivityHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/EventHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/ExternalTaskHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/JobHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/TestCaseInstance.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/TestCaseExecutor.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/UserTaskHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/BpmndtCallActivityBehavior.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/BpmndtParseListener.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/BpmndtProcessEnginePlugin.java"), is(true));
  }

  /**
   * Tests the complete task execution with Spring enabled.
   */
  @Test
  public void testExecuteSpringEnabled() {
    ctx.setSpringEnabled(true);

    generator.generate(ctx);

    Predicate<String> isFile = (className) -> {
      return Files.isRegularFile(ctx.getTestSourcePath().resolve(className));
    };

    // test cases
    assertThat(isFile.test("org/example/TC_duplicateTestCaseNames__startEvent__endEvent.java"), is(true));
    assertThat(isFile.test("org/example/TC_empty__empty.java"), is(true));
    assertThat(isFile.test("org/example/TC_happy_path__Happy_Path.java"), is(true));
    assertThat(isFile.test("org/example/TC_incomplete__incomplete.java"), is(true));
    assertThat(isFile.test("org/example/TC_invalid__startEvent__endEvent.java"), is(true));

    // should not exist, since the BPMN process provides no test cases
    assertThat(isFile.test("org/example/TC_noTestCases__startEvent__endEvent.java"), is(false));

    // Spring configuration
    assertThat(isFile.test("org/example/BpmndtConfiguration.java"), is(true));

    // API classes
    assertThat(isFile.test("org/camunda/community/bpmndt/api/AbstractJUnit4SpringBasedTestRule.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/AbstractJUnit4TestRule.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/CallActivityDefinition.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/CallActivityHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/EventHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/ExternalTaskHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/JobHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/TestCaseInstance.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/TestCaseExecutor.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/UserTaskHandler.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/AbstractConfiguration.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/BpmndtCallActivityBehavior.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/BpmndtParseListener.java"), is(true));
    assertThat(isFile.test("org/camunda/community/bpmndt/api/cfg/BpmndtProcessEnginePlugin.java"), is(true));
  }
}