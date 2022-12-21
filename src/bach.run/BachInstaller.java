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

record BachInstaller(String version, Path home) {
  static String DEFAULT_VERSION =
      System.getProperty("-Default.version".substring(2), "early-access");
  static Path DEFAULT_LOCATION =
      Path.of(System.getProperty("-Default.location".substring(2), ".bach"));

  @SuppressWarnings("unused")
  static void installDefaultVersionIntoDefaultLocation() throws Exception {
    var installer = new BachInstaller(DEFAULT_VERSION);
    if (Internal.DEBUG) installer.listInstalledModules();
    installer.install();
    installer.listInstalledModules();
  }

  @SuppressWarnings("unused")
  static void listInstallableVersions() {
    System.out.println("- Default version: " + DEFAULT_VERSION);
    System.out.println("- Released version https://github.com/sormuras/bach/releases");
  }

  BachInstaller(String version) {
    this(version, DEFAULT_LOCATION);
  }

  void install() throws Exception {
    System.out.println("Install Bach " + version + " to " + home.toUri() + "...");
    var uri = "https://github.com/sormuras/bach";
    var tmp = Files.createTempDirectory("bach-" + version + "-");
    var dot = Files.createDirectories(tmp.resolve(".bach"));
    var src = uri + "/releases/download/" + version + "/bach@" + version + ".zip";
    var zip = tmp.resolve("bach-release-" + version + ".zip");
    // download and unzip
    Internal.copy(src, zip, StandardCopyOption.REPLACE_EXISTING);
    Internal.unzip(zip, dot, 0);
    // refresh binary directory
    var bin = home.resolve("bin");
    Internal.delete(bin);
    Files.createDirectories(bin);
    Files.copy(dot.resolve("bin/bach"), bin.resolve("bach")).toFile().setExecutable(true, true);
    Files.copy(dot.resolve("bin/bach.bat"), bin.resolve("bach.bat"));
    Files.copy(dot.resolve("bin/run.bach.jar"), bin.resolve("run.bach.jar"));
    Files.copy(dot.resolve("bin/run.duke.jar"), bin.resolve("run.duke.jar"));
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

    static void unzip(Path zip, Path dir, int sub) throws Exception {
      debug("<< %s".formatted(zip.toUri()));
      debug(">> %s".formatted(dir.toUri()));
      var files = new ArrayList<Path>();
      try (var fs = FileSystems.newFileSystem(zip)) {
        for (var root : fs.getRootDirectories()) {
          try (var stream = Files.walk(root)) {
            var list = stream.filter(Files::isRegularFile).toList();
            for (var file : list) {
              var relative = root.relativize(file);
              var source = sub == 0 ? relative : relative.subpath(sub, relative.getNameCount());
              var target = dir.resolve(source.toString());
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
  }

  public static void main(String... args) throws Exception {
    installDefaultVersionIntoDefaultLocation();
  }
}
