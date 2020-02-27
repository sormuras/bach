package org.astro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import test.base.PrependModuleName;

@DisplayNameGeneration(PrependModuleName.class)
class WorldTests {
  @Test
  void test() {
    assertEquals("World", World.PACKAGE_PRIVATE_FIELD);
    assertNotNull(new test.base.Tester().toString());
  }
}
