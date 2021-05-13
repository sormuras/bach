package test.integration.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.locator.JUnitJupiterModuleLocator;
import org.junit.jupiter.api.Test;

class JUnitJupiterModuleLocatorTests {
  @Test
  void defaults() {
    var junit = new JUnitJupiterModuleLocator("0");
    assertEquals("org.junit.jupiter[*] -> JUnit Jupiter 0", junit.title());
    assertSame(ExternalModuleLocator.Stability.UNKNOWN, junit.stability());
  }
}
