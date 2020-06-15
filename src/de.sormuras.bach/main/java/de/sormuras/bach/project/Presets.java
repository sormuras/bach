package de.sormuras.bach.project;

import java.util.Optional;

public final class Presets {

  public static Presets of() {
    return new Presets(null);
  }

  private final Object[] mainModulesJavaCompilerArguments;

  public Presets(Object[] mainModulesJavaCompilerArguments) {
    this.mainModulesJavaCompilerArguments = mainModulesJavaCompilerArguments;
  }

  public Optional<Object[]> mainModulesJavaCompilerArguments() {
    return Optional.ofNullable(mainModulesJavaCompilerArguments);
  }

  public Presets withMainModulesJavaCompilerArguments(Object... arguments) {
    return new Presets(arguments);
  }
}
