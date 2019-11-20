import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

public class Bootstrap {
  public static void main(String... args) throws Exception {
    var destination = Path.of(".bach", "bootstrap").toString();
    var pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").withZone(ZoneId.of("UTC"));
    var version = ModuleDescriptor.Version.parse(pattern.format(Instant.now()));

    run(
        "javac",
        "-d",
        destination,
        "--module=de.sormuras.bach",
        "--module-source-path=src/*/main/java",
        "--module-version=" + version + "-BOOTSTRAP",
        "-encoding",
        "UTF-8",
        "-Werror",
        "-X" + "lint");
    delete(Path.of(".bach/out"));
    start(
        ProcessHandle.current().info().command().orElse("java"),
        "-D" + "user.language=en",
        "--module-path",
        destination,
        "--add-modules",
        "de.sormuras.bach",
        "src/bach/Build.java");
  }

  static void delete(Path root) throws Exception {
    if (Files.notExists(root)) return;
    try (var stream = Files.walk(root)) {
      var selected = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    }
  }

  static void run(String name, String... args) {
    System.out.printf(">> %s %s%n", name, String.join(" ", args));
    var tool = ToolProvider.findFirst(name).orElseThrow(() -> new RuntimeException(name));
    int code = tool.run(System.out, System.err, args);
    if (code != 0) {
      throw new Error("Non-zero exit code: " + code);
    }
  }

  static void start(String... command) throws Exception {
    System.out.println(">> " + String.join(" ", command));
    var process = new ProcessBuilder(command).inheritIO().start();
    if (process.waitFor() != 0) {
      throw new Error("Non-zero exit");
    }
  }
}
