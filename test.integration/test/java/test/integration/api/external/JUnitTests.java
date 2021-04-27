package test.integration.api.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.api.external.JUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class JUnitTests {

  @Test
  void checkJUnit6() {
    var junit = JUnit.of("6", "7", "8", "9");
    assertEquals("JUnit 6", junit.title());
    assertSame(ExternalModuleLocator.Stability.UNKNOWN, junit.stability());
    assertEndsWith(junit, "org.junit.jupiter", "6.jar");
    assertEndsWith(junit, "org.junit.platform.console", "7.jar");
    assertEndsWith(junit, "org.apiguardian.api", "8.jar");
    assertEndsWith(junit, "org.opentest4j", "9.jar");
  }

  @ParameterizedTest
  @ValueSource(strings = {"5.0.0", "5.55.5", "5.99.99-M99"})
  void checkJUnit(String jupiter) {
    var junit = JUnit.of(jupiter);
    var platform = "1" + jupiter.substring(1);
    assertEndsWith(junit, "org.junit.jupiter", jupiter + ".jar");
    assertEndsWith(junit, "org.junit.platform.console", platform + ".jar");
    assertEndsWith(junit, "org.apiguardian.api", "1.1.1.jar");
    assertEndsWith(junit, "org.opentest4j", "1.2.0.jar");
  }

  @ParameterizedTest
  @EnumSource(JUnit.class)
  void factoryMethodReturnsEnumContantForMatchingName(JUnit constant) {
    var junit = JUnit.of(constant.name());
    assertSame(constant, junit);
    assertSame(constant, JUnit.of(constant.name().substring(2).replace('_', '.')));
    assertSame(ExternalModuleLocator.Stability.STABLE, junit.stability());
  }

  @Test
  void checkJUnit580M1() {
    var junit = JUnit.V_5_8_0_M1;
    assertEquals("JUnit 5.8.0-M1", junit.title());
    assertSame(ExternalModuleLocator.Stability.STABLE, junit.stability());
    var jupiter = junit.locate("org.junit.jupiter").orElseThrow();
    assertTrue(jupiter.uri().endsWith("junit-jupiter-5.8.0-M1.jar"));
    var console = junit.locate("org.junit.platform.console").orElseThrow();
    assertTrue(console.uri().endsWith("junit-platform-console-1.8.0-M1.jar"));
    var guardian = junit.locate("org.apiguardian.api").orElseThrow();
    assertTrue(guardian.uri().endsWith("apiguardian-api-1.1.1.jar"));
    var opentest = junit.locate("org.opentest4j").orElseThrow();
    assertTrue(opentest.uri().endsWith("opentest4j-1.2.0.jar"));
  }

  private static void assertEndsWith(ExternalModuleLocator locator, String module, String suffix) {
    var uri = locator.locate(module).orElseThrow().uri();
    assertTrue(uri.endsWith(suffix), "URI " + uri + " doesn't end with: " + suffix);
  }
}