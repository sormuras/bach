import java.io.File;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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
    var verbose = arguments.indexOf("--verbose") == 0; // similar to Bach's CLI "--verbose" flag
    var root = computeRootPath(arguments).normalize(); // similar to Bach's CLI "--root-path[=]PATH"
    var home = computeBachHomePath(System.getProperty("bach.home")).normalize();
    var java = Path.of(System.getProperty("java.home"), "bin", "java" /*.exe*/);
    var bach = new bach(arguments.isEmpty() || verbose, home, root);

    bach.verbose(
        """
        bach.java %s
          arguments  = %s
          bach home  = "%s" (%s)
          root path  = "%s" (%s)
          java(.exe) = %s
        """
            .formatted(
                bach.class.getProtectionDomain().getCodeSource(),
                arguments,
                bach.home(),
                bach.home().toUri(),
                bach.root(),
                bach.root().toUri(),
                java));

    var reset = arguments.indexOf("reset");
    if (reset >= 0) {
      var version = arguments.size() <= reset + 1 ? "main" : arguments.get(reset + 1);
      bach.reset(version);
      main("--version");
      return;
    }

    var modulePaths = new ArrayList<Path>();
    /* "run.bach" */ {
      var bin = bach.home().resolve("bin");
      if (ModuleFinder.of(bin).find("run.bach").isEmpty()) {
        var sources = bach.home().resolve("src");
        var classes = bach.home().resolve("out/bootstrap/classes-" + feature);
        bach.verbose("Compile %s module from %s to %s".formatted("run.bach", sources, classes));
        if (!Files.isRegularFile(sources.resolve("run.bach" + "/module-info.java"))) {
          throw new UnsupportedOperationException("acquire sources");
        }
        bach.run(
            "javac",
            "--module=" + "run.bach",
            "--module-source-path=" + sources,
            "-X" + "lint:all",
            "-W" + "error",
            "-d",
            classes);
        bach.run(
            "jar",
            "--create",
            "--file=" + bin.resolve("run.bach@0-ea.jar"),
            "--main-class=run.bach.Main",
            "-C",
            classes.resolve("run.bach"),
            ".");
      }
      bach.verbose("Found %s module in %s".formatted("run.bach", bin));
      modulePaths.add(bin);
    }
    /* "project" */ {
      var name = "project";
      var sources = bach.root().resolve(".bach");
      if (Files.isRegularFile(sources.resolve(name + "/module-info.java"))) {
        var classes = sources.resolve("out/.bach/classes/java-" + feature);
        bach.verbose("Compile %s module from %s to %s".formatted(name, sources, classes));
        bach.run(
            "javac",
            "--module=project",
            "--module-source-path=" + sources,
            "--module-path=" + join(modulePaths),
            "-X" + "lint:all",
            "-d",
            classes);
        modulePaths.add(classes);
      }
    }
    var process = new ProcessBuilder(java.toString());
    process.command().add("--module-path=" + join(modulePaths));
    process.command().add("--add-modules=ALL-DEFAULT,ALL-MODULE-PATH");
    process.command().add("--module=run.bach");
    process.command().addAll(arguments);
    bach.verbose(String.join(" ", process.command()));
    var code = process.inheritIO().start().waitFor();
    if (code != 0) System.exit(code);
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

  static Path computeBachHomePath(String property) throws Exception {
    if (property != null) return Path.of(property);
    var code = bach.class.getProtectionDomain().getCodeSource();
    if (code == null) throw new AssertionError("No code source available for: " + bach.class);
    var location = code.getLocation();
    if (location == null) throw new AssertionError("No location available for code: " + code);
    var path = Path.of(location.toURI());
    if (path.endsWith("bin/bach.java")) return path.getParent().getParent();
    if (path.endsWith("bach.java")) return path.getParent();
    var env = System.getenv("BACH_HOME");
    if (env != null) return Path.of(env);
    return path; // "bach.class" in a directory structure or in a JAR file
  }

  static String join(List<Path> paths) {
    var stream = paths.stream().filter(Files::exists);
    var mapped = stream.map(Path::toString).map(entry -> entry.isEmpty() ? "." : entry);
    var distinct = mapped.distinct().toList();
    if (distinct.isEmpty()) throw new IllegalArgumentException("No valid entry in: " + paths);
    return String.join(File.pathSeparator, distinct);
  }

  void deleteTree(Path root) throws Exception {
    if (Files.notExists(root)) return;
    try (var stream = Files.walk(root)) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
    }
  }

  void reset(String version) throws Exception {
    var env = System.getenv("BACH_HOME");
    if (env != null && Path.of(env).equals(home)) {
      throw new AssertionError("No reset in BACH_HOME = " + env);
    }
    if (version.isBlank() || List.of("?", "/?", "-?", "-h", "--help").contains(version)) {
      System.out.print(
          """
              Usage: bach reset <version>

                  A reset is performed by replacing a set of directory trees below the
                  BACH_HOME folder with files extracted from a versioned Bach archive.

              Values for version include: "main", "HEAD", git tags, and git commit SHAs.
              """);
      return;
    }
    System.out.printf("Reset Bach to version %s in progress...%n", version);
    var tmp = home.resolve("out/tmp");
    deleteTree(tmp);
    var zip = tmp.resolve("bach-archive-" + version + ".zip");
    // get archive first before deleting any existing sources
    if (Files.notExists(zip)) {
      download("https://github.com/sormuras/bach/archive/" + version + ".zip", zip);
    }
    deleteTree(home.resolve("bin"));
    deleteTree(home.resolve("src/run.bach"));
    /* unzip archive */ {
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
                var target = home.resolve(file.subpath(1, file.getNameCount()).toString());
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
    System.out.println("Reset done.");
  }

  void download(String uri, Path file) throws Exception {
    verbose("<< %s".formatted(uri));
    Files.createDirectories(file.getParent());
    try (var stream = new URL(uri).openStream()) {
      var size = Files.copy(stream, file);
      verbose(">> %,7d %s".formatted(size, file.getFileName()));
    }
  }

  void run(String name, Object... args) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var strings = Stream.of(args).map(Object::toString).toList();
    verbose(name + " " + String.join(" ", strings));
    var code = tool.run(System.out, System.err, strings.toArray(String[]::new));
    if (code != 0) throw new RuntimeException(name + " -> " + code);
  }

  void verbose(String text) {
    if (verbose) {
      System.out.println(text);
    }
  }
}
