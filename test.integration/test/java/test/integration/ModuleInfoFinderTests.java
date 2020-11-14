package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.module.ModuleInfoFinder;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModuleInfoFinderTests {
  @Test
  void parseProjectBach() {
    var bachMainRequires =
        """
        java.net.http
        jdk.compiler
        jdk.crypto.ec
        jdk.jartool
        jdk.javadoc
        jdk.jdeps
        jdk.jlink
        """;

    var mains = ModuleInfoFinder.of(Path.of(""), "./*/main/java");
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

    var tests = ModuleInfoFinder.of(Path.of(""), "./*/test/java", "./*/test/java-module");
    assertEquals(
        Set.of("com.github.sormuras.bach", "test.base", "test.integration"), tests.declared());
    assertLinesMatch(bachTestRequires.lines(), tests.required().stream());

    var preview = ModuleInfoFinder.of(Path.of(""), "./*/test-preview/java");
    assertEquals(Set.of(), preview.declared());
  }

  @Test
  void parseProjectJigsawQuickStartGreetings() {
    var directory = Path.of("test.integration/project/JigsawQuickStartGreetings");
    var specific = ModuleInfoFinder.of(directory, "com.greetings=x:y;com.greetings");
    assertEquals(Set.of("com.greetings"), specific.declared());

    var pattern = ModuleInfoFinder.of(directory, ".");
    assertEquals(Set.of("com.greetings"), pattern.declared());
  }

  @Test
  void parseProjectJigsawQuickStartWorld() {
    var base = Path.of("test.integration/project/JigsawQuickStartWorld");
    var modules = Set.of("com.greetings", "org.astro");

    var specific = ModuleInfoFinder.of(base, "com.greetings=com.greetings", "org.astro=org.astro");
    assertEquals(modules, specific.declared());

    var pattern = ModuleInfoFinder.of(base, ".");
    assertEquals(modules, pattern.declared());
  }
}
