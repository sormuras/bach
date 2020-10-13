package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolResponse;
import com.github.sormuras.bach.ToolRunner;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public class Build implements ToolProvider {

  public static void main(String... args) {
    new Build().run(System.out, System.err, args);
  }

  public Build() {}

  @Override
  public String name() {
    return "build";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      new Simple(out, err).run();
      return 0;
    } catch (Exception e) {
      return 1;
    }
  }

  static class Simple {

    final PrintWriter out;
    final PrintWriter err;

    final String version = System.getProperty("bach.project.version", "15-ea");
    final Path workspace = Path.of(".bach/workspace");
    final ToolRunner runner = new ToolRunner();

    Simple(PrintWriter out, PrintWriter err) {
      this.out = out;
      this.err = err;
    }

    void run() throws Exception {
      out.println("Build " + Bach.class.getModule() + "@" + version);

      var module = "com.github.sormuras.bach";
      var classes = workspace.resolve("classes/" + Runtime.version().feature());
      var modules = Files.createDirectories(workspace.resolve("modules"));
      var file = modules.resolve(module + "@" + version + ".jar");

      run(
          Command.builder("javac")
              .with("--module=" + module)
              .with("--module-version=" + version)
              .with("--module-source-path=.bach" + File.pathSeparator + "./*/main/java")
              .with("--release=15")
              .with("-g")
              .with("-parameters")
              .with("-Werror")
              .with("-Xlint")
              .with("-encoding", "UTF-8")
              .with("-d", classes)
              .build());

      run(
          Command.builder("jar")
              .with("--create")
              .with("--file=" + file)
              .with("-C", classes.resolve(module), ".")
              .with("-C", Path.of(module, "main", "java"), ".")
              .build());

      var description = run(Command.of("jar", "--describe-module", "--file", file)).out();
      if (!description.startsWith(module + '@' + version))
        throw new AssertionError("Module name and version not found at the start!");

      var list = run(Command.of("jar", "--list", "--file", file), false).out();
      if (list.lines().noneMatch(line -> line.endsWith(".java")))
        throw new AssertionError("No Java source file found in JAR file: " + file);

      var api = workspace.resolve("documentation/api");
      run(
          Command.builder("javadoc")
              .with("--module=" + module)
              .with("--module-source-path=.bach" + File.pathSeparator + "./*/main/java")
              .with("-windowtitle", "\uD83C\uDFBC Bach " + version)
              .with("-header", "\uD83C\uDFBC Bach " + version)
              .with("-footer", "\uD83C\uDFBC Bach " + version)
              .with("-use")
              .with("-linksource")
              .with("-notimestamp")
              .with("-Werror")
              .with("-Xdoclint")
              .with("-encoding", "UTF-8")
              .with("-d", api)
              .build(),
          false);

      run(
          Command.builder("jar")
              .with("--create")
              .with("--file=" + api.getParent().resolve("bach-" + version + "-api.zip"))
              .with("--no-manifest")
              .with("-C", api, ".")
              .build());
    }

    ToolResponse run(Command command) {
      return run(command, true);
    }

    ToolResponse run(Command command, boolean verbose) {
      out.printf("<< %s%n", command);
      var response = runner.run(command);
      var normal = response.out();
      var errors = response.err();
      if (!normal.isBlank() && verbose) out.println(normal.indent(3).stripTrailing());
      if (!errors.isBlank()) err.println(errors);
      response.checkSuccessful();
      return response;
    }
  }
}
