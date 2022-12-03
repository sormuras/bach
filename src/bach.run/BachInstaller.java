import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

record BachInstaller(String version, Path home) {

  static String DEFAULT_VERSION = System.getProperty("-Default.version".substring(2), "main");
  static String DEFAULT_LOCATION = System.getProperty("-Default.location".substring(2), ".bach");

  @SuppressWarnings("unused")
  static void installDefaultVersionIntoDefaultLocation() throws Exception {
    var installer = new BachInstaller(DEFAULT_VERSION);
    installer.listInstalledModules();
    installer.install();
    installer.listInstalledModules();
  }

  @SuppressWarnings("unused")
  static void listInstallableVersions() {
    System.out.println("- Default version: " + DEFAULT_VERSION);
    System.out.println("- Released version https://github.com/sormuras/bach/releases");
    System.out.println("- Commit hash from https://github.com/sormuras/bach/commits/main");
  }

  BachInstaller(String version) {
    this(version, Path.of(DEFAULT_LOCATION));
  }

  void install() throws Exception {
    System.out.println("Install Bach " + version + " to " + home.toUri());
    // download sources to temporary directory
    var src = "https://github.com/sormuras/bach/archive/" + version + ".zip";
    var tmp = Files.createTempDirectory("bach-" + version + "-");
    var zip = tmp.resolve("bach-archive-" + version + ".zip");
    Internal.copy(src, zip, StandardCopyOption.REPLACE_EXISTING);
    // unzip and mark bash script as executable
    var from = tmp.resolve("bach-archive-" + version); // unzipped
    Internal.unzip(zip, from);
    // noinspection ResultOfMethodCallIgnored
    from.resolve("bin/bach").toFile().setExecutable(true, true);
    // build bach
    Internal.Shell.bach(from, "--piano", "compile");
    // refresh binary directory
    var bin = home.resolve("bin");
    Internal.delete(bin);
    Files.createDirectories(bin);
    Files.copy(from.resolve("bin/bach"), bin.resolve("bach"));
    Files.copy(from.resolve("bin/bach.bat"), bin.resolve("bach.bat"));
    Files.copy(from.resolve(".bach/out/main/modules/run.bach.jar"), bin.resolve("run.bach.jar"));
    Files.copy(from.resolve(".bach/out/main/modules/run.duke.jar"), bin.resolve("run.duke.jar"));
    // clean up
    Internal.delete(tmp);
  }

  void listInstalledModules() {
    var bin = home.resolve("bin");
    if (Files.isDirectory(bin)) {
      System.out.println("Installed modules in " + bin.toUri());
      ModuleFinder.of(bin).findAll().stream()
          .map(ModuleReference::descriptor)
          .map(ModuleDescriptor::toNameAndVersion)
          .sorted()
          .forEach(System.out::println);
    }
  }

  private interface Internal {
    boolean DEBUG = Boolean.getBoolean("-Debug".substring(2));

    static void debug(String message) {
      if (DEBUG) System.out.println(message);
    }

    static void copy(String source, Path target, CopyOption... options) throws Exception {
      debug("<< %s".formatted(source));
      Files.createDirectories(target.getParent());
      try (var stream =
                   source.startsWith("http")
                           ? URI.create(source).toURL().openStream()
                           : Files.newInputStream(Path.of(source))) {
        var size = Files.copy(stream, target, options);
        debug(">> %,7d %s".formatted(size, target.getFileName()));
      }
    }

    static void delete(Path path) throws Exception {
      var start = path.normalize().toAbsolutePath();
      if (Files.notExists(start)) return;
      for (var root : start.getFileSystem().getRootDirectories()) {
        if (start.equals(root)) {
          debug("deletion of root directory?! " + path);
          return;
        }
      }
      debug("delete directory tree " + start);
      try (var stream = Files.walk(start)) {
        var files = stream.sorted((p, q) -> -p.compareTo(q));
        for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
      }
    }

    static void unzip(Path zip, Path dir) throws Exception {
      debug("<< %s".formatted(zip.toUri()));
      var files = new ArrayList<Path>();
      try (var fs = FileSystems.newFileSystem(zip)) {
        for (var root : fs.getRootDirectories()) {
          try (var stream = Files.walk(root)) {
            var list = stream.filter(Files::isRegularFile).toList();
            for (var file : list) {
              var target = dir.resolve(file.subpath(1, file.getNameCount()).toString());
              // debug(target.toUri().toString());
              Files.createDirectories(target.getParent());
              Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
              files.add(target);
            }
          }
        }
      }
      debug(">> %d files copied".formatted(files.size()));
    }

    interface Shell {
      static void bach(Path directory, String... arguments) throws Exception {
        var builder = new ProcessBuilder();
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        builder.directory(directory.toFile());
        var command = builder.command();
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
          command.add("cmd.exe");
          command.add("/c");
          command.add("bin\\bach.bat");
        } else {
          command.add("bin/bach");
        }
        command.addAll(List.of(arguments));
        debug("%s -> %s".formatted(builder.directory(), builder.command()));
        record LinePrinter(InputStream stream, Consumer<String> consumer) implements Runnable {
          @Override
          public void run() {
            new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
          }
        }
        var process = builder.start();
        new Thread(new LinePrinter(process.getInputStream(), System.out::println)).start();
        new Thread(new LinePrinter(process.getErrorStream(), System.err::println)).start();
        int code = process.waitFor();
        if (code != 0) throw new RuntimeException("Non-zero exit code " + code);
      }
    }
  }
}
