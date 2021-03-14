package com.github.sormuras.bach;

import com.github.sormuras.bach.api.BachAPI;
import com.github.sormuras.bach.api.JavaFormatterAPI;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.project.Flag;
import com.github.sormuras.bach.project.Folders;
import com.github.sormuras.bach.project.Property;
import com.github.sormuras.bach.util.Records;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ServiceLoader;

/** Java Shell Builder. */
public class Bach implements AutoCloseable, BachAPI {

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
    var root = Path.of("");
    var bach = root.resolve(".bach");
    var name = options.get(Property.BACH_INFO, ProjectInfo.MODULE);
    var layer = ModuleLayerBuilder.build(bach, name, Bach.bin(), bach.resolve("workspace"));
    var module = layer.findModule(name);
    if (module.isEmpty()) return new Bach(options);
    var info = module.get().getAnnotation(ProjectInfo.class);
    return ServiceLoader.load(layer, Provider.class)
        .findFirst()
        .map(factory -> factory.newBach(options.with(info)))
        .orElse(new Bach(options.with(info)));
  }

  public static Path bin() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    var resolved = module.getLayer().configuration().findModule(module.getName()).orElseThrow();
    var uri = resolved.reference().location().orElseThrow();
    return Path.of(uri).getParent();
  }

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
  }

  private final Options options;
  private final Logbook logbook;
  private /*-*/ Browser browser;
  private final Project project;

  public Bach(Options options) {
    this.options = options;
    this.logbook = new Logbook(options);
    this.browser = null; // defered creation in its accessor
    this.project = newProject();
  }

  /** {@return an instance of {@code HttpClient} that is memoized by this Bach object} */
  protected HttpClient newHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  /** {@return an instance of {@code Project} that is used as a component of this Bach object} */
  protected Project newProject() {
    return computeProject();
  }

  /** {@return this} */
  @Override
  public final Bach bach() {
    return this;
  }

  /** {@return the options instance passed to the constructor of this Bach object} */
  public final Options options() {
    return options;
  }

  public final Folders folders() {
    return project.settings().folders();
  }

  public final synchronized Browser browser() {
    if (browser == null) browser = new Browser(this);
    return browser;
  }

  public final Project project() {
    return project;
  }

  public final Logbook logbook() {
    return logbook;
  }

  public final boolean is(Flag flag) {
    return options.is(flag);
  }

  @Override
  public final void build() throws Exception {
    say("Build %s %s", project().name(), project().version());
    if (is(Flag.VERBOSE)) info();
    var start = Instant.now();
    if (is(Flag.STRICT)) formatJavaSourceFiles(JavaFormatterAPI.Mode.VERIFY);
    loadMissingExternalModules();
    verifyExternalModules();
    buildProjectMainSpace();
    buildProjectTestSpace();
    say("Build took %s", Strings.toString(Duration.between(start, Instant.now())));
  }

  public final void clean() throws Exception {
    log("Clean...");

    var workspace = folders().workspace();
    if (Files.notExists(workspace)) return;
    try (var walk = Files.walk(workspace)) {
      var paths = walk.sorted((p, q) -> -p.compareTo(q)).toArray(Path[]::new);
      log("Delete %s paths", paths.length);
      for (var path : paths) Files.deleteIfExists(path);
    }
  }

  @Override
  public void close() {
    if (logbook.runs().isEmpty()) return;
    try {
      var logbook = writeLogbook();
      say("Logbook written to %s", logbook.toUri());
    } catch (Exception exception) {
      say("Write logbook failed: %s", exception.getMessage());
    }
  }

  public final void info() {
    say("bin: %s", bin());
    say("bach: %s/%s", getClass().getModule().getName(), getClass().getName());
    say("project.name: %s", project.name());
    say("project.version: %s", project.version());
    say("%s", Records.toLines(project));
  }

  @Override
  public final void format() {
    log("Format...");
    formatJavaSourceFiles();
  }
}
