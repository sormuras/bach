package de.sormuras.bach.project;

import de.sormuras.bach.internal.Factory;

/** A code space for {@code test-preview} modules. */
public final class TestSpacePreview implements Space<TestSpacePreview> {

  private final Units units;

  public TestSpacePreview(Units units) {
    this.units = units;
  }

  public Units units() {
    return units;
  }

  @Override
  public String name() {
    return "test-preview";
  }

  @Factory
  public static TestSpacePreview of() {
    return new TestSpacePreview(Units.of());
  }

  @Factory(Factory.Kind.SETTER)
  public TestSpacePreview units(Units units) {
    return new TestSpacePreview(units);
  }
}
