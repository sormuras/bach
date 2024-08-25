package run.demo;

import run.bach.*;
import run.info.bach.*;

class ToolVersionsDemo {
  public static void main(String... args) {
    // 1-shot, tool provider
    Tool.of("jar").run("--version");

    // 1-shot, tool program
    Tool.of("java").run("--version");

    // 1-shot, tool installer
    Tool.of("https://github.com/rife2/bld/releases/download/2.0.1/bld-2.0.1.jar").run("version");
    Tool.of(new Ant(), ToolInstaller.Mode.INSTALL_IMMEDIATE).run("-version");

    // multi-shot, tool finder
    var finder =
        ToolFinder.ofInstaller(ToolInstaller.Mode.INSTALL_IMMEDIATE)
            .with(new Ant())
            .withJavaApplication(JResolve.ID, JResolve.URI)
            .withJavaApplication(
                "rife2/bld@2.0.1",
                "https://github.com/rife2/bld/releases/download/2.0.1/bld-2.0.1.jar")
            .withJavaApplication(
                "org.junit.platform/junit@1.11.0",
                "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.0/junit-platform-console-standalone-1.11.0.jar")
            .with("run.bach/google-java-format@1.23", new GoogleJavaFormat("1.23.0"))
            .with("run.bach/google-java-format@1.19", new GoogleJavaFormat("1.19.2"))
            .with(new Maven("3.9.9"));

    var runner = ToolRunner.of(finder);
    runner.run("ant", "-version");
    runner.run("bld", "version");
    runner.run("junit", "--version");
    runner.run("jresolve", "--version");
    runner.run("google-java-format", "--version");
    runner.run("google-java-format@1.19", "--version");
    runner.run("maven", "--version");
  }
}
