package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Project;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaultProject() {
    var project = Project.ofDefaults();
    assertEquals("unnamed", project.name().toString());
    assertEquals(0, project.modules().size());
  }

  @Test
  void customProject() {
    var project =
        Project.ofDefaults()
            .withModule("init", "doc/example-projects/processing-code/processor/src/init/java")
            .withModule("main", "doc/example-projects/processing-code/production/src/main/java")
            .withEnablePreviewFeatures("test");
    assertEquals(2, project.modules().size());
    var init = project.spaces().init();
    assertEquals(0, init.release());
    assertEquals(List.of("processor"), init.modules().names());
    var main = project.spaces().main();
    assertEquals(0, main.release());
    assertEquals(List.of("production"), main.modules().names());
    var test = project.spaces().test();
    assertEquals(Runtime.version().feature(), test.release());
    assertEquals(List.of("--enable-preview"), test.additionalCompileJavacArguments());
  }
}
