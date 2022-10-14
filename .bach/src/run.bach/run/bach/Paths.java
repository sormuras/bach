package run.bach;

import java.nio.file.Path;

/** All about folders and files. */
public record Paths(Path root, Path out) {
  public static Paths ofRoot(Path root) {
    return new Paths(root, root.resolve(".bach/out"));
  }

  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  public Path out(String first, String... more) {
    return out.resolve(Path.of(first, more));
  }

  public Path externalModules() {
    return root(".bach", "external-modules");
  }

  public Path externalModules(String first, String... more) {
    return externalModules().resolve(Path.of(first, more));
  }

  public Path externalTools() {
    return root(".bach", "external-tools");
  }

  public Path externalTools(String first, String... more) {
    return externalTools().resolve(Path.of(first, more));
  }

  public Path javaHome() {
    return Path.of(System.getProperty("java.home"));
  }

  public String toString(int indent) {
    return """
            root             = %s
            out              = %s
            external-modules = %s
            external-tools   = %s
            """
        .formatted(
            root("").toUri(),
            out("").toUri(),
            externalModules("").toUri(),
            externalTools("").toUri())
        .indent(indent)
        .stripTrailing();
  }
}
