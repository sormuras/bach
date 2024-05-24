package run.demo;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.stream.Stream;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.ToolRunner;

public class JResolveDemo {
  public static void main(String... args) {
    var runner = ToolRunner.ofSilence();
    var jresolve =
        Tool.of(
            "https://github.com/bowbahdoe/jresolve-cli/releases/download/v2024.05.10/jresolve.jar#SIZE=754432");
    var run =
        runner.run(
            jresolve,
            "pkg:maven/org.junit.jupiter/junit-jupiter-engine@5.10.2",
            "pkg:maven/org.junit.platform/junit-platform-console@1.10.2");

    var paths = Stream.of(run.out().split(File.pathSeparator)).map(Path::of).toArray(Path[]::new);
    var junit = ToolFinder.of(ModuleFinder.of(paths)).get("junit");
    junit.run("--help");
  }
}
