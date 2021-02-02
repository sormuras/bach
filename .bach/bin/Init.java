import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Bach's Initialization class used by Bach's Init Script. */
class Init {

  /** Default initialization program. */
  public static void main(String... args) throws Exception {
    var version = args.length == 0 ? "17-ea" : args[0];
    var current = Path.of("").toAbsolutePath();

    System.out.printf("Initialize Bach %s in directory: %s%n", version, current);
    Files.createDirectories(Path.of(".bach/bin"));

    loadScript("bach").toFile().setExecutable(true);
    loadScript("bach.bat");
    loadScript("boot.jsh");
    loadModule("com.github.sormuras.bach", version);

    System.out.printf("%nBach %s initialized.%n", version);
  }

  static Path loadScript(String name) throws Exception {
    var uri = "https://github.com/sormuras/bach/raw/main/.bach/bin/" + name;
    return copy(uri, name);
  }

  static void loadModule(String name, String version) throws Exception {
    var jar = name + "@" + version + ".jar";
    var uri = "https://github.com/sormuras/bach/releases/download/" + version + "/" + jar;
    copy(uri, jar);
  }

  static Path copy(String uri, String name) throws Exception {
    var file = Path.of(".bach", "bin", name);
    try (var stream = new URL(uri).openStream()) {
      var size = Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
      System.out.printf("%,7d %s%n", size, file);
    }
    return file;
  }

  /** Hidden default constructor. */
  private Init() {}
}
