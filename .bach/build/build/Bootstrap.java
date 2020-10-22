package build;

import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.spi.ToolProvider;

class Bootstrap {
  public static void main(String... args) throws Exception {
    var module = "com.github.sormuras.bach";
    var version = "15-ea+BOOTSTRAP";

    var classes = deleteDirectories(Path.of(".bach/workspace")).resolve("classes/.bootstrap");
    run(
        "javac",
        "--module=" + module + ",build",
        "--module-version=" + version + "-" + Instant.now(),
        "--module-source-path=./*/main/java" + File.pathSeparator + ".bach",
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
        "-C",
        classes.resolve(module).toString(),
        ".");
  }

  static void run(String name, String... args) {
    System.out.println(">> " + name + " " + String.join(" ", args));
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  static Path deleteDirectories(Path directory) {
    try { // trivial case: delete existing empty directory or single file
      Files.deleteIfExists(directory);
      return directory;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    } catch (Exception exception) {
      throw new Error("Delete directories failed: " + directory, exception);
    }
    try (var stream = Files.walk(directory)) { // default case: walk the tree...
      var selected = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.toArray(Path[]::new)) Files.deleteIfExists(path);
    } catch (Exception exception) {
      throw new Error("Delete directories failed: " + directory, exception);
    }
    return directory;
  }
}
