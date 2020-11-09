package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.module.ModuleInfoFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModuleInfoFinderTests {
  @Test
  void parseProjectBach() {
    var bachMainRequires =
        """
        java.net.http
        jdk.compiler
        jdk.jartool
        jdk.javadoc
        jdk.jdeps
        jdk.jlink
        """;

    var mains = ModuleInfoFinder.of(Path.of("."), List.of("./*/main/java"));
    assertEquals(Set.of("com.github.sormuras.bach"), mains.declared());
    assertLinesMatch(bachMainRequires.lines(), mains.required().stream());
    var bach = mains.find("com.github.sormuras.bach").orElseThrow().descriptor();
    assertEquals("com.github.sormuras.bach.Main", bach.mainClass().orElseThrow());

    var bachTestRequires =
        """
        com.github.sormuras.bach
        """
            + bachMainRequires
            + """
        org.junit.jupiter
        test.base
        """;

    var tests = ModuleInfoFinder.of(Path.of("."), List.of("./*/test/java", "./*/test/java-module"));

    assertEquals(Set.of("com.github.sormuras.bach", "test.base", "test.modules"), tests.declared());
    assertLinesMatch(bachTestRequires.lines(), tests.required().stream());

    var preview = ModuleInfoFinder.of(Path.of(""), List.of("./*/test-preview/java"));
    assertEquals(Set.of(), preview.declared());
  }
}
