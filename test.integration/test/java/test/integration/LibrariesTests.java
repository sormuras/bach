package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Libraries;
import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.Maven;
import org.junit.jupiter.api.Test;

class LibrariesTests {

  @Test
  void checkModuleUri() {
    var libraries = Libraries.of(new ExternalModuleLookup("m", "u"));
    var m = libraries.find("m").orElseThrow();
    assertEquals("u", m.uri());
    assertFalse(m.by().lookupUri("u").isPresent());
  }

  @Test
  void checkMaven() {
    var libraries =
        Libraries.of(
            new ExternalModuleLookup(
                "junit",
                Maven.Joiner.of("junit", "junit", "4.13")
                    .repository(Maven.CENTRAL_REPOSITORY)
                    .classifier("alt")
                    .type("jar")
                    .toString()));
    var junit = libraries.find("junit").orElseThrow();
    assertTrue(junit.uri().endsWith("junit-4.13-alt.jar"));
    assertEquals(Maven.central("junit", "junit", "4.13", "alt"), junit.uri());
  }

  @Test
  void checkJUnit570() {
    var libraries = Libraries.of(Libraries.JUnit.V_5_7_0);
    var jupiter = libraries.find("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.7.0.jar"));
    var console = libraries.find("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.7.0.jar"));
    var guardian = libraries.find("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = libraries.find("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }

  @Test
  void checkJUnit571() {
    var libraries = Libraries.of(Libraries.JUnit.V_5_7_1);
    var jupiter = libraries.find("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.7.1.jar"));
    var console = libraries.find("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.7.1.jar"));
    var guardian = libraries.find("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = libraries.find("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }
}
