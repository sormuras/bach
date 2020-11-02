package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ProjectBuilder;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.tool.Command;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.spi.ToolProvider;

public class BachBuilder implements ProjectBuilder {

  public static void main(String... args) {
    new BachBuilder().build(Bach.ofSystem(), args);
  }

  private static String version() {
    try {
      return Files.readString(Path.of("VERSION"));
    } catch (Exception exception) {
      throw new Error("Read VERSION failed: ", exception);
    }
  }

  public BachBuilder() {}

  @Override
  public void build(Bach bach, String... args) {
    var out = bach.printStream();
    var err = System.err;
    var version = args.length == 0 ? version() : args[0];
    var jarslug = args.length < 2 ? version : args[1];
    out.println("Build Bach " + version + " using Bach " + Bach.version());
    var info = getClass().getModule().getAnnotation(ProjectInfo.class);
    System.out.println(info);
    var start = Instant.now();
    try {
      new Simple(out, err, version, jarslug).run();
    } catch (Exception exception) {
      err.println(exception);
    } finally {
      out.printf("Build took %d milliseconds%n", Duration.between(start, Instant.now()).toMillis());
    }
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }

  static class Simple {

    final PrintStream out;
    final PrintStream err;
    final String version;
    final String jarslug;

    final Path workspace = Path.of(".bach/workspace");

    Simple(PrintStream out, PrintStream err, String version, String jarslug) {
      this.out = out;
      this.err = err;
      this.version = version;
      this.jarslug = jarslug;
    }

    void run() throws Exception {
      var module = "com.github.sormuras.bach";
      var classes = workspace.resolve("classes/" + Runtime.version().feature());
      var modules = Files.createDirectories(workspace.resolve("modules"));
      var file = modules.resolve(module + "@" + jarslug + ".jar");

      run(
          Command.builder("javac")
              .with("--module", module)
              .with("--module-source-path", "./*/main/java")
              .with("--release", 16)
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
              .with("--file", file)
              .with("--main-class", module + ".Main")
              .with("--module-version", version)
              .with("-C", classes.resolve(module), ".")
              .with("-C", Path.of(module, "main", "java"), ".")
              .build());

      var api = workspace.resolve("documentation/api");
      run(
          Command.builder("javadoc")
              .with("--module", module)
              .with("--module-source-path", "./*/main/java")
              .with("-windowtitle", "\uD83C\uDFBC Bach " + version)
              .with("-header", "\uD83C\uDFBC Bach " + version)
              .with("-footer", "\uD83C\uDFBC Bach " + version)
              .with("-use")
              .with("-linksource")
              .with("-notimestamp")
              .with("-Werror")
              .with("-Xdoclint")
              .with("-encoding", "UTF-8")
              .with("-quiet")
              .with("-d", api)
              .build());

      run(
          Command.builder("jar")
              .with("--create")
              .with("--file", api.getParent().resolve("bach-api-" + version + ".zip"))
              .with("--no-manifest")
              .with("-C", api, ".")
              .build());
    }

    void run(Command command) {
      out.printf("<< %s%n", command);
      var tool = ToolProvider.findFirst(command.name()).orElseThrow();
      var normal = new StringWriter();
      var errors = new StringWriter();
      var code = tool.run(new PrintWriter(normal), new PrintWriter(errors), command.args());

      if (!normal.getBuffer().isEmpty()) out.println(normal.toString().indent(3).stripTrailing());
      if (!errors.getBuffer().isEmpty()) err.println(errors);

      if (code != 0) throw new Error(tool.name() + " returned error code: " + code);
    }
  }
}
