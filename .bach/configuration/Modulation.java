package configuration;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Flag;
import com.github.sormuras.bach.Libraries;
import com.github.sormuras.bach.Libraries.JUnit;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.GitHubReleasesModuleLookup;
import com.github.sormuras.bach.lookup.Maven;
import com.github.sormuras.bach.lookup.ToolProvidersModuleLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class Modulation extends Bach {

  public static void main(String... args) {
    var options = Options.of(args.length == 0 ? new String[] {"build"} : args);
    System.exit(new Main().run(provider().newBach(options)));
  }

  public static Factory<Modulation> provider() {
    return options -> {
      options.flags().add(Flag.VERBOSE);
      options.flags().add(Flag.RUN_COMMANDS_SEQUENTIALLY);
      return new Modulation(options);
    };
  }

  private Modulation(Options options) {
    super(options);
  }

  @Override
  protected Libraries newLibraries() {
    return new Libraries(
        JUnit.V_5_7_1,
        new ExternalModuleLookup("junit", Maven.central("junit", "junit", "4.13.1")),
        new ExternalModuleLookup("org.hamcrest", Maven.central("org.hamcrest", "hamcrest", "2.2")),
        new GitHubReleasesModuleLookup(this),
        new ToolProvidersModuleLookup(this, Bach.EXTERNALS));
  }

  @Override
  protected Project newProject() {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(now);
    var version = Project.defaultVersion(readVersionFromFile() + "-custom+" + timestamp);
    return super.newProject().version(version);
  }

  private static String readVersionFromFile() {
    try {
      return Files.readString(Path.of("VERSION"));
    } catch (Exception exception) {
      throw new RuntimeException("Read version failed: " + exception);
    }
  }

  @Override
  public void buildMainSpace() throws Exception {
    var module = "com.github.sormuras.bach";
    var moduleVersion = project().version();
    var version = project().versionNumberAndPreRelease();
    var javaRelease = 16;
    var destination = base().workspace("classes", "main", "" + javaRelease);
    var modules = base().workspace("modules");

    run(
        Command.javac()
            .add("--release", javaRelease)
            .add("--module", module)
            .add("--module-version", moduleVersion)
            .add("--module-source-path", "./*/main/java")
            .add("--module-path", Bach.EXTERNALS)
            .add("-encoding", "UTF-8")
            .add("-g")
            .add("-parameters")
            .add("-Xlint")
            .add("-Werror")
            .add("-d", destination));

    Files.createDirectories(modules);
    var file = modules.resolve(computeMainJarFileName(module));
    run(
        Command.jar()
            .add("--verbose")
            .add("--create")
            .add("--file", file)
            .add("--main-class", module + ".Main")
            .add("-C", destination.resolve(module), ".")
            .add("-C", base().directory(module, "main/java"), "."));

    run(
        Command.javadoc()
            .add("--module", module)
            .add("--module-source-path", "./*/main/java")
            .add("--module-path", Bach.EXTERNALS)
            .add("-encoding", "UTF-8")
            .add("-windowtitle", "🎼 Bach " + version)
            .add("-doctitle", "🎼 Bach " + version)
            .add("-header", "🎼 Bach " + version)
            .add("-notimestamp")
            .add("-Xdoclint:-missing")
            .add("-Werror")
            .add("-d", base().workspace("documentation", "api")));

    generateMavenConsumerPom(module, version, file);
  }

  private void generateMavenConsumerPom(String module, String version, Path file) throws Exception {
    var maven = Files.createDirectories(base().workspace("deploy", "maven"));

    var pom =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>

          <groupId>com.github.sormuras.bach</groupId>
          <artifactId>${MODULE}</artifactId>
          <version>${VERSION}</version>

          <name>Bach</name>
          <description>🎼 Java Shell Builder</description>

          <url>https://github.com/sormuras/bach</url>
          <scm>
            <url>https://github.com/sormuras/bach.git</url>
          </scm>

          <developers>
            <developer>
              <name>Christian Stein</name>
              <id>sormuras</id>
            </developer>
          </developers>

          <licenses>
            <license>
              <name>MIT License</name>
              <url>https://opensource.org/licenses/mit-license</url>
              <distribution>repo</distribution>
            </license>
          </licenses>

          <dependencies>
            <!-- system module: java.base -->
            <!-- system module: java.net.http -->
            <!-- system module: jdk.compiler -->
            <!-- system module: jdk.crypto.ec -->
            <!-- system module: jdk.jartool -->
            <!-- system module: jdk.javadoc -->
            <!-- system module: jdk.jdeps -->
            <!-- system module: jdk.jlink -->
          </dependencies>
        </project>
        """
            .replace("${MODULE}", module)
            .replace("${VERSION}", version);

    // deploy/maven/${MODULE}.pom.xml
    var pomFile = Files.writeString(maven.resolve(module + ".pom.xml"), pom);

    // deploy/maven/empty.zip
    // https://central.sonatype.org/pages/requirements.html#supply-javadoc-and-sources
    var emptyZip = maven.resolve("empty.zip");
    byte[] bytes = {0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    Files.write(emptyZip, bytes);

    // deploy/maven/${MODULE}.files
    var files =
        new StringJoiner(" ")
            .add("-DpomFile=" + pomFile)
            .add("-Dfile=" + file)
            .add("-Dsources=" + emptyZip)
            .add("-Djavadoc=" + emptyZip);
    Files.writeString(maven.resolve(module + ".files"), files.toString());
  }
}