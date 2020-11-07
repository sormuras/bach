package test.modules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Http;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectBuilder;
import com.github.sormuras.bach.project.Base;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectBuilderTests {
  @Test
  void buildInEmptyDirectory(@TempDir Path temp) {
    var baos = new ByteArrayOutputStream();
    var log = new PrintStream(baos, true, StandardCharsets.UTF_8);
    var bach = new Bach(log, Http::newClient);

    var base = Base.of(temp);
    var project = Project.of(base);

    assertEquals(temp.getFileName().toString(), project.name());
    assertTrue(project.main().modules().isEmpty());
    assertFalse(project.main().isPresent());
    assertEquals(Runtime.version().feature(), project.main().release());
    assertEquals(List.of("*/main/java", "*/main/java-module"), project.main().moduleSourcePaths());
    assertEquals(List.of("-encoding", "UTF-8"), project.main().tweaks().get("javac"));

    var builder = new ProjectBuilder(bach, project);
    assertDoesNotThrow(builder::build);

    assertLinesMatch("""
        Work on Project.+
        """.lines(), baos.toString().lines());
  }

  @ParameterizedTest
  @ValueSource(strings = {"canonical"})
  void build(String name) {
    var baos = new ByteArrayOutputStream();
    var log = new PrintStream(baos, true, StandardCharsets.UTF_8);
    var bach = new Bach(log, Http::newClient);

    var base = Base.of("test.projects", name);
    var project = Project.of(base);

    assertEquals(name, project.name());
    assertEquals(List.of("foo"), project.main().modules(), project.toString());

    var builder = new ProjectBuilder(bach, project);
    assertDoesNotThrow(builder::build);
  }
}
