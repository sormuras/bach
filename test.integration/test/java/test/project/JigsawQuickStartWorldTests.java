package test.project;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.BuildProgram;
import com.github.sormuras.bach.Http;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.Base;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class JigsawQuickStartWorldTests {

  @Test
  void build() {
    var name = "JigsawQuickStartWorld";
    var prefix = Files.isDirectory(Path.of("test.integration")) ? "test.integration" : "";
    var base = Base.of(prefix, "project", name);
    var project = Project.of(base);

    assertEquals(name, project.name());
    assertEquals(List.of("com.greetings", "org.astro"), project.main().modules());

    var baos = new ByteArrayOutputStream();
    var log = new PrintStream(baos, true, StandardCharsets.UTF_8);
    var bach = new Bach(log, Http::newClient);
    assertDoesNotThrow(() -> BuildProgram.build(bach, base, "build"));

    var finder = ModuleFinder.of(base.workspace("modules"));
    assertTrue(finder.find("com.greetings").isPresent());
    assertTrue(finder.find("org.astro").isPresent());
  }
}
