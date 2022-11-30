package run.bach;

import java.nio.file.Path;

/** All about folders and files. */
public record Folders(Path root, Path out, Path externalModules, Path externalTools) {

  public static final Folders CURRENT_WORKING_DIRECTORY = Folders.ofRoot(Path.of(""));

  public static Folders ofRoot(Path root) {
    return new Folders(
        root,
        root.resolve(".bach/out"),
        root.resolve(".bach/external-modules"),
        root.resolve(".bach/external-tools"));
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
}
