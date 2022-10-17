import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;

interface bach {

  Path BACH_SOURCE_FOLDER = Path.of(".bach", "src");
  Path BACH_VERSION_FILE = Path.of(".bach", "bach.version");
  Path GIT_IGNORE_FILE = Path.of(".bach", ".gitignore");

  static void main(String... args) throws Exception {
    // prepare handling of special case: "bach reset <version>"
    var reset = args.length > 0 && args[0].equals("reset");
    var version = reset && args.length > 1 ? args[1] : readBachVersionFromFileOrElseReturnMain();
    if (reset) {
      acquireArchive(version); // acquire first before deleting anything
      System.out.printf("Reset Bach to version %s in progress...%n", version);
      deleteTree(Path.of(".bach/bin"));
      deleteTree(BACH_SOURCE_FOLDER.resolve("run.bach"));
    }
    // extract and generate
    if (Files.notExists(BACH_SOURCE_FOLDER.resolve("run.bach"))) {
      extract(acquireArchive(version));
      //noinspection ResultOfMethodCallIgnored
      Path.of(".bach/bin/bach").toFile().setExecutable(true, true);
      if (Files.notExists(GIT_IGNORE_FILE)) Files.writeString(GIT_IGNORE_FILE, generateGitIgnore());
      Files.writeString(BACH_VERSION_FILE, version);
    }
    if (reset) {
      System.out.println("Reset done.");
      return;
    }
    // compile and run
    var modules = compileBachModules(BACH_SOURCE_FOLDER);
    var code = runBachTool(modules, args);
    if (code != 0) System.exit(code);
  }

  static Path compileBachModules(Path sources) {
    var classes = Path.of(".bach/bin/classes-" + Runtime.version().feature());
    var module = module(sources);
    run(
        "javac",
        "--module=" + module,
        "--module-source-path=" + sources,
        "-X" + "lint:all",
        "-W" + "error",
        "-d",
        classes.toString());
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

  static String readBachVersionFromFileOrElseReturnMain() throws Exception {
    return Files.exists(BACH_VERSION_FILE) ? Files.readAllLines(BACH_VERSION_FILE).get(0) : "main";
  }

  static void run(String name, String... args) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new RuntimeException(name + " -> " + code);
  }

  static void deleteTree(Path root) throws Exception {
    if (Files.notExists(root)) return;
    try (var stream = Files.walk(root)) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
    }
  }

  static Path acquireArchive(String version) throws Exception {
    var zip = Path.of(".bach/tmp/bach-archive-" + version + ".zip");
    if (Files.notExists(zip)) {
      Files.createDirectories(zip.getParent());
      load("https://github.com/sormuras/bach/archive/" + version + ".zip", zip);
    }
    // TODO Verify archive is signed
    return zip;
  }

  static String generateGitIgnore() {
    return """
           out/
           *.jar
           """;
  }

  static void load(String uri, Path file) throws Exception {
    System.out.printf("<< %s%n", uri);
    try (var stream = new URL(uri).openStream()) {
      var size = Files.copy(stream, file);
      System.out.printf(">> %,7d %s%n", size, file.getFileName());
    }
  }

  static void extract(Path zip) throws Exception {
    System.out.printf("<< %s%n", zip.toUri());
    var files = new ArrayList<Path>();
    try (var fs = FileSystems.newFileSystem(zip)) {
      for (var root : fs.getRootDirectories()) {
        try (var stream = Files.walk(root)) {
          var list = stream.filter(Files::isRegularFile).toList();
          for (var file : list) {
            var string = file.toString();
            if (string.contains(".bach/bin") || string.contains(".bach/src/run.bach")) {
              var target = Path.of(file.subpath(1, file.getNameCount()).toString());
              Files.createDirectories(target.getParent());
              Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
              files.add(target);
            }
          }
        }
      }
    }
    System.out.printf(">> %d files extracted%n", files.size());
  }
}
