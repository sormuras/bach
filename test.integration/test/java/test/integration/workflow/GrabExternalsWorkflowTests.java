package test.integration.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.workflow.GrabExternalsWorkflow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrabExternalsWorkflowTests {

  @Test
  void computeRequiresInMultiSpaceMultiModuleProject(@TempDir Path temp) throws Exception {
    var foo = Files.createDirectories(temp.resolve("foo/main/java/"));
    Files.writeString(
        foo.resolve("module-info.java"),
        """
        module foo {
          requires java.base; // SYSTEM
          requires java.xml; // SYSTEM
          requires bar; // PROJECT
          requires baz.one; // EXTERNAL
        }
        """);

    var bar = Files.createDirectories(temp.resolve("bar/main/java/"));
    Files.writeString(bar.resolve("module-info.java"), """
        module bar {
          requires java.se; // SYSTEM
          requires baz.two; // EXTERNAL
        }
        """);

    var test = Files.createDirectories(temp.resolve("test/test/java"));
    Files.writeString(test.resolve("module-info.java"), """
        module test {
          requires java.se; // SYSTEM
          requires foo; // PROJECT
          requires bar; // PROJECT
          requires org.junit.jupiter; // EXTERNAL
        }
        """);

    var project =
        Project.of("ComputeRequiresInMultiSpaceMultiModuleProject", "0")
            .withSpaces(
                spaces ->
                    spaces.withSpace(
                        "main",
                        main ->
                            main.withModule(foo, module -> module)
                                .withModule(bar, module -> module))
                        .withSpace("test",
                            Set.of("main"),
                            tests -> tests.withModule(test, module -> module)));

    var bach =
        new Bach(
            Configuration.of(
                Configuration.Pathing.of(temp), Configuration.Printing.ofErrorsOnly()));
    var requires = new GrabExternalsWorkflow(bach, project).computeMissingRequiredExternalModules();

    Assertions.assertLinesMatch("""
        baz.one
        baz.two
        org.junit.jupiter
        """.lines(), requires.stream());
  }
}
