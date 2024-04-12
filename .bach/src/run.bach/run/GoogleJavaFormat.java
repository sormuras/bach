package run;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

/**
 * @see <a href="https://github.com/google/google-java-format">Google Java Format</a>
 */
public record GoogleJavaFormat(String version) implements ToolInstaller {
  /**
   * @see <a href="https://github.com/google/google-java-format/releases/latest">Latest release</a>
   */
  public static final String DEFAULT_VERSION = "1.22.0";

  public static void main(String... args) {
    var version = System.getProperty("version", DEFAULT_VERSION);
    new GoogleJavaFormat(version)
        .install()
        .run(args.length == 0 ? new String[] {"--version"} : args);
  }

  public GoogleJavaFormat() {
    this(DEFAULT_VERSION);
  }

  @Override
  public String name() {
    return "google-java-format";
  }

  @Override
  public ToolProvider installInto(Path into) {
    var filename = "google-java-format-" + version + "-all-deps.jar";
    var target = into.resolve(filename);
    if (!Files.exists(target)) {
      var releases = "https://github.com/google/google-java-format/releases/download/";
      var source = releases + "v" + version + "/" + filename;
      download(target, URI.create(source));
    }
    return ToolProgram.java("-jar", target.toString());
  }
}
