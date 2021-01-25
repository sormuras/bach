package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Finder;
import com.github.sormuras.bach.Finders;
import org.junit.jupiter.api.Test;

class FinderTests {

  @Test
  void checkModuleUri() {
    var finder = Finder.empty().link("m").toUri("u");
    var m = finder.find("m").orElseThrow();
    assertEquals("u", m.uri());
    assertFalse(m.by().lookupModule("u").isPresent());
  }

  @Test
  void checkMaven() {
    var finder = Finder.empty()
        .link("junit")
        .toMaven("junit", "junit", "4.12", maven -> maven.repository(""));
    var m = finder.find("junit").orElseThrow();
    assertEquals("u", m.uri());
  }

  @Test
  void checkJUnit570() {
    var finder = Finder.empty().with(Finders.JUnit.V_5_7_0);
    var jupiter = finder.find("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.7.0.jar"));
    var console = finder.find("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.7.0.jar"));
    var guardian = finder.find("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = finder.find("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }
}
