package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Paths;
import com.github.sormuras.bach.ToolCallTweak;
import com.github.sormuras.bach.internal.ModulesSupport;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

public record ProjectSpace(
    String name,
    DeclaredModules modules,
    int release,
    Optional<String> launcher,
    List<String> requires, // used to compute "--[processor-]module-path"
    Map<String, ToolCallTweak> tweaks)
    implements Formattable {

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

  public Optional<String> toModulePath(Paths paths) {
    var externalModules = Stream.of(paths.externalModules());
    var requiredModules = requires.stream().map(required -> paths.out(required, "modules"));
    var elements =
        Stream.concat(requiredModules, externalModules)
            .filter(Files::isDirectory)
            .map(Path::toString)
            .toList();
    if (elements.isEmpty()) return Optional.empty();
    return Optional.of(String.join(File.pathSeparator, elements));
  }

  public ToolCallTweak tweak(String id) {
    return tweaks.getOrDefault(id, ToolCallTweak.identity());
  }

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    formatter.format(
        """
        Space
        %20s = %s
        %20s = %d
        %20s = %s
        %20s = %s
        """
            .formatted(
                "name",
                name,
                "release",
                release,
                "requires spaces",
                requires,
                "required modules",
                ModulesSupport.required(modules.toModuleFinder())));
  }
}
