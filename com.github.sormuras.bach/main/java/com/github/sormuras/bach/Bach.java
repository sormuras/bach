package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.lookup.LookupException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Java Shell Builder. */
public class Bach {

  public static final Path BIN = Path.of(".bach/bin");

  public static final Path EXTERNALS = Path.of(".bach/external-modules");

  public static final Path WORKSPACE = Path.of(".bach/workspace");

  public interface Factory<B extends Bach> {
    B newBach(Options options);
  }

  public static Bach of(Options options) {
    var layer = ModuleLayerBuilder.build(options.configuration().orElse("configuration"));
    return ServiceLoader.load(layer, Factory.class)
        .findFirst()
        .map(factory -> factory.newBach(options))
        .orElse(new Bach(options));
  }

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
  }

  private final Options options;
  private final Base base;
  private final Set<Flag> flags;
  private final Queue<Recording> recordings;
  private /*-*/ Browser browser;
  private final Libraries libraries;
  private final Project project;

  public Bach(Options options) {
    this.options = options;
    this.base = newBase();
    this.flags = options.flags().isEmpty() ? Set.of() : EnumSet.copyOf(options.flags());
    this.recordings = new ConcurrentLinkedQueue<>();
    this.browser = null; // defered creation in its accessor
    this.libraries = newLibraries();
    this.project = newProject();
  }

  public Options options() {
    return options;
  }

  protected Base newBase() {
    return Base.ofSystem();
  }

  protected Browser newBrowser() {
    return new Browser(this);
  }

  protected HttpClient newHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  protected Logbook newLogbook() {
    return new Logbook(this);
  }

  protected Libraries newLibraries() {
    return new Libraries();
  }

  protected Project newProject() {
    return new Project();
  }

  public final Base base() {
    return base;
  }

  public final synchronized Browser browser() {
    if (browser == null) browser = newBrowser();
    return browser;
  }

  public final List<Recording> recordings() {
    return recordings.stream().toList();
  }

  public final Project project() {
    return project;
  }

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

  public String computeExternalModuleUri(String module) {
    var found = libraries.find(module).orElseThrow(() -> new LookupException(module));
    debug("%s <- %s", module, found);
    return found.uri();
  }

  public Path computeExternalModuleFile(String module) {
    return base.externals().resolve(module + ".jar");
  }

  public String computeMainJarFileName(String module) {
    return module + '@' + project().versionNumberAndPreRelease() + ".jar";
  }

  /** @return the names of all modules that are required but not locatable by this instance */
  public Set<String> computeMissingExternalModules() {
    var finder = ModuleFinder.of(EXTERNALS);
    var missing = Modules.required(finder);
    if (missing.isEmpty()) return Set.of();
    missing.removeAll(Modules.declared(finder));
    missing.removeAll(Modules.declared(ModuleFinder.of(BIN)));
    missing.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    if (missing.isEmpty()) return Set.of();
    return missing;
  }

  public Stream<ToolProvider> computeToolProviders() {
    var layer = new ModuleLayerBuilder().before(ModuleFinder.of(EXTERNALS)).build();
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }

  public ToolProvider computeToolProvider(String name) {
    var provider = computeToolProviders().filter(it -> it.name().equals(name)).findFirst();
    if (provider.isPresent()) return provider.get();
    throw new RuntimeException("No tool provider found for name: " + name);
  }

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

  public void loadExternalModules(String... modules) {
    debug("Load %d external module%s", modules.length, modules.length == 1 ? "" : "s");
    if (modules.length == 0) return;
    UnaryOperator<String> uri = this::computeExternalModuleUri;
    Function<String, Path> jar = this::computeExternalModuleFile;
    if (modules.length == 1) browser().load(uri.apply(modules[0]), jar.apply(modules[0]));
    else browser().load(Stream.of(modules).collect(Collectors.toMap(uri, jar)));
  }

  public void loadMissingExternalModules() {
    debug("Load missing external modules");
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var missing = computeMissingExternalModules();
      if (missing.isEmpty()) break;
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      loadExternalModules(missing.toArray(String[]::new));
      loaded.addAll(missing);
    }
    debug("Loaded %d module%s", loaded.size(), loaded.size() == 1 ? "" : "s");
  }

  public String print(String format, Object... args) {
    var message = args == null || args.length == 0 ? format : String.format(format, args);
    options.out().println(message);
    return message;
  }

  public void info() {
    print("module: %s", getClass().getModule().getName());
    print("class: %s", getClass().getName());
    print("base: %s", base);
    print("flags: %s", flags);
    print("project: %s", project);
  }

  public Recording run(Command<?> command) {
    debug("Run %s", command.toLine());
    var provider = computeToolProvider(command.name());
    var arguments = command.toStrings();
    return run(provider, arguments);
  }

  public List<Recording> run(Command<?> command, Command<?>... commands) {
    return run(Stream.concat(Stream.of(command), Stream.of(commands)).toList());
  }

  public List<Recording> run(List<Command<?>> commands) {
    var size = commands.size();
    debug("Run %d command%s", size, size == 1 ? "" : "s");
    if (size == 0) return List.of();
    if (size == 1) return List.of(run(commands.iterator().next()));
    var sequential = is(Flag.RUN_COMMANDS_SEQUENTIALLY);
    var stream = sequential ? commands.stream() : commands.stream().parallel();
    return stream.map(this::run).toList();
  }

  public Recording run(ToolProvider provider, List<String> arguments) {
    var currentThread = Thread.currentThread();
    var currentLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(provider.getClass().getClassLoader());

    var out = new StringWriter();
    var err = new StringWriter();
    var args = arguments.toArray(String[]::new);
    var start = Instant.now();
    int code;
    try {
      code = provider.run(new PrintWriter(out), new PrintWriter(err), args);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var duration = Duration.between(start, Instant.now());
    var tid = currentThread.getId();
    var output = out.toString().trim();
    var errors = err.toString().trim();
    var recording = new Recording(provider.name(), arguments, tid, duration, code, output, errors);

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
