import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolutionException;
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

    var bootstrap = Path.of(".bach/workspace/classes/bootstrap");
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
      throw new Error("Build program failed!", exception);
    }
  }

  static void run(ToolProvider tool, String... args) {
    System.out.println(">> " + tool.name() + " " + String.join(" ", args));
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }
}
