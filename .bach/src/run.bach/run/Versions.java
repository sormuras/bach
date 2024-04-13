package run;

import run.bach.*;

class Versions {
  public static void main(String... args) {
    // 1-shot, tool provider
    Tool.of("jar").run("--version");

    // 1-shot, tool program
    Tool.of("java").run("--version");

    // 1-shot, tool installer
    Tool.of("https://github.com/rife2/bld/releases/download/1.9.0/bld-1.9.0.jar").run("version");
    Tool.of(new Ant(), ToolInstaller.Mode.INSTALL_IMMEDIATE).run("-version");

    // multi-shot, tool finder
    var finder =
        ToolFinder.ofInstaller(ToolInstaller.Mode.INSTALL_IMMEDIATE)
            .with(new Ant())
            .withJavaApplication(
                "rife2/bld@1.9.0",
                "https://github.com/rife2/bld/releases/download/1.9.0/bld-1.9.0.jar")
            .withJavaApplication(
                "org.junit.platform/junit@1.10.2",
                "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar")
            .with("run.bach/google-java-format@1.22", new GoogleJavaFormat("1.22.0"))
            .with("run.bach/google-java-format@1.19", new GoogleJavaFormat("1.19.2"))
            .with(new Maven());

    var runner = ToolRunner.of(finder);
    runner.run("ant", "-version");
    runner.run("bld", "version");
    runner.run("junit", "engines");
    runner.run("google-java-format", "--version");
    runner.run("google-java-format@1.19", "--version");
    runner.run("maven", "--version");
  }
}
