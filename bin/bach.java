import java.io.File;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

record bach(boolean verbose, Path home, Path root) {
  public static void main(String... args) throws Exception {
    var feature = Runtime.version().feature();
    if (feature < 17) {
      System.err.println("Java " + Runtime.version() + " in " + System.getProperty("java.home"));
      throw new AssertionError("At least Java 17 is required");
    }

    var arguments = List.of(args);
    var verbose = arguments.isEmpty() || arguments.indexOf("!") == 0;
    var root = computeRootPath(arguments).normalize(); // similar to Bach's CLI "--root-path[=]PATH"
    var home = computeBachHomePath(System.getProperty("bach.home")).normalize();
    var bach = new bach(verbose, home, root);

    if (arguments.indexOf("!") == 0) {
      if (arguments.indexOf("reset") == 1) {
        if (arguments.size() == 2) bach.reset();
        else bach.reset(/*version*/ arguments.get(2));
        return;
      }
      throw new UnsupportedOperationException(arguments.toString());
    }

    var finder = ModuleFinder.of(bach.home("bin"));
    if (finder.find("run.bach").isEmpty()) {
      if (Files.isRegularFile(bach.home("src", "run.bach", "module-info.java"))) {
        var src = bach.home("src");
        var jar = bach.home("bin", "run.bach.jar");
        bach.bootstrapBachModule(src, jar);
      } else {
        bach.reset();
      }
    }

    var code = bach.run(arguments);
    if (code != 0) System.exit(code);
  }

  static Path computeBachHomePath(String property) throws Exception {
    if (property != null) return Path.of(property);
    var code = bach.class.getProtectionDomain().getCodeSource();
    if (code == null) throw new AssertionError("No code source available for: " + bach.class);
    var location = code.getLocation();
    if (location == null) throw new AssertionError("No location available for code: " + code);
    var path = Path.of(location.toURI());
    if (path.endsWith("bin/bach.java")) return path.getParent().getParent();
    if (path.endsWith("bach.java")) return path.getParent();
    return path; // "bach.class" in a directory structure or in a JAR file
  }

  static Path computeRootPath(List<String> arguments) {
    var index = arguments.indexOf("--root-path");
    if (index >= 0) return Path.of(arguments.get(index + 1));
    var argument = arguments.stream().filter(arg -> arg.startsWith("--root-path=")).findFirst();
    if (argument.isPresent()) {
      var arg = argument.get();
      var val = arg.substring(arg.indexOf('=') + 1);
      return Path.of(val);
    }
    return Path.of("");
  }

