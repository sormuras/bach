package org.astro;

public class WorldTests {
  static {
    assert World.PACKAGE_PRIVATE_FIELD.equals("World");
    new test.base.Tester();
  }
}
