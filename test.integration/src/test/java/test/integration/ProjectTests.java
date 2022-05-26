package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaultProject() {
    var project = Project.ofDefaults();
    assertEquals("unnamed", project.name().toString());
    assertEquals(0, project.modules().size());
    assertLinesMatch(
        """
        Project
                        name = unnamed
                     version = 0-ea
                version.date = .+
                   modules # = 0
                init modules = []
                main modules = []
                test modules = []
        >> SPACES >>
        """
            .lines(),
        String.format("%s", project).lines());
  }

  @Test
  void customProject() {
    var base = "test.workflow/example-projects/processing-code";
    var root = Path.of(base);
    var project =
        Project.ofDefaults()
            .withModule("init", root, base + "/processor/src/init/java")
            .withModule("main", root, base + "/production/src/main/java")
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
    assertEquals(
        List.of("--enable-preview"),
        ToolCall.of("javac")
            .with(test.tweak("com.github.sormuras.bach/compile-classes::javac"))
            .arguments());
  }
}
