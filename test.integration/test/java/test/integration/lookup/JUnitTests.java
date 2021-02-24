package test.integration.lookup;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.lookup.JUnit;
import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.project.Libraries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class JUnitTests {

  private static void assertEndsWith(ModuleLookup lookup, String module, String suffix) {
    var uri = lookup.lookupUri(module).orElseThrow();
    assertTrue(uri.endsWith(suffix), "URI " + uri + " doesn't end with: " + suffix);
  }

  @ParameterizedTest
  @ValueSource(strings = {"5.0.0", "5.55.5", "5.99.99-M99"})
  void checkJUnit(String jupiter) {
    var junit = ModuleLookup.ofJUnit(jupiter);
    var platform = "1" + jupiter.substring(1);
    assertEndsWith(junit, "org.junit.jupiter", jupiter + ".jar");
    assertEndsWith(junit, "org.junit.platform.console", platform + ".jar");
    assertEndsWith(junit, "org.apiguardian.api", "1.1.1.jar");
    assertEndsWith(junit, "org.opentest4j", "1.2.0.jar");
  }

  @ParameterizedTest
  @EnumSource(JUnit.class)
  void factoryMethodReturnsEnumContantForMatchingName(JUnit constant) {
    assertSame(constant, ModuleLookup.ofJUnit(constant.name()));
    assertSame(constant, ModuleLookup.ofJUnit(constant.name().substring(2).replace('_', '.')));
  }

  @Test
  void checkJUnit580M1() {
    var libraries = Libraries.of(JUnit.V_5_8_0_M1);
    var jupiter = libraries.find("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.8.0-M1.jar"));
    var console = libraries.find("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.8.0-M1.jar"));
    var guardian = libraries.find("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = libraries.find("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }

  @Test
  void checkJUnit571() {
    var libraries = Libraries.of(JUnit.V_5_7_1);
    var jupiter = libraries.find("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.7.1.jar"));
    var console = libraries.find("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.7.1.jar"));
    var guardian = libraries.find("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = libraries.find("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }

  @Test
  void checkJUnit570() {
    var libraries = Libraries.of(JUnit.V_5_7_0);
    var jupiter = libraries.find("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.7.0.jar"));
    var console = libraries.find("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.7.0.jar"));
    var guardian = libraries.find("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = libraries.find("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }
}
