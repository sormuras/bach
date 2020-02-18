package org.astro;

public class WorldTests {
  static {
    if (!World.PACKAGE_PRIVATE_FIELD.equals("World")) throw new AssertionError(")-:");
    new test.base.Tester();
  }
}
