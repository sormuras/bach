package run.bach.project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import run.bach.Paths;

public record ProjectSpace(
    String name,
    DeclaredModules modules,
    int release,
    Optional<String> launcher,
    List<String> requires // used to compute "--[processor-]module-path"
    ) {

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
    this(name, DeclaredModules.of(), 0, Optional.empty(), List.of(requires));
  }

  public ProjectSpace withModules(DeclaredModules modules) {
    return new ProjectSpace(name, modules, release, launcher, requires);
  }

  public ProjectSpace withTargetsJava(int release) {
    return new ProjectSpace(name, modules, release, launcher, requires);
  }

  public ProjectSpace withLauncher(String launcher) {
    return new ProjectSpace(name, modules, release, Optional.of(launcher), requires);
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
}
