package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** Java Shell Builder. */
public class Bach {

  public static final Path CACHE = Path.of(".bach/cache");

  public static final Path EXTERNALS = Path.of(".bach/external-modules");

  public static final Path WORKSPACE = Path.of(".bach/workspace");

  public static Bach of(String module) {
    var layer = new ModuleLayerBuilder(module).build();
    return ServiceLoader.load(layer, Bach.class).findFirst().orElseGet(Bach::new);
  }

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
  }

  private final Base base;
  private final Set<Flag> flags;
  private final Consumer<String> printer;
  private final Queue<Recording> recordings;
  private final Project project;

  public Bach() {
    this(Base.ofSystem(), System.out::println);
  }

  public Bach(Base base, Consumer<String> printer, Flag... flags) {
    this.base = base;
    this.flags = flags.length == 0 ? Set.of() : EnumSet.copyOf(Set.of(flags));
    this.printer = printer;
    this.recordings = new ConcurrentLinkedQueue<>();

    debug("module: %s", getClass().getModule().getName());
    debug("class: %s", getClass().getName());
    debug("base: %s", this.base);
    debug("flags: %s", this.flags);

    try {
      this.project = newProject();
    } catch (Exception exception) {
      throw new Error("Project creation failed", exception);
    }

    debug("project: %s", project);
  }

  public final Base base() {
    return base;
  }

  public final List<Recording> recordings() {
    return recordings.stream().toList();
  }

  public final Project project() {
    return project;
  }

  public Logbook newLogbook() {
    return new Logbook(this);
  }

  public Project newProject() throws Exception {
    return new Project();
  }

  @Main.Action
  public void build() throws Exception {
    debug("Build...");

    try {
      var root = base.directory();
      try (var walk = Files.walk(root, 9)) {
        var found =
            walk.filter(path -> String.valueOf(path.getFileName()).equals("module-info.java"))
                .toList();
        debug("%d module declaration(s) found", found.size());
        if (found.size() == 0) {
          var message = print("No modules found in file tree rooted at %s", root);
          throw new IllegalStateException(message);
        }

        buildMainSpace();
      }
    } finally {
      writeLogbook();
    }
  }

  public void buildMainSpace() throws Exception {}

  public String computeMainJarFileName(String module) {
    return module + '@' + project().versionNumberAndPreRelease() + ".jar";
  }

  @Main.Action
  public void clean() throws Exception {
    debug("Clean...");

    if (Files.notExists(base.workspace())) return;
    try (var walk = Files.walk(base.workspace())) {
      var paths = walk.sorted((p, q) -> -p.compareTo(q)).toArray(Path[]::new);
      debug("Delete %s paths", paths.length);
      for (var path : paths) Files.deleteIfExists(path);
    }
  }

  public boolean is(Flag flag) {
    return flags.contains(flag);
  }

  public void debug(String format, Object... args) {
    if (is(Flag.VERBOSE)) print("// " + format, args);
  }

  public String print(String format, Object... args) {
    var message = args == null || args.length == 0 ? format : String.format(format, args);
    printer.accept(message);
    return message;
  }

  @Main.Action({"help", "usage"})
  public void printHelp() {
    print("""
      Usage: bach ACTION [ARGS...]
      """);
  }

  @Main.Action("version")
  public void printVersion() {
    print(version());
  }

  public void run(Command<?> command) {
    var provider = ToolProvider.findFirst(command.name()).orElseThrow();
    var arguments = command.toStrings().toArray(String[]::new);
    debug("Run %s", command.toLine());
    var recording = run(provider, arguments);
    recording.requireSuccessful();
  }

  public Recording run(ToolProvider provider, String... arguments) {
    var output = new StringWriter();
    var outputPrintWriter = new PrintWriter(output);
    var errors = new StringWriter();
    var errorsPrintWriter = new PrintWriter(errors);

    var start = Instant.now();
    var code = provider.run(outputPrintWriter, errorsPrintWriter, arguments);
    var recording =
        new Recording(
            provider.name(),
            arguments,
            Thread.currentThread().getId(),
            Duration.between(start, Instant.now()),
            code,
            output.toString().trim(),
            errors.toString().trim());

    recordings.add(recording);
    return recording;
  }

  public void writeLogbook() throws Exception {
    var logbook = newLogbook();
    var lines = logbook.build();

    Files.createDirectories(base.workspace());
    var file = Files.write(base.workspace("logbook.md"), lines);
    var logbooks = Files.createDirectories(base.workspace("logbooks"));
    Files.copy(file, logbooks.resolve("logbook-" + logbook.timestamp() + ".md"));
  }
}
