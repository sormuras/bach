import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

record bach(boolean verbose, Path home, Path root) {
  public static void main(String... args) throws Exception {
    var arguments = List.of(args);
    var verbose = arguments.indexOf("--verbose") == 0; // similar to Bach's CLI "--verbose" flag
    var root = computeRootPath(arguments).normalize(); // similar to Bach's CLI "--root-path=PATH"
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

    var modulePaths = new ArrayList<Path>();
    /* "run.bach" */ {
      var bin = bach.home().resolve("bin");
      if (ModuleFinder.of(bin).find("run.bach").isEmpty()) {
        var sources = bach.home().resolve("src");
        var classes = bach.home().resolve("out/bootstrap/classes-" + Runtime.version().feature());
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
        var classes = sources.resolve("out/.bach/classes/java-" + Runtime.version().feature());
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
    return path; // "bach.class" in a directory structure or in a JAR file
  }

  static String join(List<Path> paths) {
    var stream = paths.stream().filter(Files::exists);
    var mapped = stream.map(Path::toString).map(entry -> entry.isEmpty() ? "." : entry);
    var distinct = mapped.distinct().toList();
    if (distinct.isEmpty()) throw new IllegalArgumentException("No valid entry in: " + paths);
    return String.join(File.pathSeparator, distinct);
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
