import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

class BuildJigsawQuickStartWorld {

  public static void main(String... arguments) throws Exception {
    var base = Path.of("doc", "project", "JigsawQuickStartWorld");
    var build = new BuildJigsawQuickStartWorld(base);

    var mainModules = Set.of("com.greetings", "org.astro");
    var mainSourcePath = "--module-source-path=" + base + "/*/main";
    var mainModulesDirectory = build.compile("main", mainModules, List.of(mainSourcePath));
    {
      var api = Files.createDirectories(base.resolve("build/api"));
      var overview =
          Files.write(
              api.resolve("overview-body.html"),
              List.of(
                  "<body>",
                  "<p>Inspired by Project Jigsaw: Module System Quick-Start Guide</p>",
                  "</body>"));
      var args = new ArrayList<>();
      args.add("--module=" + String.join(",", mainModules));
      args.add(mainSourcePath);
      args.add("-linksource");
      args.add("-doctitle");
      args.add("Bach.java<br>BuildJigsawQuickStartWorld.java");
      args.add("-overview");
      args.add(overview);
      args.add("-quiet");
      args.add("-d");
      args.add(api);
      runTool("javadoc", args.toArray());
    }
    {
      var image = deleteDirectories(base.resolve("build/image"));
      var args = new ArrayList<>();
      args.add("--add-modules=" + String.join(",", mainModules));
      args.add("--module-path=" + mainModulesDirectory);
      args.add("--launcher=greet=com.greetings/com.greetings.Main");
      args.add("--output=" + image);
      runTool("jlink", args.toArray());
    }

    var testModules = Set.of("test.modules");
    var testSourcePath = "--module-source-path=" + base + "/*/test";
    var testModulesDirectory =
        build.compile(
            "test", testModules, List.of(testSourcePath, "--module-path=" + mainModulesDirectory));
    {
      var testTool = findTestTool("test.modules", testModulesDirectory, mainModulesDirectory);
      testTool.ifPresent(tool -> runTool(tool));
    }
  }

  final Path base;

  BuildJigsawQuickStartWorld(Path base) {
    this.base = base;
  }

  Path compile(String realm, Set<String> modules, List<Object> javac) throws Exception {
    System.out.printf("%n[%s]%n%n", realm);
    var classes = Files.createDirectories(base.resolve("build/classes/" + realm));
    {
      var args = new ArrayList<>();
      args.add("--module=" + String.join(",", modules));
      args.addAll(javac);
      args.add("-d");
      args.add(classes);
      runTool("javac", args.toArray());
    }
    var path = Files.createDirectories(base.resolve("build/modules/" + realm));
    for (var mod : modules) {
      runTool(
          "jar",
          "--create",
          "--file=" + path.resolve(mod + ".jar"),
          "-C",
          classes.resolve(mod),
          ".");
    }
    return path;
  }

  static Optional<ToolProvider> findTestTool(String module, Path... modulePaths) {
    var roots = Set.of(module);
    var finder = ModuleFinder.of(modulePaths);
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
    var parent = ClassLoader.getPlatformClassLoader();
    var controller = ModuleLayer.defineModulesWithManyLoaders(configuration, List.of(boot), parent);
    var layer = controller.layer();
    var loader = layer.findLoader(module);
    loader.setDefaultAssertionStatus(true);
    var services = ServiceLoader.load(layer, ToolProvider.class);
    var providers = services.stream().map(ServiceLoader.Provider::get);
    return providers.filter(provider -> provider.name().equals("test(" + module + ")")).findFirst();
  }

  static void runTool(String name, Object... arguments) {
    runTool(ToolProvider.findFirst(name).orElseThrow(), arguments);
  }

  static void runTool(ToolProvider tool, Object... arguments) {
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
    System.out.printf("%-8s %s%n", tool.name(), String.join(" ", args));
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new AssertionError("Non-zero exit code: " + code);
  }

  static Path deleteDirectories(Path root) throws Exception {
    if (Files.notExists(root)) return root;
    try (var stream = Files.walk(root)) {
      var paths = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
    }
    return root;
  }
}
