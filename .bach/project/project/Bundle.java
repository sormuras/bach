package project;

import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.spi.ToolProvider;

public class Bundle implements ToolProvider {
  @Override
  public String name() {
    return "bundle";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var zip = Path.of(args.length == 0 ? ".bach/out/bach.zip" : args[0]);
    try {
      Files.deleteIfExists(zip);
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
    var env = Map.of("create", "true");
    try (var target = FileSystems.newFileSystem(zip, env)) {
      // base
      Files.copy(Path.of("LICENSE"), target.getPath("LICENSE"));
      Files.copy(Path.of("README.md"), target.getPath("README.md"));
      // bin
      var bin = Files.createDirectories(target.getPath("bin"));
      Files.copy(Path.of("bin/bach"), bin.resolve("bach"));
      Files.copy(Path.of("bin/bach.bat"), bin.resolve("bach.bat"));
      Files.copy(Path.of(".bach/out/main/modules/run.bach.jar"), bin.resolve("run.bach.jar"));
      Files.copy(Path.of(".bach/out/main/modules/run.duke.jar"), bin.resolve("run.duke.jar"));
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
