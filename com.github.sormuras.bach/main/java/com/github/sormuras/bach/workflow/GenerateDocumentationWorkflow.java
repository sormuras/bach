package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.tool.JarCall;
import com.github.sormuras.bach.tool.JavadocCall;
import java.nio.file.Path;

public class GenerateDocumentationWorkflow extends BachWorkflow {

  public GenerateDocumentationWorkflow(Bach bach) {
    super(bach);
  }

  public void generate() {
    generateApiDocumentation();
  }

  public void generateApiDocumentation() {
    var project = bach().project();
    if (!project.tools().enabled("javadoc")) return;
    if (project.spaces().main().modules().isEmpty()) {
      bach().log("Main module list is empty, nothing to do here.");
      return;
    }

    bach().say("Generate API documentation");

    var api = project.folders().workspace("documentation", "api");
    var zip = project.name() + "-api-" + project.version() + ".zip";
    bach().run(javadoc(api)).requireSuccessful();
    bach().run(jar(api, zip)).requireSuccessful();
  }

  protected JavadocCall javadoc(Path destination) {
    var project = bach().project();
    var main = project.spaces().main();
    var tweaks = project.tools().tweaks();
    return new JavadocCall()
        .with("--module", main.modules().toNames(","))
        .forEach(
            main.modules().toModuleSourcePaths(false),
            (javadoc, path) -> javadoc.with("--module-source-path", path))
        .ifPresent(main.paths().pruned(), (javadoc, paths) -> javadoc.with("--module-path", paths))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javadoc"))
        .with("-d", destination);
  }

  protected JarCall jar(Path destination, String file) {
    return new JarCall()
        .with("--create")
        .with("--file", destination.getParent().resolve(file))
        .with("--no-manifest")
        .with("-C", destination, ".");
  }
}
