package run;

import run.bach.Bach;
import run.bach.ToolCall;

class Build {
  private static final Bach.Folders folders = Bach.Folders.ofCurrentWorkingDirectory();

  public static void main(String... args) {
    var classes = folders.out("classes");
    var modules = folders.out("modules");
    var moduleSourcePath = folders.computeModuleSourcePath("run.bach", ".bach/src/run.bach");

    ToolCall.of("javac")
        .add("--module", "run.bach")
        .add("--module-source-path", moduleSourcePath)
        .add("-X" + "lint:all")
        .add("-W" + "error")
        .add("-d", classes)
        .run();

    ToolCall.of("jar", "--create")
        .when(Boolean.getBoolean("-Debug".substring(2)), "--verbose")
        .add("--file", modules.resolve("run.bach.jar"))
        .add("-C", classes.resolve("run.bach"), ".")
        .run();
  }
}
