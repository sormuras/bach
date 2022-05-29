package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModuleSourcePathSupportTests {

  @Nested
  class PatternFormTests {
    @ParameterizedTest
    @CsvSource(
        textBlock =
            """
            .                 , foo               , foo
            ./*/src           , foo/src           , foo
            ./*/src/main      , foo/src/main      , foo
            ./*/src/main/java , foo/src/main/java , foo
            src               , src/foo           , foo
            src/*/java        , src/foo/java      , foo
            """)
    void test(String expected, Path info, String module) {
      expected = expected.replace('/', File.separatorChar);
      var actual = ModuleSourcePathSupport.toPatternForm(info, module);
      assertEquals(expected, actual);
    }

    @Test
    void throwsOnModuleNameNotBeingPartOfPath() {
      assertThrows(
          FindException.class,
          () -> ModuleSourcePathSupport.toPatternForm(Path.of("src/main"), "foo"));
    }
  }

  @Nested
  class SpecificFormTests {
    @ParameterizedTest
    @CsvSource(
        textBlock =
            """
            foo=foo           , foo               , foo
            foo=foo/src       , foo/src           , foo
            foo=src/foo       , src/foo           , foo
            """)
    void test(String expected, Path info, String module) {
      expected = expected.replace('/', File.separatorChar);
      var actual = ModuleSourcePathSupport.toSpecificForm(module, List.of(info));
      assertEquals(expected, actual);
    }
  }

  @Test
  void empty() {
    assertTrue(ModuleSourcePathSupport.compute(Map.of(), false).isEmpty());
    assertTrue(ModuleSourcePathSupport.compute(Map.of(), true).isEmpty());
  }

  @Test
  void foo() {
    var map = Map.of("foo", List.of(Path.of("foo")));
    assertLinesMatch(
        """
        .
        """.lines(), ModuleSourcePathSupport.compute(map, false).stream());
    assertLinesMatch(
        """
        foo=foo
        """.lines(),
        ModuleSourcePathSupport.compute(map, true).stream());
  }

  @Test
  void fooAndBar() {
    var map =
        Map.of(
            "foo", List.of(Path.of("foo")),
            "bar", List.of(Path.of("bar")));
    assertLinesMatch(
        """
        .
        """.lines(), ModuleSourcePathSupport.compute(map, false).stream());
    assertLinesMatch(
        """
        bar=bar
        foo=foo
        """.lines().sorted(),
        ModuleSourcePathSupport.compute(map, true).stream().sorted());
  }

  @Test
  void fooInSrc() {
    var map = Map.of("foo", List.of(Path.of("src")));
    assertLinesMatch(
        """
        foo=src
        """.lines(),
        ModuleSourcePathSupport.compute(map, false).stream());
    assertLinesMatch(
        """
        foo=src
        """.lines().sorted(),
        ModuleSourcePathSupport.compute(map, true).stream().sorted());
  }
}
