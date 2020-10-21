package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.MainArguments;
import com.github.sormuras.bach.internal.Paths;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
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
    var arguments = MainArguments.of(args);
    if (arguments.verbose()) {
      out.accept("Bach " + Bach.version());
      out.accept("  - arguments: [" + String.join(", ", args) + "]");
      out.accept("  - base directory: " + Path.of("").toAbsolutePath());
    }

    if (arguments.actions().isEmpty()) {
      out.accept(help());
      return;
    }

    var actions = new ArrayDeque<>(arguments.actions());
    while (!actions.isEmpty()) {
      var action = actions.removeFirst();
      if (arguments.verbose()) out.accept("action: " + action);
      switch (action) {
        case "build" -> {
          build(actions.toArray(String[]::new));
          actions.clear();
        }
        case "clean" -> clean();
        case "help", "/?" -> out.accept(help());
        case "version" -> out.accept(Bach.version());
        default -> throw new IllegalArgumentException("Unsupported action: " + action);
      }
    }
  }

  void build(String... args) {
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

  void clean() {
    var base = Path.of("");
    var workspace = base.resolve(".bach/workspace");
    Paths.deleteDirectories(workspace);
  }

  String help() {
    return """
        Usage: bach [options] action [actions/args...]
        
        Options:
          --verbose
                Output messages about what Bach and other tools are doing.
        
        Actions:
          build [args...]
                Build the modular Java project. Consumes all following arguments.
          clean
                Delete workspace directory.
          help, /?
                Print this help message.
          version
                Print Bach's version: %s
        """.formatted(Bach.version());
  }
}
