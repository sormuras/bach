package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.Project;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaultProject() {
    var project = Project.ofDefaults();
    assertEquals("unnamed", project.name().toString());
  }

  @Test
  void parsingDirectoryWithCustomPattern() {
    var directory = Path.of("");
    var pattern = "glob:*/src/{init,main,test}/{java,java-module}/module-info.java";
    var project = Project.ofDefaults().withWalkingDirectory(directory, pattern);

    assertEquals("bach", project.name().toString());
    assertLinesMatch(
        """
        com.github.sormuras.bach
        com.github.sormuras.bach
        test.base
        test.integration
        """
            .lines(),
        project.modules().stream().map(DeclaredModule::name).sorted());
  }
}
