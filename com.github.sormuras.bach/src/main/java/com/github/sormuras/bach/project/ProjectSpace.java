package com.github.sormuras.bach.project;

import com.github.sormuras.bach.ToolCallTweak;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record ProjectSpace(
    String name,
    DeclaredModules modules,
    int release,
    Optional<String> launcher,
    List<String> requires, // used to compute "--[processor-]module-path"
    Map<String, ToolCallTweak> tweaks) {

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
    this(name, DeclaredModules.of(), 0, Optional.empty(), List.of(requires), Map.of());
  }

  public ProjectSpace withModules(DeclaredModules modules) {
    return new ProjectSpace(name, modules, release, launcher, requires, tweaks);
  }

  public ProjectSpace withTargetsJava(int release) {
    return new ProjectSpace(name, modules, release, launcher, requires, tweaks);
  }

  public ProjectSpace withLauncher(String launcher) {
    return new ProjectSpace(name, modules, release, Optional.of(launcher), requires, tweaks);
  }

  public ProjectSpace withTweak(String id, ToolCallTweak tweak) {
    var tweaks = new TreeMap<>(tweaks());
    tweaks.merge(id, tweak, ToolCallTweak::merged);
    return new ProjectSpace(name, modules, release, launcher, requires, Map.copyOf(tweaks));
  }

  public Optional<Integer> targets() {
    return release == 0 ? Optional.empty() : Optional.of(release);
  }

  public ToolCallTweak tweak(String id) {
    return tweaks.getOrDefault(id, ToolCallTweak.identity());
  }
}
