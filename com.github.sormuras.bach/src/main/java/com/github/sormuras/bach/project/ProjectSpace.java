package com.github.sormuras.bach.project;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record ProjectSpace(
    String name,
    DeclaredModules modules,
    int release,
    Optional<String> launcher,
    List<String> requires, // used to compute "--[processor-]module-path"
    List<String> additionalCompileJavacArguments) {

  public ProjectSpace {
    Objects.requireNonNull(name);
    Objects.requireNonNull(modules);
    var feature = Runtime.version().feature();
    if (release != 0 && (release < 9 || release > feature))
      throw new IndexOutOfBoundsException(
          "Release %d not in range of %d..%d".formatted(release, 9, feature));
    Objects.requireNonNull(launcher);
    Objects.requireNonNull(requires);
  }

  public ProjectSpace(String name, String... requires) {
    this(name, DeclaredModules.of(), 0, Optional.empty(), List.of(requires), List.of());
  }

  public ProjectSpace withModules(DeclaredModules modules) {
    return new ProjectSpace(
        name, modules, release, launcher, requires, additionalCompileJavacArguments);
  }

  public ProjectSpace withTargetsJava(int release) {
    return new ProjectSpace(
        name, modules, release, launcher, requires, additionalCompileJavacArguments);
  }

  public ProjectSpace withLauncher(String launcher) {
    return new ProjectSpace(
        name, modules, release, Optional.of(launcher), requires, additionalCompileJavacArguments);
  }

  public ProjectSpace withAdditionalCompileJavacArguments(String... args) {
    var arguments = Stream.concat(additionalCompileJavacArguments.stream(), Stream.of(args));
    return new ProjectSpace(name, modules, release, launcher, requires, arguments.toList());
  }

  public Optional<Integer> targets() {
    return release == 0 ? Optional.empty() : Optional.of(release);
  }
}
