package org.astro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import test.base.PrependModuleName;
import test.base.Tester;

@DisplayNameGeneration(PrependModuleName.class)
class WorldTests {

  @Test
  void createTesterFromModuleTestBase() {
    assertNotNull(new Tester().toString());
  }

  @Test
  void accessPublicMember() {
    assertEquals("public World", World.publicName());
  }

  @Test
  void accessPackagePrivateMember() {
    assertEquals("package-private World", World.packagePrivateName());
  }
}
