package run;

import run.bach.*;

class Versions {
  public static void main(String... args) {
    // 1-shot, tool provider
    Tool.of("jar").run("--version");

    // 1-shot, tool program
    Tool.of("java").run("--version");

    // 1-shot, tool installer
    new GoogleJavaFormat().tool("1.22.0").run("--version");

    // JReleaser.tool(cache, "1.11.0").run("--version");
    // Ant.tool("1.10.14", tools.cache()).run("-version");
    // Maven.tool(cache, "3.9.6").run("--version");

    // canonical
    var finder =
        ToolFinder.ofInstaller()
            .with(new GoogleJavaFormat().installation("1.21.0"))
            .with(new GoogleJavaFormat().installation("1.22.0"))
        // JReleaser.tool(cache, "1.11.0"),
        // Ant.tool("1.10.14", tools.cache())
        // Maven.tool(cache, "3.9.6")
        ;
    var runner = ToolRunner.of(finder);
    runner.run("google-java-format@1.21.0", "--version");
    runner.run("google-java-format@1.22.0", "--version");
    //    runner.run("jreleaser", "--version");
    //    runner.run("ant", "-version");
    //    runner.run("maven", "-version");
  }
}
