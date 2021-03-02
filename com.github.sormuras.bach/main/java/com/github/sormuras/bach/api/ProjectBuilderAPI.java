package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.internal.Strings;
import java.time.Duration;
import java.time.Instant;

/** Methods related to building projects. */
public interface ProjectBuilderAPI {

  Bach bach();

  default void buildProject() throws Exception {
    var bach = bach();
    var project = bach.project();
    bach.print("Build %s %s", project.name(), project.version());
    if (bach.is(Options.Flag.VERBOSE)) bach.info();
    var start = Instant.now();
    if (bach.is(Options.Flag.STRICT)) bach.formatJavaSourceFiles(JavaFormatterAPI.Mode.VERIFY);
    bach.loadMissingExternalModules();
    bach.verifyExternalModules();
    buildProjectMainSpace();
    buildProjectTestSpace();
    bach.print("Build took %s", Strings.toString(Duration.between(start, Instant.now())));
  }

  default void buildProjectMainSpace() throws Exception {}

  default void buildProjectTestSpace() throws Exception {}
}
