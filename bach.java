import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolutionException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

interface bach {
  static void main(String... args) {
    if (args.length == 2) {
      if ("version".equals(args[0])) {
        version(Version.parse(args[1]));
        return;
      }
    }

    var cache = Path.of(".bach/cache");
    find("bach", ModuleFinder.of(cache))
        .ifPresentOrElse(
            bach -> bach.run(System.out, System.err, args),
            () -> System.err.println("Bach not found in directoy: " + cache)
        );
  }

  static void version(Version version) {
    var cache = Path.of(".bach/cache");
    var module = "com.github.sormuras.bach";
    if (Files.isDirectory(cache))
      try (var stream = Files.newDirectoryStream(cache, module + '*')) {
        stream.forEach(bach::delete);
      }
      catch (Exception exception) { throw new Error("version() failed", exception); }
    var jar = module + '@' + version + ".jar";
    var source = "https://github.com/sormuras/bach/releases/download/" + version + '/' + jar;
    load(source, cache.resolve(jar));
  }

  private static void delete(Path path) {
    try { Files.deleteIfExists(path); }
    catch (Exception exception) { throw new Error("delete() failed", exception); }
  }

  private static void load(String source, Path target) {
    System.out.println("  " + target.getFileName() + " << " + source);
    try (var stream = new URL(source).openStream()) {
      if (target.getParent() != null) Files.createDirectories(target.getParent());
      Files.copy(stream, target);
    }
    catch (Exception exception) { throw new Error("load() failed", exception); }
  }

  private static Optional<ToolProvider> find(String name, ModuleFinder finder) {
    try {
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(ModuleFinder.of(), finder, Set.of());
      var parent = ClassLoader.getSystemClassLoader();
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
      var layer = controller.layer();
      var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
      return serviceLoader.stream()
          .map(ServiceLoader.Provider::get)
          .filter(tool -> tool.name().equals(name))
          .findFirst();
    } catch (FindException | ResolutionException exception) {
      throw new Error("Loading tool provider failed for name: " + name, exception);
    }
  }
}
