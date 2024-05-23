package run.bach.workflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/**
 * Well-known directories and files.
 *
 * @param root the project home directory that usually contains a {@code .bach/} subdirectory
 * @param dot is the "dot-bach" directory, defaults to {@code ${root}/.bach/}
 * @param out used to store generated files into, defaults to {@code ${root}/.bach/out/}
 * @param tmp used to store external tools into, defaults to {@code ${root}/.bach/tmp/}
 */
public record Folders(Path root, Path dot, Path out, Path tmp) {
  /** {@code .bach} */
  public static Folders ofCurrentWorkingDirectory() {
    return Folders.of(Path.of(""));
  }

  /** {@code C:\Users\${user.name}\.bach} */
  public static Folders ofUserHomeDirectory() {
    return Folders.of(Path.of(System.getProperty("user.home", "")));
  }

  /** {@code C:\Users\${user.name}\AppData\Local\Temp\.bach} */
  public static Folders ofTemporaryDirectory() {
    return Folders.of(Path.of(System.getProperty("java.io.tmpdir", "")));
  }

  /**
   * {@code C:\Users\${user.name}\AppData\Local\Temp\${prefix}17762885918332141894\.bach}
   *
   * @see Files#createTempDirectory(String, FileAttribute[])
   */
  public static Folders ofTemporaryDirectory(String prefix) {
    try {
      return Folders.of(Files.createTempDirectory(prefix));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static Folders of(Path root) {
    var normalized = root.normalize();
    var dot = normalized.resolve(".bach");
    var out = dot.resolve("out");
    var tmp = dot.resolve("tmp");
    return new Folders(normalized, dot, out, tmp);
  }

  public Path root(String first, String... more) {
    return root.resolve(first, more);
  }

  public Path out(String first, String... more) {
    return out.resolve(first, more);
  }

  public Path tool(String first, String... more) {
    return tmp.resolve("tool").resolve(first, more);
  }
}
