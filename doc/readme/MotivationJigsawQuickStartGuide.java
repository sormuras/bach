import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

class MotivationJigsawQuickStartGuide {

  public static void main(String... args) throws Exception {
    var base = Path.of("doc", "project", "JigsawQuickStart");

    var module = "com.greetings";
    var classes = Files.createDirectories(base.resolve("build/classes"));
    runTool("javac", "--module=" + module, "--module-source-path=" + base, "-d", classes);

    var modules = Files.createDirectories(base.resolve("build/modules"));
    var file = modules.resolve(module + ".jar");
    runTool("jar", "--create", "--file=" + file, "-C", classes.resolve(module), ".");

    var image = deleteDirectories(base.resolve("build/image"));
    var addModules = "--add-modules=" + module;
    var modulePath = "--module-path=" + modules;
    var launcher = "--launcher=greet=" + module + "/com.greetings.Main";
    var output = "--output=" + image;
    runTool("jlink", addModules, modulePath, launcher, output);
  }

  static void runTool(String name, Object... arguments) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
    System.out.printf("%-8s %s%n", name, String.join(" ", args));
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new AssertionError("Non-zero exit code: " + code);
  }

  static Path deleteDirectories(Path root) throws IOException {
    if (Files.notExists(root)) return root;
    try (var stream = Files.walk(root)) {
      var paths = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
    }
    return root;
  }
}
