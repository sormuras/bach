package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.trait.PrintTrait;
import com.github.sormuras.bach.trait.ResolveTrait;
import com.github.sormuras.bach.trait.ToolTrait;
import com.github.sormuras.bach.trait.WorkflowTrait;
import java.time.Duration;
import java.time.Instant;

public record Bach(Core core, Project project)
    implements WorkflowTrait, PrintTrait, ResolveTrait, ToolTrait {

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
  }

  public Bach bach() {
    return this;
  }

  public Logbook logbook() {
    return core.logbook();
  }

  public Options options() {
    return core.options();
  }

  public void say(String message) {
    logbook().info(message);
  }

  public void log(String message) {
    logbook().debug(message);
  }

  public int run() {
    var options = options();
    if (options.command().isPresent()) {
      var command = options.command().get();
      var name = command.name();
      var arguments = command.arguments();
      switch (name) {
        case PRINT_MODULES -> printModules();
        case PRINT_DECLARED_MODULES -> printDeclaredModules();
        case PRINT_EXTERNAL_MODULES -> printExternalModules();
        case PRINT_SYSTEM_MODULES -> printSystemModules();
        case PRINT_TOOLS -> printTools();
        case LOAD_EXTERNAL_MODULE -> loadExternalModules(arguments.toArray(String[]::new));
        case LOAD_MISSING_EXTERNAL_MODULES -> loadMissingExternalModules();
      }
      return 0;
    }

    say("Bach " + version());
    if (options.verbose()) {
      say("Configuration");
      say(options.toString());
    }
    say("Work on project %s %s".formatted(project.name(), project.version()));

    var workflows = options.workflow();
    var start = Instant.now();
    try {
      workflows.forEach(this::run);
    } catch (Exception exception) {
      logbook().log(exception);
      return 1;
    } finally {
      say("Bach run took " + Strings.toString(Duration.between(start, Instant.now())));
      writeLogbook();
    }
    return 0;
  }
}
