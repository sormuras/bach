package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.trait.PrintTrait;
import com.github.sormuras.bach.trait.ResolveTrait;
import com.github.sormuras.bach.trait.ToolTrait;
import java.time.Duration;
import java.time.Instant;

public record Bach(Settings settings, Project project) implements PrintTrait, ResolveTrait, ToolTrait {

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) return "(unnamed)";
    return module.getDescriptor().version().map(Object::toString).orElse("(exploded)");
  }

  public Bach bach() {
    return this;
  }

  public Options options() {
    return settings.options();
  }

  public Logbook logbook() {
    return settings.logbook();
  }

  public void say(String message) {
    logbook().info(message);
  }

  public void log(String message) {
    logbook().debug(message);
  }

  public int buildAndWriteLogbook() {
    var options = options();

    say("Bach " + version());
    if (options.verbose()) {
      say("Configuration");
      say(options.toString());
    }
    say("Work on project %s %s".formatted(project.name(), project.version()));

    var start = Instant.now();
    try {
      settings.workflows().newBuildWorkflow().with(this).build();
    } catch (Exception exception) {
      logbook().log(exception);
      return 1;
    } finally {
      say("Bach run took " + Strings.toString(Duration.between(start, Instant.now())));
      settings.workflows().newWriteLogbookWorkflow().with(this).write();
    }
    return 0;
  }
}
