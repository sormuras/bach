package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.Maven;
import com.github.sormuras.bach.project.Libraries;
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
}
