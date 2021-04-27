package test.integration.api.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.api.external.JUnitPlatformModuleLocator;
import org.junit.jupiter.api.Test;

class JUnitPlatformModuleLocatorTests {
  @Test
  void defaults() {
    var junit = new JUnitPlatformModuleLocator("0");
    assertEquals("org.junit.platform[*] -> JUnit Platform 0", junit.title());
    assertSame(ExternalModuleLocator.Stability.UNKNOWN, junit.stability());
  }
}