  static void delete(Path root) throws Exception {
    if (Files.notExists(root)) return;
    try (var stream = Files.walk(root)) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
    }
  }

  static String join(List<Path> paths) {
    var stream = paths.stream().filter(Files::exists);
    var mapped = stream.map(Path::toString).map(entry -> entry.isEmpty() ? "." : entry);
    var distinct = mapped.distinct().toList();
    if (distinct.isEmpty()) throw new IllegalArgumentException("No valid entry in: " + paths);
    return String.join(File.pathSeparator, distinct);
  }

  Path home(String first, String... more) {
    return home.resolve(Path.of(first, more));
  }

  Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  void bootstrapBachModule(Path sources, Path jar) {
    var module = "run.bach";
    var classes = home("out", ".bach", "classes-" + Runtime.version().feature());
    verbose("Compile %s module from %s to %s".formatted(module, sources, classes));
    run(
        "javac",
        "--module=" + module,
        "--module-source-path=" + sources,
        "-X" + "lint:all",
        "-W" + "error",
        "-d",
        classes);
    run(
        "jar",
        "--create",
        "--file=" + jar,
        "--main-class=" + module + ".Main",
        "-C",
        classes.resolve(module),
        ".");
  }

  Optional<Path> buildProjectModule() {
    if (Files.notExists(root(".bach", "project", "module-info.java"))) return Optional.empty();
    var feature = Runtime.version().feature();
    var sources = root(".bach");
    var classes = root(".bach", "out", ".bach", "classes-" + feature);

    verbose("Compile project module from %s to %s".formatted(sources, classes));
    run(
        "javac",
        "--module=project",
        "--module-source-path=" + sources, // with project/module-info.java
        "--module-path=" + home("bin"), // with run.bach
        "-X" + "lint:all",
        "-d",
        classes);

    return Optional.of(classes);
  }

  void reset() throws Exception {
    var console = System.console();
    if (console == null) throw new IllegalStateException("No console for reading user input");
    var version =
        console.readLine(
            """
            Enter version of Bach to install.
            >\s""");
    reset(version == null ? "?" : version);
  }

  void reset(String version) throws Exception {
    if (version.isBlank() || List.of("?", "/?", "-?", "-h", "--help").contains(version)) {
      System.out.print(
          """
          Usage: bach ! reset <version>

              A reset is performed by replacing a set of directory trees below Bach's
              home folder with files extracted from a versioned Bach archive.

          Values for version include: "main", "HEAD", git tags, and git commit SHAs.
          """);
      return;
    }
    verbose("Reset to version %s in progress...".formatted(version));
    // load to temp
    var tmp = home("out", "tmp");
    var zip = tmp.resolve("bach-archive-" + version + ".zip");
    if (Files.notExists(zip)) {
      download("https://github.com/sormuras/bach/archive/" + version + ".zip", zip);
    }
    var sources = tmp.resolve("bach-archive-" + version);
    unzip(zip, sources);
    var jar = tmp.resolve("bach-" + version + ".jar");
    bootstrapBachModule(sources.resolve("src"), jar);
    var git = Files.isDirectory(home(".git"));
    if (git) {
      System.out.println("Found .git directory in " + home());
      return;
    }
    var bin = home("bin");
    delete(bin);
    Files.createDirectories(bin);
    Files.copy(sources.resolve("bin/bach"), home("bin/bach")).toFile().setExecutable(true, true);
    Files.copy(sources.resolve("bin/bach.bat"), home("bin/bach.bat"));
    Files.copy(sources.resolve("bin/bach.java"), home("bin/bach.java"));
    Files.copy(jar, home("bin/run.bach.jar"));
    verbose("Reset done.");
  }

  void download(String uri, Path file) throws Exception {
    verbose("<< %s".formatted(uri));
    Files.createDirectories(file.getParent());
    try (var stream = new URL(uri).openStream()) {
      var size = Files.copy(stream, file);
      verbose(">> %,7d %s".formatted(size, file.getFileName()));
    }
  }

  void unzip(Path zip, Path dir) throws Exception {
    verbose("<< %s".formatted(zip.toUri()));
    var files = new ArrayList<Path>();
    try (var fs = FileSystems.newFileSystem(zip)) {
      var binaries = fs.getPathMatcher("glob:/*/bin/*");
      var sources = fs.getPathMatcher("glob:/*/src/**");
      for (var root : fs.getRootDirectories()) {
        try (var stream = Files.walk(root)) {
          var list = stream.filter(Files::isRegularFile).toList();
          for (var file : list) {
            if (binaries.matches(file) || sources.matches(file)) {
              var target = dir.resolve(file.subpath(1, file.getNameCount()).toString());
              verbose(target.toUri().toString());
              Files.createDirectories(target.getParent());
              Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
              files.add(target);
            }
          }
        }
      }
    }
    verbose(">> %d files copied".formatted(files.size()));
  }

  void run(String name, Object... args) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var strings = Stream.of(args).map(Object::toString).toList();
    verbose(name + " " + String.join(" ", strings));
    var code = tool.run(System.out, System.err, strings.toArray(String[]::new));
    if (code != 0) throw new RuntimeException(name + " returned error code " + code);
  }

  int run(List<String> arguments) throws Exception {
    var modulePaths = new ArrayList<>(List.of(home("bin")));
    buildProjectModule().ifPresent(modulePaths::add);

    var java = Path.of(System.getProperty("java.home"), "bin", "java" /*.exe*/);
    var process = new ProcessBuilder(java.toString());
    process.command().add("--module-path=" + join(modulePaths));
    process.command().add("--add-modules=ALL-DEFAULT,ALL-MODULE-PATH");
    process.command().add("--module=run.bach");
    process.command().addAll(arguments);
    verbose(String.join(" ", process.command()));
    return process.inheritIO().start().waitFor();
  }

  void verbose(String message) {
    if (verbose) System.out.println(message);
  }
}
