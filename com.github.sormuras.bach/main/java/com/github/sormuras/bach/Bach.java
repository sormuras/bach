package com.github.sormuras.bach;

import com.github.sormuras.bach.Options.Flag;
import com.github.sormuras.bach.Options.Property;
import com.github.sormuras.bach.api.BachAPI;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.spi.ToolProvider;

/** Java Shell Builder. */
public class Bach implements BachAPI {

  public static final Path BIN = Path.of(".bach/bin");

  public static final Path EXTERNALS = Path.of(".bach/external-modules");

  public static final Path WORKSPACE = Path.of(".bach/workspace");

  public static final String INFO_MODULE = "bach.info";

  /** A {@code Bach}-creating service. */
  public interface Provider<B extends Bach> {
    /**
     * {@return a new instance of service-provider that extends {@code Bach}}
     *
     * @param options the options object to be passed on
     */
    B newBach(Options options);
  }

  public static void main(String... args) {
    System.exit(new Main().run(args));
  }

  public static Bach of(Options options) {
    var layer = ModuleLayerBuilder.build(options.get(Property.BACH_INFO, INFO_MODULE));
    return ServiceLoader.load(layer, Provider.class)
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
  private final Queue<Recording> recordings;
  private /*-*/ Browser browser;
  private final Project project;

  public Bach(Options options) {
    this.options = options;
    this.base = newBase();
    this.recordings = new ConcurrentLinkedQueue<>();
    this.browser = null; // defered creation in its accessor
    this.project = newProject();
  }

  @Override
  public final Bach bach() {
    return this;
  }

  public Options options() {
    return options;
  }

  public boolean is(Flag flag) {
    return options.is(flag);
  }

  public String get(Property property, String defaultValue) {
    return options.get(property, defaultValue);
  }

  public Optional<String> get(Property property) {
    return Optional.ofNullable(get(property, null));
  }

  protected Base newBase() {
    return Base.of(Path.of(options.get(Property.BASE_DIRECTORY, "")));
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

  protected Project newProject() {
    return computeProject();
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
    buildProject();
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

  public void debug(String format, Object... args) {
    if (is(Flag.VERBOSE)) print("// " + format, args);
  }

  public void print(String format, Object... args) {
    var message = args == null || args.length == 0 ? format : String.format(format, args);
    options.out().println(message);
  }

  public void info() {
    print("module: %s", getClass().getModule().getName());
    print("class: %s", getClass().getName());
    print("base: %s", base);
    print("flags: %s", options.flags());
    print("project: %s", project);
  }

  @Override
  public Recording run(ToolProvider provider, List<String> arguments) {
    var name = provider.name();
    var currentThread = Thread.currentThread();
    var currentLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(provider.getClass().getClassLoader());

    var out = new StringWriter();
    var err = new StringWriter();
    var args = arguments.toArray(String[]::new);
    var start = Instant.now();
    int code;
    try {
      var skips = options.values(Property.SKIP_TOOL);
      var skip = skips.contains(name);
      if (skip) debug("Skip run of '%s' due to --skip-tool=%s", name, skips);
      code = skip ? 0 : provider.run(new PrintWriter(out), new PrintWriter(err), args);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var duration = Duration.between(start, Instant.now());
    var tid = currentThread.getId();
    var output = out.toString().trim();
    var errors = err.toString().trim();
    var recording = new Recording(name, arguments, tid, duration, code, output, errors);

    recordings.add(recording);
    return recording;
  }

  public Path writeLogbook() throws Exception {
    var logbook = newLogbook();
    var lines = logbook.build();

    Files.createDirectories(base.workspace());
    var file = Files.write(base.workspace("logbook.md"), lines);
    var logbooks = Files.createDirectories(base.workspace("logbooks"));
    Files.copy(file, logbooks.resolve("logbook-" + logbook.timestamp() + ".md"));
    return file;
  }
}
