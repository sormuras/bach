package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Durations;
import com.github.sormuras.bach.workflow.Browser;
import com.github.sormuras.bach.workflow.BuildWorkflow;
import com.github.sormuras.bach.workflow.CompileWorkflow;
import com.github.sormuras.bach.workflow.ExecuteTestsWorkflow;
import com.github.sormuras.bach.workflow.Folders;
import com.github.sormuras.bach.workflow.Logbook;
import com.github.sormuras.bach.workflow.ManageExternalModulesWorkflow;
import com.github.sormuras.bach.workflow.Printer;
import com.github.sormuras.bach.workflow.Resolver;
import com.github.sormuras.bach.workflow.Runner;
import com.github.sormuras.bach.workflow.WriteLogbookWorkflow;
import java.lang.System.Logger.Level;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;

public class Bach {

  public static void build(UnaryOperator<Project> composer, String... args) {
    var options = Options.of(args);
    var project = Project.of("unnamed", "0").with(options);
    Bach.build(Bach.of(composer.apply(project)));
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
    return Bach.of(project, Settings.of());
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
  protected final Resolver resolver;

  public Bach(Project project, Settings settings) {
    this.browser = new AtomicReference<>();
    this.project = project;
    this.settings = settings;
    this.logbook = Logbook.of(settings.logbookSettings());
    this.folders = Folders.of(settings.folderSettings());
    this.printer = new Printer(this);
    this.runner = new Runner(this);
    this.resolver = new Resolver(this);
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

  public final Resolver resolver() {
    return resolver;
  }

  public final void log(String format, Object... args) {
    log(Level.DEBUG, format, args);
  }

  public final void log(Level level, String format, Object... args) {
    logbook.log(level, args.length == 0 ? format : String.format(format, args));
  }

  public void execute(Call call) {
    var handler = settings.workflowSettings().tweakHandler();
    var tweak = new Tweak(this, call);
    executeTweaked(handler.handle(tweak));
  }

  private void executeTweaked(Call call) {
    log(Level.INFO, "  %-9s %s", call.name(), call.toDescription(117));
    var tool = provider(call);
    var arguments = call.arguments();
    runner.run(tool, arguments).requireSuccessful();
  }

  private ToolProvider provider(Call call) {
    if (call instanceof ToolProvider provider) return provider;
    return runner.findToolProvider(call.name()).orElseThrow();
  }

  public void execute(Call.Tree tree) {
    if (tree.isEmpty()) return;
    log(Level.INFO, tree.caption());
    var calls = tree.parallel() ? tree.calls().parallelStream() : tree.calls().stream();
    calls.forEach(this::execute);
    tree.trees().forEach(this::execute);
  }

  public void execute(Workflow workflow) {
    workflow.execute();
  }

  public void build() {
    execute(new BuildWorkflow(this));
  }

  public void manageExternalModules() {
    execute(new ManageExternalModulesWorkflow(this));
  }

  public void compileMainSpace() {
    execute(new CompileWorkflow(this, project.spaces().main()));
  }

  public void compileTestSpace() {
    execute(new CompileWorkflow(this, project.spaces().test()));
  }

  public void executeTests() {
    execute(new ExecuteTestsWorkflow(this));
  }

  public void writeLogbook() {
    execute(new WriteLogbookWorkflow(this));
  }
}
