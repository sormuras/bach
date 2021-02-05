import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Bach's init program. */
class init {

  public static void main(String... args) throws Exception {
    var version = args.length == 0 ? "17-ea" : args[0];
    System.out.printf("Initialize Bach %s in %s%n", version, Path.of("").toAbsolutePath());

    var bin = Path.of(".bach/bin");
    if (Files.isDirectory(bin)) {
      var module = ModuleFinder.of(bin).find("com.github.sormuras.bach");
      if (module.isPresent()) Files.delete(Path.of(module.get().location().orElseThrow()));
    } else {
      Files.createDirectories(bin);
    }

    loadScript("bach").toFile().setExecutable(true);
    loadScript("bach.bat");
    loadScript("boot.java");
    loadScript("boot.jsh");
    loadScript("init.java");
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
}
