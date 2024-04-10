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
public final class GoogleJavaFormat implements ToolInstaller {
  public static void main(String... args) {
    var installer = new GoogleJavaFormat();
    var version = System.getProperty("version", installer.version());
    installer.tool(version).run(args.length == 0 ? new String[] {"--version"} : args);
  }

  @Override
  public String name() {
    return "google-java-format";
  }

  /**
   * @see <a href="https://github.com/google/google-java-format/releases/latest">Latest release</a>
   */
  public String version() {
    return "1.22.0";
  }

  @Override
  public ToolProvider install(String version, Path into) {
    var filename = "google-java-format-" + version + "-all-deps.jar";
    var target = into.resolve(filename);
    if (!Files.exists(target)) {
      var releases = "https://github.com/google/google-java-format/releases/download/";
      var source = releases + "v" + version + "/" + filename;
      download(target, URI.create(source));
    }
    return ToolProgram.findJavaDevelopmentKitTool("java", "-jar", target.toString()).orElseThrow();
  }
}
