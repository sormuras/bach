package com.github.sormuras.bach.project;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Space(
    String name,
    List<DeclaredModule> modules,
    int release,
    Optional<String> launcher,
    List<String> requires, // used to compute "--[processor-]module-path"
    List<String> additionalCompileJavacArguments) {

  public Space {
    Objects.requireNonNull(name);
    Objects.requireNonNull(modules);
    var feature = Runtime.version().feature();
    if (release != 0 && (release < 9 || release > feature))
      throw new IndexOutOfBoundsException(
          "Release %d not in range of %d..%d".formatted(release, 9, feature));
    Objects.requireNonNull(launcher);
    Objects.requireNonNull(requires);
  }

  public Space(String name, String... requires) {
    this(name, List.of(), 0, Optional.empty(), List.of(requires), List.of());
  }

  public Space withModules(List<DeclaredModule> modules) {
    return new Space(name, modules, release, launcher, requires, additionalCompileJavacArguments);
  }

  public Space withTargetsJava(int release) {
    return new Space(name, modules, release, launcher, requires, additionalCompileJavacArguments);
  }

  public Space withLauncher(String launcher) {
    return new Space(
        name, modules, release, Optional.of(launcher), requires, additionalCompileJavacArguments);
  }

  public Optional<DeclaredModule> findDeclaredModule(String name) {
    return modules.stream().filter(module -> module.name().equals(name)).findFirst();
  }

  public Optional<Integer> targets() {
    return release == 0 ? Optional.empty() : Optional.of(release);
  }
}
