package build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.spi.ToolProvider;

class Bootstrap {
  public static void main(String... args) throws Exception {
    var module = "com.github.sormuras.bach";
    var version = "16-ea+BOOTSTRAP";

    deleteWorkspaces();

    var classes = Path.of(".bach/workspace/.bootstrap");
    run(
        "javac",
        "--module=" + module,
        "--module-version=" + version + "-" + Instant.now(),
        "--module-source-path=./*/main/java",
        "-g",
        "-parameters",
        "-Werror",
        "-Xlint",
        "-encoding",
        "UTF-8",
        "-d",
        classes.toString());

    var file = module + "@" + version + ".jar";
    var jar = Files.createDirectories(Path.of(".bach/cache")).resolve(file);
    run(
        "jar",
        "--create",
        "--file=" + jar,
        "--main-class=" + module + ".Main",
        "-C",
        classes.resolve(module).toString(),
        ".");
  }

  static void deleteWorkspaces() throws Exception {
    try (var stream = Files.walk(Path.of(""))) {
      var selected = stream
          .filter(path -> path.toUri().toString().contains(".bach/workspace"))
          .sorted(Comparator.reverseOrder())
          .toArray(Path[]::new);
      for (var path : selected) Files.delete(path);
      System.out.println(">> " + selected.length + " workspace files deleted");
    }
  }

  static void run(String name, String... args) {
    System.out.println(">> " + name + " " + String.join(" ", args));
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }
}
