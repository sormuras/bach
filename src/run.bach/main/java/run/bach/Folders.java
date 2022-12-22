package run.bach;

import java.nio.file.Path;

/** All about folders and files. */
public record Folders(Path root, Path out, Path externalModules, Path externalTools) {

  public static final Folders CURRENT_WORKING_DIRECTORY = Folders.of(Path.of(""), Path.of(".bach/out"));

  public static Folders of(Path root) {
    return Folders.of(root, root.resolve(".bach/out"));
  }

  public static Folders of(Path root, Path out) {
    var dot = root.resolve(".bach");
    return new Folders(root, out, dot.resolve("external-modules"), dot.resolve("external-tools"));
  }

  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  public Path out(String first, String... more) {
    return out.resolve(Path.of(first, more));
  }

  public Path externalModules(String first, String... more) {
    return externalModules.resolve(Path.of(first, more));
  }

  public Path externalTools(String first, String... more) {
    return externalTools.resolve(Path.of(first, more));
  }

  public Path javaHome() {
    return Path.of(System.getProperty("java.home"));
  }

  public Path javaHome(String first, String... more) {
    return javaHome().resolve(Path.of(first, more));
  }
}
