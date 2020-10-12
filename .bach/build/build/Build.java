package build;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
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

    final String version = "15-ea";
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
              .with("--module-source-path=.bach" + File.pathSeparator + "./*/main/java")
              .with("--module-version=" + version)
              .with("-Werror")
              .with("-Xlint")
              .with("-Xdoclint:-missing")
              .with("-encoding", "UTF-8")
              .with("-d", classes)
              .build());

      run(
          Command.builder("jar")
              .with("--create")
              .with("--file=" + file)
              .with("-C", classes.resolve(module), ".")
              .build());

      run(Command.of("jar", "--describe-module", "--file", file));
    }

    void run(Command command) {
      out.printf("<< %s%n", command);
      var response = runner.run(command);
      if (!response.out().isBlank()) out.println(response.out());
      if (!response.err().isBlank()) err.println(response.err());
      response.checkSuccessful();
    }
  }
}
