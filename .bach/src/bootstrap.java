import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.spi.ToolProvider;

/** Compile and package Bach's main module. */
class bootstrap {
  public static void main(String... args) throws Exception {
    var module = "com.github.sormuras.bach";
    var version = Files.readString(Path.of("VERSION")) + "+BOOTSTRAP";

    var bin = Path.of(".bach/bin");
    delete(bin, "com.github.sormuras.bach@*.jar");
    deleteWorkspaces();

    var classes = Path.of(".bach/workspace/.bootstrap");
    run(
        "javac",
        "--module",
        module,
        "--module-source-path",
        "./*/main/java",
        "-g",
        "-parameters",
        "-Werror",
        "-Xlint",
        "-encoding",
        "UTF-8",
        "-d",
        classes.toString());

    var file = module + "@" + version + ".jar";
    var jar = Files.createDirectories(bin).resolve(file);
    run(
        "jar",
        "--verbose",
        "--create",
        "--file=" + jar,
        "--module-version=" + version + "+" + Instant.now().truncatedTo(ChronoUnit.SECONDS),
        "-C",
        classes.resolve(module).toString(),
        ".",
        "-C",
        Path.of(module).resolve("main/resources").toString(),
        ".",
        "-C",
        Path.of(module).resolve("main/java").toString(),
        ".");
    System.out.println("<< " + Files.size(jar));
  }

  static void delete(Path directory, String glob) throws Exception {
    try (var stream = Files.newDirectoryStream(directory, glob)) {
      for (var path : stream) {
        Files.delete(path);
        System.out.println(">> " + path + " deleted");
      }
    }
  }

  static void deleteWorkspaces() throws Exception {
    try (var stream = Files.walk(Path.of(""))) {
      var selected =
          stream
              .filter(path -> path.toUri().toString().contains(".bach/workspace"))
              .sorted(Comparator.reverseOrder())
              .toArray(Path[]::new);
      for (var path : selected) {
        Files.delete(path);
        // also delete non-empty ".bach" directory
        if (path.endsWith(Path.of(".bach", "workspace")))
          try {
            Files.deleteIfExists(path.getParent());
          } catch (DirectoryNotEmptyException ignore) {
          }
      }
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
