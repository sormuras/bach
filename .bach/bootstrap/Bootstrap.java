import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolutionException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

class Bootstrap {
  public static void main(String[] args) {
    System.out.println("Bootstrap Bach");

    System.out.println("Delete workspace");
    var workspace = deleteDirectories(Path.of(".bach/workspace"));
    var bootstrap = workspace.resolve("classes/bootstrap");
    var pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmm").withZone(ZoneId.of("UTC"));
    var version = ModuleDescriptor.Version.parse(pattern.format(Instant.now()));

    run(
        ToolProvider.findFirst("javac").orElseThrow(),
        "--module=build,com.github.sormuras.bach",
        "--module-source-path=.bach" + File.pathSeparator + "./*/main/java",
        "--module-version=" + version + "-BOOTSTRAP",
        "-Werror",
        "-Xlint",
        "-encoding",
        "UTF-8",
        "-d",
        bootstrap.toString());

    run(tool(ModuleFinder.of(bootstrap), "build"));

    System.out.println("END.");
  }

  static void run(ToolProvider tool, String... args) {
    System.out.println(">> " + tool.name() + " " + String.join(" ", args));
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  static ToolProvider tool(ModuleFinder finder, String name) {
    var parent = Bootstrap.class.getClassLoader();
    try {
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), Set.of());
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
      var layer = controller.layer();
      var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
      return serviceLoader.stream()
          .map(ServiceLoader.Provider::get)
          .filter(tool -> tool.name().equals(name))
          .findFirst()
          .orElseThrow();
    } catch (FindException | ResolutionException exception) {
      throw new Error("No such tool found: " + name, exception);
    }
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
