package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Options.Flag;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.Recording;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

public class CustomBach extends Bach {

  public static void main(String... args) {
    var options = Options.of(args.length == 0 ? new String[] {"build"} : args);
    var bach = provider().newBach(options);
    System.exit(new Main().run(bach));
  }

  public static Provider<CustomBach> provider() {
    return options -> new CustomBach(options.with(Flag.VERBOSE));
  }

  private CustomBach(Options options) {
    super(options);
  }

  @Override
  public String computeProjectVersion(ProjectInfo info) {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(now);
    try {
      return Files.readString(Path.of("VERSION")) + "-custom+" + timestamp;
    } catch (Exception exception) {
      throw new RuntimeException("Read version failed: " + exception);
    }
  }

  @Override
  public void buildProjectMainSpace() throws Exception {
    var module = "com.github.sormuras.bach";
    var moduleVersion = project().version();
    var version = project().versionNumberAndPreRelease();
    var javaRelease = 16;
    var destination = folders().workspace("classes", "main", "" + javaRelease);
    var modules = folders().workspace("modules");

    run(Command.javac()
            .add("--release", javaRelease)
            .add("--module", module)
            .add("--module-version", moduleVersion)
            .add("--module-source-path", "./*/main/java")
            .add("--module-path", folders().externalModules())
            .add("-encoding", "UTF-8")
            .add("-g")
            .add("-parameters")
            .add("-Xlint")
            .add("-Werror")
            .add("-d", destination))
        .requireSuccessful();

    Files.createDirectories(modules);
    var file = modules.resolve(computeMainJarFileName(module));
    run(Command.jar()
            .add("--verbose")
            .add("--create")
            .add("--file", file)
            .add("--main-class", module + ".Main")
            .add("-C", destination.resolve(module), ".")
            .add("-C", folders().root(module, "main/java"), "."))
        .requireSuccessful();

    run(Command.javadoc()
            .add("--module", module)
            .add("--module-source-path", "./*/main/java")
            .add("--module-path", folders().externalModules())
            .add("-encoding", "UTF-8")
            .add("-windowtitle", "🎼 Bach " + version)
            .add("-doctitle", "🎼 Bach " + version)
            .add("-header", "🎼 Bach " + version)
            .add("-notimestamp")
            .add("-Xdoclint:-missing")
            .add("-Werror")
            .add("-d", folders().workspace("documentation", "api")))
        .requireSuccessful();

    var pom = generateMavenConsumerPom(module, version, file);
    run(PomChecker.install(this).checkMavenCentral(pom)).requireSuccessful();
  }

  @Override
  public void buildProjectTestSpace() throws Exception {
    var module = "com.github.sormuras.bach";
    var names = List.of(module, "test.base", "test.integration", "test.projects");
    var mainModules = folders().workspace("modules");
    var destination = folders().workspace("classes", "test");
    var moduleSourcePath = String.join(File.pathSeparator, "./*/test/java", "./*/test/java-module");
    run(Command.javac()
            .add("--module", String.join(",", names))
            .add("--module-source-path", moduleSourcePath)
            .add("--module-path", mainModules, folders().externalModules())
            .add("--patch-module", module + "=" + module + "/main/java")
            .add("-encoding", "UTF-8")
            .add("-g")
            .add("-parameters")
            .add("-Xlint")
            .add("-d", destination))
        .requireSuccessful();

    var testModules = folders().workspace("modules-test");
    Files.createDirectories(testModules);
    for (var name : names) jarTestModule(destination, name, testModules);
    for (var name : names) runTestModule(testModules, name);
    var errors = recordings().stream().filter(Recording::isError).toList();
    if (errors.isEmpty()) return;
    for (var recording : errors) options().err().print(recording);
    throw new RuntimeException(errors.size() + " test run(s) failed");
  }

  private void jarTestModule(Path classes, String module, Path modules) {
    var file = modules.resolve(module + "+test.jar");
    run(Command.jar()
            .add("--verbose")
            .add("--create")
            .add("--file", file)
            .add("-C", classes.resolve(module), ".")
            .add("-C", folders().root(module, "test/java"), "."))
        .requireSuccessful();
  }

  private void runTestModule(Path testModules, String module) {
    var finder =
        ModuleFinder.of(
            testModules.resolve(module + "+test.jar"), // module under test
            folders().workspace("modules"), // main modules
            testModules, // (more) test modules
            folders().externalModules());

    var junit =
        Command.of("junit")
            .add("--select-module", module)
            .add("--fail-if-no-tests")
            .add("--reports-dir", folders().workspace("reports", "junit", module));

    run(junit, finder, module);
  }

  private Path generateMavenConsumerPom(String module, String version, Path file) throws Exception {
    var maven = Files.createDirectories(folders().workspace("deploy", "maven"));

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
    return pomFile;
  }
}
