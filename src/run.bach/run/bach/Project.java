package run.bach;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Modular project model. */
public record Project(Name name, Version version, Spaces spaces, Externals externals) {

  /** {@return an {@code "unnamed 0-ea"} project with no module source code spaces} */
  public static Project ofDefaults(Bach bach) {
    var cli = bach.cli();
    var name = new Name(cli.__project_name().orElse("unnamed"));
    var version =
        new Version(
            cli.__project_version().orElse("0-ea"),
            cli.__project_version_timestamp()
                .map(ZonedDateTime::parse)
                .orElseGet(ZonedDateTime::now));
    var spaces = new Spaces();
    var externals = new Externals();
    return new Project(name, version, spaces, externals);
  }

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  public String toNameAndVersion() {
    return name.value + ' ' + version.value;
  }

  @FunctionalInterface
  public interface Factory {
    Project createProject(Bach bach);
  }

  public sealed interface Component {}

  public record Name(String value) implements Component {
    public Name {
      if (value.isBlank()) throw new IllegalArgumentException("Name must not be blank");
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public record Version(String value, ZonedDateTime timestamp) implements Component {
    public Version {
      ModuleDescriptor.Version.parse(value);
      timestamp.toInstant();
    }
  }

  public record Spaces(List<Space> list) implements Component {
    public Spaces(Space... spaces) {
      this(List.of(spaces));
    }

    public List<String> names() {
      return list.stream().map(Space::name).toList();
    }

    public Space space(String name) {
      var first = list.stream().filter(space -> space.name().equals(name)).findFirst();
      return first.orElseThrow(() -> new IllegalArgumentException("No such space: " + name));
    }
  }

  public record Space(
      String name,
      DeclaredModules modules,
      int release,
      Optional<String> launcher,
      List<String> requires // used to compute "--[processor-]module-path"
      ) {

    public Space {
      if (name.isBlank()) throw new IllegalArgumentException("Space name must not be blank");
      if (modules == null) throw new IllegalArgumentException("Space modules must not be null");
      //noinspection OptionalAssignedToNull
      if (launcher == null) throw new IllegalArgumentException("Space launcher must not be null");
      if (requires == null) throw new IllegalArgumentException("Space requires must not be null");
      var feature = Runtime.version().feature();
      if (release != 0 && (release < 9 || release > feature)) {
        var message = "Java release %d not in range of %d..%d".formatted(release, 9, feature);
        throw new IndexOutOfBoundsException(message);
      }
    }

    public Space(String name, String... requires) {
      this(name, DeclaredModules.of(), 0, Optional.empty(), List.of(requires));
    }

    public Space withModules(DeclaredModule... more) {
      return withModules(modules.with(more));
    }

    public Space withModules(DeclaredModules modules) {
      return new Space(name, modules, release, launcher, requires);
    }

    public Space withTargetsJava(int release) {
      return new Space(name, modules, release, launcher, requires);
    }

    public Space withLauncher(String launcher) {
      return new Space(name, modules, release, Optional.of(launcher), requires);
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

  public record Externals(Set<String> requires) implements Component {

    public Externals() {
      this(Set.of());
    }

    public Externals withRequires(String... modules) {
      var requires = Set.copyOf(Stream.concat(requires().stream(), Stream.of(modules)).toList());
      return new Externals(requires);
    }
  }
}
