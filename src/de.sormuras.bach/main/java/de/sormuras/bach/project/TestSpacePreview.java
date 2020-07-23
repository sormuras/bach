package de.sormuras.bach.project;

import de.sormuras.bach.internal.Factory;

/** A code space for {@code test-preview} modules. */
public final class TestSpacePreview implements CodeSpace<TestSpacePreview> {

  private final CodeUnits units;

  public TestSpacePreview(CodeUnits units) {
    this.units = units;
  }

  public CodeUnits units() {
    return units;
  }

  @Override
  public String name() {
    return "test-preview";
  }

  @Factory
  public static TestSpacePreview of() {
    return new TestSpacePreview(CodeUnits.of());
  }

  @Factory(Factory.Kind.SETTER)
  public TestSpacePreview units(CodeUnits units) {
    return new TestSpacePreview(units);
  }
}
