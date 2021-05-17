package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StringsTests {

  @Test
  void durations() {
    assertEquals("0s", Strings.toString(Duration.ZERO));
    assertEquals("0.001s", Strings.toString(Duration.ofMillis(1)));
    assertEquals("0.999s", Strings.toString(Duration.ofMillis(999)));
    assertEquals("1.001s", Strings.toString(Duration.ofMillis(1001)));
    assertEquals("1s", Strings.toString(Duration.ofSeconds(1)));
    assertEquals("59s", Strings.toString(Duration.ofSeconds(59)));
    assertEquals("1m", Strings.toString(Duration.ofSeconds(60)));
    assertEquals("1m 1s", Strings.toString(Duration.ofSeconds(61)));
  }

  @Test
  void toNumberAndPreRelease() {
    assertEquals("1", Strings.toNumberAndPreRelease(Version.parse("1")));
    assertEquals("1-ea", Strings.toNumberAndPreRelease(Version.parse("1-ea")));
    assertEquals("1-ea", Strings.toNumberAndPreRelease(Version.parse("1-ea+2")));
    assertEquals("1+ea", Strings.toNumberAndPreRelease(Version.parse("1+ea+3")));
  }

  @Nested
  class NameOfPath {
    @TestFactory
    List<DynamicTest> nameRootDirectories() {
      return StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), true)
          .map(root -> DynamicTest.dynamicTest(root.toString(), () -> nameOfRootPath(root)))
          .toList();
    }

    private static void nameOfRootPath(Path path) {
      assertEquals(0, path.getNameCount());
      assertNull(Strings.name(path));
      assertNull(Strings.nameOrElse(path, null));
      assertEquals("b", Strings.nameOrElse(path, "b"));
    }

    @Test
    void nameOfPathWithOneElement() {
      assertEquals("a", Strings.name(Path.of("a")));
      assertEquals("a", Strings.nameOrElse(Path.of("a"), "b"));
    }

    @Test
    void nameOfPathEndingWithSingleDot() {
      assertEquals("a", Strings.nameOrElse(Path.of("a/."), "b"));
    }

    @Test
    void nameOfPathWithMoreElements() {
      assertEquals("a", Strings.name(Path.of("x/y/z/a")));
      assertEquals("a", Strings.nameOrElse(Path.of("x/y/z/a"), "b"));
    }
  }

  @Nested
  class JoinPaths {
    @Test
    void joinEmptyCollection() {
      assertEquals("", Strings.join(List.of()));
    }

    @Test
    void joinSinglePath() {
      var paths = List.of(Path.of("a"));
      assertEquals("a", Strings.join(paths));
    }

    @Test
    void joinMultiplePaths() {
      var paths = List.of(Path.of("a"), Path.of("b"), Path.of("c"));
      assertEquals("a|b|c", Strings.join(paths, "|"));
    }
  }

  @Nested
  class ModuleSourcePath {
    @Test
    void modulePatternFormFromPathWithoutModulesNameFails() {
      var path = Path.of("a/b/c/module-info.java");
      var exception =
          assertThrows(FindException.class, () -> Strings.toModuleSourcePathPatternForm(path, "d"));
      assertEquals("Name 'd' not found: " + path, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
      ".               , foo/module-info.java",
      "src             , src/foo/module-info.java",
      "./*/src         , foo/src/module-info.java",
      "src/*/main/java , src/foo/main/java/module-info.java"
    })
    void modulePatternFormForModuleFoo(String expected, Path path) {
      var actual = Strings.toModuleSourcePathPatternForm(path, "foo");
      assertEquals(expected.replace('/', File.separatorChar), actual);
    }
  }
}
