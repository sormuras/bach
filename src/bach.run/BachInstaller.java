import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;

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
    Thread.sleep(1234);
    throw new Error("Not implemented, yet.");
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
}
