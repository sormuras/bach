package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Durations;
import com.github.sormuras.bach.workflow.Browser;
import com.github.sormuras.bach.workflow.CompileWorkflow;
import com.github.sormuras.bach.workflow.Folders;
import com.github.sormuras.bach.workflow.Logbook;
import com.github.sormuras.bach.workflow.Printer;
import com.github.sormuras.bach.workflow.Runner;
import com.github.sormuras.bach.workflow.WriteLogbookWorkflow;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;

public class Bach {

  public static void build(UnaryOperator<Project> projector) {
    var project = projector.apply(Project.of("project", "0"));
    var settings = Settings.newSettings();
    Bach.build(project, settings);
  }

  public static void build(Project project, Settings settings) {
    build(new Bach(project, settings));
  }

  public static void build(Bach bach) {
    try {
      bach.build();
    } catch (Throwable cause) {
      if (cause instanceof Error) throw cause;
      throw new Error("Caught unhandled throwable", cause);
    }
  }

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) return "(unnamed)";
    return module.getDescriptor().version().map(Object::toString).orElse("(exploded)");
  }

  public static Bach of(Project project) {
    return Bach.of(project, Settings.newSettings());
  }

  public static Bach of(Project project, Settings settings) {
    return new Bach(project, settings);
  }

  protected final AtomicReference<Browser> browser;
  protected final Project project;
  protected final Settings settings;
  protected final Logbook logbook;
  protected final Folders folders;
  protected final Printer printer;
  protected final Runner runner;

  public Bach(Project project, Settings settings) {
    this.browser = new AtomicReference<>();
    this.project = project;
    this.settings = settings;
    this.logbook = Logbook.of(settings.logbookSettings());
    this.folders = Folders.of(settings.folderSettings());
    this.printer = new Printer(this);
    this.runner = new Runner(this);
  }

  public final Browser browser() {
    var current = browser.get();
    if (current != null) return current;
    var client = settings.browserSettings().httpClientBuilder().build();
    log(
        "New HttpClient created with %s connect timeout and redirect policy of: %s",
        client.connectTimeout().map(Durations::beautify).orElse("no"), client.followRedirects());
    current = new Browser(this, client);
    return browser.compareAndSet(null, current) ? current : browser.get();
  }

  public final Project project() {
    return project;
  }

  public final Settings settings() {
    return settings;
  }

  public final Logbook logbook() {
    return logbook;
  }

  public final Folders folders() {
    return folders;
  }

  public final Printer printer() {
    return printer;
  }

  public final Runner runner() {
    return runner;
  }

  public final void log(String format, Object... args) {
    log(Level.DEBUG, format, args);
  }

  public final void log(Level level, String format, Object... args) {
    logbook.log(level, args.length == 0 ? format : String.format(format, args));
  }

  public void execute(Call call) {
    log(Level.INFO, "  %-9s %s".formatted(call.name(), call.toDescription(117)));
    var tool =
        call instanceof ToolProvider provider
            ? provider
            : runner.findToolProvider(call.name()).orElseThrow();
    var arguments = call.arguments();
    var run = runner.run(tool, arguments);
    logbook.log(run);
    run.requireSuccessful();
  }

  public void execute(Call.Tree tree) {
    log(Level.INFO, tree.caption());
    tree.calls().forEach(this::execute);
    tree.trees().forEach(this::execute);
  }

  public void execute(Workflow workflow) {
    workflow.execute();
  }

  public void build() {
    log(Level.INFO, "Project %s", project.toNameAndVersion());
    var start = Instant.now();
    try {
      compileMainSpace();
      compileTestSpace();
    } catch (Exception exception) {
      logbook.log(exception);
      throw new RuntimeException("Build failed!", exception);
    } finally {
      log(Level.INFO, "Build took %s", Durations.beautifyBetweenNow(start));
      writeLogbook();
    }
  }

  public void compileMainSpace() {
    execute(new CompileWorkflow(this, project.spaces().main()));
  }

  public void compileTestSpace() {
    execute(new CompileWorkflow(this, project.spaces().test()));
  }

  public void writeLogbook() {
    execute(new WriteLogbookWorkflow(this));
  }
}
