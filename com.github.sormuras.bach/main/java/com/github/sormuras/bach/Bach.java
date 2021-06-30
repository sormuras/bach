package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Durations;
import com.github.sormuras.bach.workflow.Browser;
import com.github.sormuras.bach.workflow.CompileMainModulesWorkflow;
import com.github.sormuras.bach.workflow.CompileTestModulesWorkflow;
import com.github.sormuras.bach.workflow.Printer;
import com.github.sormuras.bach.workflow.WriteLogbookWorkflow;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class Bach {

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
  protected final Printer printer;

  public Bach(Project project, Settings settings) {
    this.browser = new AtomicReference<>();
    this.project = project;
    this.settings = settings;
    this.printer = new Printer(this);
  }

  public final Project project() {
    return project;
  }

  public final Settings settings() {
    return settings;
  }

  public final Printer printer() {
    return printer;
  }

  public final void log(String format, Object... args) {
    log(Level.DEBUG, format, args);
  }

  public final void log(Level level, String format, Object... args) {
    settings.logbook().log(level, args.length == 0 ? format : String.format(format, args));
  }

  public final Browser browser() {
    var current = browser.get();
    if (current != null) return current;
    var client = settings.browserSettings().newHttpClient();
    log(
        "New HttpClient created with %s connect timeout and redirect policy of: %s",
        client.connectTimeout().map(Durations::beautify).orElse("no"), client.followRedirects());
    current = new Browser(this, client);
    return browser.compareAndSet(null, current) ? current : browser.get();
  }

  public void build() {
    var logbook = settings.logbook();
    log(Level.INFO, "Project %s", project.toNameAndVersion());
    var start = Instant.now();
    try {
      compileMainModules();
      compileTestModules();
    } catch (Exception exception) {
      logbook.log(exception);
    } finally {
      log(Level.INFO, "Build took %s", Durations.beautifyBetweenNow(start));
      writeLogbook();
    }
  }

  public void compileMainModules() {
    new CompileMainModulesWorkflow(this).execute();
  }

  public void compileTestModules() {
    new CompileTestModulesWorkflow(this).execute();
  }

  public void writeLogbook() {
    new WriteLogbookWorkflow(this).execute();
  }
}
