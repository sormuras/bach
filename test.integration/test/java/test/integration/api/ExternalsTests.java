package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocations;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.api.Externals;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExternalsTests {
  @Test
  void empty() {
    var externals = new Externals(Set.of(), List.of());
    assertTrue(externals.requires().isEmpty());
    assertTrue(externals.findExternal("m").isEmpty());
  }

  @Test
  void oneLocationAsOneLocator() {
    var m = new ExternalModuleLocation("m", "u");
    assertSame(ExternalModuleLocator.Stability.STABLE, m.stability());
    var externals = new Externals(Set.of(), List.of(m));
    var external = externals.findExternal("m").orElseThrow();
    assertSame(m, external.by());
    assertEquals("m", external.location().module());
    assertEquals("u", external.location().uri());
  }

  @Test
  void twoLocationsAsTwoLocators() {
    var m1 = new ExternalModuleLocation("m1", "u1");
    var m2 = new ExternalModuleLocation("m2", "u2");
    var externals = new Externals(Set.of(), List.of(m1, m2));
    var external1 = externals.findExternal("m1").orElseThrow();
    assertEquals("m1", external1.location().module());
    assertEquals("u1", external1.location().uri());
    var external2 = externals.findExternal("m2").orElseThrow();
    assertEquals("m2", external2.location().module());
    assertEquals("u2", external2.location().uri());
  }

  @Test
  void twoLocationsInOneLocator() {
    var m1 = new ExternalModuleLocation("m1", "u1");
    var m2 = new ExternalModuleLocation("m2", "u2");
    var locations = new ExternalModuleLocations(Map.of(m1.module(), m1, m2.module(), m2));
    assertSame(ExternalModuleLocator.Stability.STABLE, locations.stability());
    var externals = new Externals(Set.of(), List.of(locations));
    var external1 = externals.findExternal("m1").orElseThrow();
    assertEquals("m1", external1.location().module());
    assertEquals("u1", external1.location().uri());
    var external2 = externals.findExternal("m2").orElseThrow();
    assertEquals("m2", external2.location().module());
    assertEquals("u2", external2.location().uri());
  }
}
