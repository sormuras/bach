package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import java.nio.file.Files;
import java.nio.file.Path;
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

  default void buildProjectMainSpace() throws Exception {
    var main = bach().project().spaces().main();
    var modules = main.declarations();
    if (modules.isEmpty()) {
      bach().debug("Main module list is empty, nothing to build here.");
      return;
    }
    bach().print("Build %d main module%s", modules.size(), modules.size() == 1 ? "" : "s");

    var release = main.release();
    var feature = "" + (release == 0 ? Runtime.version().feature() : release);
    var classes = bach().folders().workspace("classes", main.name(), feature);

    bach().run(buildProjectMainJavac(release, classes)).requireSuccessful();

    Files.createDirectories(bach().folders().workspace("modules"));
    bach()
        .run(modules.map().values().stream().map(module -> buildProjectMainJar(module, classes)))
        .requireSuccessful();
  }

  /**
   * {@return the {@code javac} call to compile all modules of the main space}
   *
   * @param release the release
   */
  default Javac buildProjectMainJavac(int release, Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    return Command.javac()
        .ifTrue(release != 0, javac -> javac.add("--release", release))
        .add("--module", main.declarations().toNames(","))
        .add("--module-version", project.version())
        .ifPresent(main.modulePaths().list(), (javac, paths) -> javac.add("--module-path", paths))
        .ifTrue(bach().is(Options.Flag.STRICT), javac -> javac.add("-Xlint").add("-Werror"))
        .addAll(main.tweaks().arguments("javac"))
        .add("-d", classes);
  }

  default Jar buildProjectMainJar(ModuleDeclaration module, Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    var name = module.name();
    var file = bach().folders().workspace("modules", bach().computeMainJarFileName(name));
    var jar =
        Command.jar()
            .ifTrue(bach().is(Options.Flag.VERBOSE), command -> command.add("--verbose"))
            .add("--create")
            .add("--file", file)
            .addAll(main.tweaks().arguments("jar"))
            .addAll(main.tweaks().arguments("jar(" + name + ")"));
    var baseClasses = classes.resolve(name);
    if (Files.isDirectory(baseClasses)) jar = jar.add("-C", baseClasses, ".");
    return jar;
  }

  default void buildProjectTestSpace() throws Exception {
    var test = bach().project().spaces().test();
    var modules = test.declarations();
    if (modules.isEmpty()) {
      bach().debug("Test module list is empty, nothing to build here.");
      return;
    }
    bach().print("Build %d test module%s", modules.size(), modules.size() == 1 ? "" : "s");

    throw new UnsupportedOperationException("buildProjectTestSpace() needs some love");
  }
}
