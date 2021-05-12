package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javadoc;
import java.nio.file.Path;

public class GenerateDocumentationAction extends BachAction {

  public GenerateDocumentationAction(Bach bach) {
    super(bach);
  }

  public void generate() {
    generateApiDocumentation();
  }

  public void generateApiDocumentation() {
    var project = bach().project();
    if (!project.tools().enabled("javadoc")) return;
    if (project.spaces().main().modules().isEmpty()) {
      bach().log("Main module list is empty, nothing to build here.");
      return;
    }

    bach().say("Generate API documentation");

    var api = project.folders().workspace("documentation", "api");
    var zip = project.name() + "-api-" + project.version() + ".zip";
    bach().run(javadoc(api)).requireSuccessful();
    bach().run(jar(api, zip)).requireSuccessful();
  }

  public Javadoc javadoc(Path destination) {
    var project = bach().project();
    var main = project.spaces().main();
    var tweaks = project.tools().tweaks();
    return new Javadoc()
        .with("--module", main.modules().toNames(","))
        .forEach(
            main.modules().toModuleSourcePaths(false),
            (javadoc, path) -> javadoc.with("--module-source-path", path))
        .ifPresent(main.paths().pruned(), (javadoc, paths) -> javadoc.with("--module-path", paths))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javadoc"))
        .with("-d", destination);
  }

  public Jar jar(Path destination, String file) {
    return new Jar()
        .with("--create")
        .with("--file", destination.getParent().resolve(file))
        .with("--no-manifest")
        .with("-C", destination, ".");
  }
}
