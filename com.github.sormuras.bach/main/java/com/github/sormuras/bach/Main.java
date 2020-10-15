package com.github.sormuras.bach;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Bach's main program. */
public class Main {

  /**
   * Executes Bach's main program.
   *
   * @param args tthe command line arguments
   */
  public static void main(String... args) {
    new Main().run(args);
  }

  private final Consumer<String> out;
  private final Consumer<String> err;

  /** Initializes an object of the main program with default components. */
  public Main() {
    this(System.out::println, System.err::println);
  }

  /**
   * Initializes an object of the main program with the given components.
   *
   * @param out the line-based consumer of normal (expected) output messages
   * @param err the line-based consumer of error messages
   */
  public Main(Consumer<String> out, Consumer<String> err) {
    this.out = out;
    this.err = err;
  }

  /**
   * Builds a modular Java project.
   *
   * @param args the command line arguments
   */
  public void run(String... args) {
    out.accept("Bach in module " + getClass().getModule().getDescriptor().toNameAndVersion());
    out.accept("  - base directory: " + Path.of("").toAbsolutePath());

    var base = Path.of("");
    var workspace = base.resolve(".bach/workspace");
    var build = base.resolve(".bach/build");
    var cache = base.resolve(".bach/cache");

    if (Files.exists(build.resolve("module-info.java"))) {
      var classes = workspace.resolve("classes/.build");
      var runner = new ToolRunner(ModuleFinder.of(classes, cache));
      var compile =
          Command.of(
              "javac",
              "--module=build",
              "--module-source-path", base.resolve(".bach"),
              "--module-path", cache,
              "-d",
              classes);
      runner.run(compile).checkSuccessful();
      runner.run("build", args).checkSuccessful();
      runner.history().stream().map(ToolResponse::out).forEach(out);
      return;
    }

    err.accept("Zero-configuration build operation not implemented, yet");
    throw new UnsupportedOperationException();
  }
}
