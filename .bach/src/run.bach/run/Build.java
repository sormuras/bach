package run;

import java.nio.file.Path;
import run.bach.ToolCall;

class Build {
  public static void main(String... args) {
    var sources = Path.of(".bach/src");
    var classes = Path.of(".bach/out/classes");
    var modules = Path.of(".bach/out/modules");

    ToolCall.of("javac")
        .add("--module", "run.bach")
        .add("--module-source-path", sources)
        .add("-Xlint:all")
        .add("-Werror")
        .add("-d", classes)
        .run();

    ToolCall.of("jar", "--create")
        .when(Boolean.getBoolean("-Debug".substring(2)), "--verbose")
        .add("--file", modules.resolve("run.bach.jar"))
        .add("-C", classes.resolve("run.bach"), ".")
        .run();
  }
}
