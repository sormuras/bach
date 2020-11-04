package test.modules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Http;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectBuilder;
import com.github.sormuras.bach.project.Base;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectBuilderTests {
  @Test
  void buildInEmptyDirectory(@TempDir Path temp) {
    var baos = new ByteArrayOutputStream();
    var log = new PrintStream(baos, true, StandardCharsets.UTF_8);
    var bach = new Bach(log, Http::newClient);

    var base = Base.of(temp);
    var project = Project.of(base);
    var builder = new ProjectBuilder(bach, project);
    assertDoesNotThrow(builder::build);

    assertLinesMatch("""
        Work on Project.+
        """.lines(), baos.toString().lines());
  }
}
