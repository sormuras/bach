package run.demo;

import run.bach.*;
import run.info.bach.*;

class ToolVersionsDemo {
  static void main() {
    // 1-shot, tool provider
    Tool.of("jar").run("--version");

    // 1-shot, tool program
    Tool.of("java").run("--version");

    // 1-shot, tool installer
    Tool.of("https://github.com/rife2/bld/releases/download/2.3.0/bld-2.3.0.jar").run("version");
    Tool.of(new Ant(), ToolInstaller.Mode.INSTALL_IMMEDIATE).run("-version");

    // multi-shot, tool finder
    var finder =
        ToolFinder.ofInstaller(ToolInstaller.Mode.INSTALL_IMMEDIATE)
            .with(new Ant())
            .with(JResolve.URI)
            .with("https://github.com/rife2/bld/releases/download/2.3.0/bld-2.3.0.jar")
            .withJavaApplication(
                "maven/org.junit.platform/junit@6.0.1",
                "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/6.0.1/junit-platform-console-standalone-6.0.1.jar")
            .with("run.bach/google-java-format@1.32", new GoogleJavaFormat("1.32.0"))
            .with("run.bach/google-java-format@1.23", new GoogleJavaFormat("1.23.0"))
            .with("run.bach/google-java-format@1.19", new GoogleJavaFormat("1.19.2"))
            .withJavaApplication(
                "maven/toolbox@0.14.3",
                "https://repo.maven.apache.org/maven2/eu/maveniverse/maven/plugins/toolbox/0.14.3/toolbox-0.14.3-cli.jar")
            .with(new Maven("3.9.11"));

    var runner = ToolRunner.of(finder);
    runner.run("ant", "-version");
    runner.run("bld", "version");
    runner.run("junit", "--version");
    runner.run("jresolve-cli", "--version");
    runner.run("google-java-format", "--version");
    runner.run("google-java-format@1.19", "--version");
    runner.run("toolbox", "--version");
    runner.run("maven", "--version");
  }
}
