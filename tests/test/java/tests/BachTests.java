package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import org.junit.jupiter.api.Test;
import tests.util.SwallowSystem;

class BachTests {
  @Test
  void classBachResidesInNamedModule() {
    assertEquals("com.github.sormuras.bach", Bach.class.getModule().getName());
  }

  @Test
  @SwallowSystem
  void tree(SwallowSystem.Streams streams) {
    Bach.tree();
    assertLinesMatch("""
        .bach
        >> MANY DOT DIRECTORIES AND THEIR FILES >>
        bach
        bach.bat
        bach.java
        >> MORE FILES >>
        README.md
        >> TESTS >>
        """.lines(), streams.lines());
  }

  @Test
  void versionOfBachIsNotNullAndNotBlack() {
    var version = Bach.version();
    assertNotNull(version);
    assertFalse(version.isBlank());
  }
}
