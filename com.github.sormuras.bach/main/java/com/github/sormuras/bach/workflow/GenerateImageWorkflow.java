package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.tool.JLinkCall;
import java.nio.file.Path;

public class GenerateImageWorkflow extends BachWorkflow {

  public GenerateImageWorkflow(Bach bach) {
    super(bach);
  }

  public void generate() {
    generateCustomRuntimeImage();
  }

  public void generateCustomRuntimeImage() {
    var project = bach().project();
    if (!project.tools().enabled("jlink")) return;
    if (project.spaces().main().modules().isEmpty()) {
      bach().log("Main module list is empty, nothing to do here.");
      return;
    }

    bach().say("Generate custom runtime image");

    var image = project.folders().workspace("image");
    Paths.deleteDirectories(image);
    bach().run(jlink(image));
  }

  protected JLinkCall jlink(Path image) {
    var project = bach().project();
    var main = project.spaces().main();
    var test = project.spaces().test();
    var tweaks = project.tools().tweaks();
    return new JLinkCall()
        .with("--add-modules", main.modules().toNames(","))
        .ifPresent(test.paths().pruned(), (jlink, paths) -> jlink.with("--module-path", paths))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "jlink"))
        .with("--output", image);
  }
}
