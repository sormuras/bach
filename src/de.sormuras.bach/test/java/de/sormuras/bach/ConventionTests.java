package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConventionTests {

  @Nested
  class MainClassConvention {

    @Test
    void mainClassOfBachIsPresent() {
      var module = "de.sormuras.bach";
      var info = Path.of("src", module, "main", "java", "module-info.java");
      var mainClass = Convention.mainClass(info, module);
      assertTrue(Files.isRegularFile(info), info.toUri().toString());
      assertTrue(mainClass.isPresent());
      assertEquals("de.sormuras.bach.Main", mainClass.get());
    }

    @Test
    void mainClassOfNotExistingModuleInfoIsNotPresent() {
      assertFalse(Convention.mainClass(Path.of("module-info.java"), "a.b.c").isPresent());
    }
  }

  @Nested
  class AmendJUnitTestEnginesConvention {
    @Test
    void emptyRemainsEmpty() {
      var map = new TreeMap<String, Version>();
      Convention.amendJUnitTestEngines(map);
      assertTrue(map.isEmpty());
    }

    @Test
    void junitTriggersVintageEngine() {
      var actual = new TreeMap<String, Version>();
      actual.put("junit", null);
      Convention.amendJUnitTestEngines(actual);
      var expected = new TreeMap<String, Version>();
      expected.put("junit", null);
      expected.put("org.junit.vintage.engine", null);
      assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "module={0}")
    @ValueSource(strings = {"org.junit.jupiter", "org.junit.jupiter.api"})
    void moduleTriggersJupiterEngine(String module) {
      var actual = new TreeMap<String, Version>();
      actual.put(module, null);
      Convention.amendJUnitTestEngines(actual);
      var expected = new TreeMap<String, Version>();
      expected.put(module, null);
      expected.put("org.junit.jupiter.engine", null);
      assertEquals(expected, actual);
    }
  }

  @Nested
  class AmendJUnitPlatformConsoleConvention {
    @Test
    void emptyRemainsEmpty() {
      var empty = new TreeMap<String, Version>();
      Convention.amendJUnitPlatformConsole(empty);
      assertTrue(empty.isEmpty());
    }

    @ParameterizedTest(name = "module={0}")
    @ValueSource(
        strings = {
          "org.junit.jupiter.engine",
          "org.junit.vintage.engine",
          "org.junit.platform.engine"
        })
    void moduleTriggersJUnitPlatformConsole(String module) {
      var actual = new TreeMap<String, Version>();
      actual.put(module, null);
      Convention.amendJUnitPlatformConsole(actual);
      var expected = new TreeMap<String, Version>();
      expected.put(module, null);
      expected.put("org.junit.platform.console", null);
      assertEquals(expected, actual);
    }
  }
}
