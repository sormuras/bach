package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.ProjectScanner;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectScannerTests {

  @Test
  void scanJigsawQuickStartGreetings() {
    var base = Path.of("test.projects", "JigsawQuickStartGreetings");

    var expected =
        Project.of("JigsawQuickStartGreetings", "99")
            .withSpaces(
                spaces ->
                    spaces.withSpace(
                        "main",
                        main ->
                            main.withModule(
                                base.resolve("com.greetings"),
                                module -> module.withMainClass("com.greetings.Main"))));

    var bach =
        new Bach(
            Configuration.of(
                Configuration.Pathing.of(base), Configuration.Printing.ofErrorsOnly()));

    var actual =
        new ProjectScanner(bach)
            .scanProject(base)
            .withVersion("99")
            .withModuleTweak(
                "main", "com.greetings", module -> module.withMainClass("com.greetings.Main"));

    assertEquals(expected, actual);
  }
}
