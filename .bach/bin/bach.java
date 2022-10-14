import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;

interface bach {
  static void main(String... args) throws Exception {
    var sources = acquireBachSources();
    var modules = compileBachModules(sources);
    var code = runBachTool(modules, args);
    if (code != 0) System.exit(code);
  }

  static Path acquireBachSources() {
    var sources = Path.of(".bach/src");
    if (!Files.isDirectory(sources)) throw new RuntimeException("No sources found: " + sources);
    return sources;
  }

  static Path compileBachModules(Path sources) {
    var classes = Path.of(".bach/bin/classes-" + Runtime.version().feature());
    var module = module(sources);
    run("javac", "--module=" + module, "--module-source-path=" + sources, "-d", classes.toString());
    return classes;
  }

  static int runBachTool(Path modules, String... args) throws Exception {
    var java = Path.of(System.getProperty("java.home"), "bin", "java");
    var process = new ProcessBuilder(java.toString(), "--module-path", modules.toString());
    process.command().add("--add-modules=ALL-DEFAULT"); // #217
    process.command().add("--module");
    process.command().add("run.bach/run.bach.Main");
    process.command().addAll(List.of(args));
    return process.inheritIO().start().waitFor();
  }

  static String module(Path sources) {
    try (var stream = Files.newDirectoryStream(sources, Files::isDirectory)) {
      var joiner = new StringJoiner(",");
      stream.forEach(dir -> joiner.add(dir.getFileName().toString()));
      return joiner.toString();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static void run(String name, String... args) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new RuntimeException(name + " -> " + code);
  }
}
