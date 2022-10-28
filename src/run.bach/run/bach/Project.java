package run.bach;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Modular project model. */
public record Project(Name name, Version version, Spaces spaces, Externals externals) {

  @Target(ElementType.MODULE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Info {
    String name() default "unnamed";

    String version() default "0-ea";

    String findModuleInfoPattern() default "**module-info.java";

    String findModuleInfoSyntax() default "glob";

    int multiReleaseBaseJava() default 0; // with 0 meaning: no `--release=N` argument is generated
  }

  /** {@return an {@code "unnamed 0-ea"} project with empty init, main, and test module spaces} */
  public static Project ofDefaults() {
    var info = Project.class.getModule().getAnnotation(Info.class);
    var name = new Name(info.name());
    var version = new Version(info.version(), ZonedDateTime.now());
    var init = new Space("init");
    var main = new Space("main", "init");
    var test = new Space("test", "main");
    var spaces = new Spaces(init, main, test);
    var externals = new Externals();
    return new Project(name, version, spaces, externals);
  }

  @SuppressWarnings("PatternVariableHidesField")
  private Project with(Component component) {
    return new Project(
        component instanceof Name name ? name : name,
        component instanceof Version version ? version : version,
        component instanceof Spaces spaces ? spaces : spaces,
        component instanceof Externals externals ? externals : externals);
  }

  public Project with(Space space) {
    return with(spaces.with(space));
  }

  /** {@return new project instance using the given string as the project name} */
  public Project withName(String string) {
    return with(new Name(string));
  }

  /** {@return new project instance using the given string as the project version} */
  public Project withVersion(String string) {
    return with(version.with(string));
  }

  /** {@return new project instance using the given string as the project version date} */
  public Project withVersionDate(String string) {
    return with(version.withDate(string));
  }

  /**
   * {@return new project instance setting main space's Java release feature number to the integer
   * value of the given string}
   */
  public Project withTargetsJava(String string) {
    return withTargetsJava(Integer.parseInt(string));
  }

  /** {@return new project instance setting main space's Java release feature number} */
  public Project withTargetsJava(int release) {
    return withTargetsJava("main", release);
  }

  /** {@return new project instance setting specified space's Java release feature number} */
  public Project withTargetsJava(String space, int release) {
    return withTargetsJava(spaces.space(space), release);
  }

  /** {@return new project instance setting specified space's Java release feature number} */
  public Project withTargetsJava(Space space, int release) {
    return with(space.withTargetsJava(release));
  }

  /** {@return new project instance setting main space's launcher} */
  public Project withLauncher(String launcher) {
    return withLauncher("main", launcher);
  }

  /** {@return new project instance setting specified space's launcher} */
  public Project withLauncher(String space, String launcher) {
    return withLauncher(spaces.space(space), launcher);
  }

  /** {@return new project instance setting specified space's launcher} */
  public Project withLauncher(Space space, String launcher) {
    return with(space.withLauncher(launcher));
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(String space, String root, String info) {
    return withModule(spaces.space(space), DeclaredModule.of(Path.of(root), Path.of(info)));
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(Space space, DeclaredModule module) {
    return with(spaces.with(space.withModules(space.modules().with(module))));
  }

  /** {@return new project instance with one or more additional modular dependences} */
  public Project withRequiresModule(String name, String... more) {
    return with(externals.withRequires(name).withRequires(more));
  }

  public Project withWalkingAnnotation(Info info) {
    var project = this;
    project = project.withName(info.name());
    project = project.with(project.spaces.main().withTargetsJava(info.multiReleaseBaseJava()));
    return project;
  }

  /**
   * {@return new project instance configured by finding {@code module-info.java} files below the
   * specified root directory matching the given {@link
   * java.nio.file.FileSystem#getPathMatcher(String) syntaxAndPattern}}
   */
  public Project withWalkingDirectory(Path directory, String syntaxAndPattern) {
    var project = this;
    var directoryName = directory.normalize().toAbsolutePath().getFileName();
    if (directoryName != null) project = project.withName(directoryName.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        if (uri.contains("/.bach/")) continue; // exclude project-local modules
        if (uri.matches(".*?/java-\\d+.*")) continue; // exclude non-base modules
        var module = DeclaredModule.of(directory, path);
        if (uri.contains("/init/")) {
          project = project.withModule(project.spaces().init(), module);
          continue;
        }
        if (uri.contains("/test/")) {
          project = project.withModule(project.spaces().test(), module);
          continue;
        }
        project = project.withModule(project.spaces().main(), module);
        var name = module.name();
        var launcher =
            module.base().sources().stream()
                .map(dir -> dir.resolve(name.replace('.', '/') + "/Main.java"))
                .filter(Files::isRegularFile)
                .findFirst();
        if (launcher.isPresent())
          project = project.withLauncher(project.spaces().main(), name + '/' + name + ".Main");
      }
    } catch (Exception exception) {
      throw new RuntimeException(
          "Find with %s failed: %s".formatted(syntaxAndPattern, exception), exception);
    }
    return project;
  }

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  @FunctionalInterface
  public interface Composer {
    Project composeProject(Project project);
  }

  public sealed interface Component {}

  public record Name(String value) implements Component {
    public Name {
      Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public record Version(String value, ZonedDateTime date) implements Component {
    public Version {
      ModuleDescriptor.Version.parse(value);
    }

    public Version with(String value) {
      return new Version(value, date);
    }

    public Version with(ZonedDateTime date) {
      return new Version(value, date);
    }

    public Version withDate(String text) {
      return with(ZonedDateTime.parse(text));
    }
  }

  public record Spaces(Space init, Space main, Space test) implements Component {
    public List<Space> list() {
      return List.of(init, main, test);
    }

    public Spaces with(Space space) {
      return new Spaces(
          space.name().equals(init.name()) ? space : init,
          space.name().equals(main.name()) ? space : main,
          space.name().equals(test.name()) ? space : test);
    }

    public Space space(String name) {
      return switch (name) {
        case "init" -> init;
        case "main" -> main;
        case "test" -> test;
        default -> throw new IllegalArgumentException("No such space: " + name);
      };
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
      this(name, DeclaredModules.of(), 0, Optional.empty(), List.of(requires));
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
