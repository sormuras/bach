import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.Tweak;
import com.github.sormuras.bach.call.CompileMainSpaceJavacCall;
import com.github.sormuras.bach.call.JUnitCall;
import com.github.sormuras.bach.call.TestCall;
import com.github.sormuras.bach.external.ExternalModuleLocation;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.external.Maven;
import com.github.sormuras.bach.project.PatchMode;
import com.github.sormuras.bach.workflow.BuildWorkflow;
import com.github.sormuras.bach.Checkpoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.StringJoiner;

class build {
  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/src/logging.properties");
    Bach.build(bach(args));
  }

  static MyBach bach(String... args) {
    var options = Options.of(args);
    return new MyBach(project(options), settings(options));
  }

  static Project project(Options options) {
    return Project.of("bach", "17-ea")
        .assertJDK(version -> version.feature() >= 16, "JDK 16+ is required")
        .assertJDK(Runtime.version().feature())
        .withDefaultSourceFileEncoding("UTF-8")
        .withMainSpace(
            main ->
                main.withJavaRelease(16)
                    .withModuleSourcePaths("./*/main/java")
                    .withModule(
                        "com.github.sormuras.bach/main/java/module-info.java",
                        module -> module.withResources("com.github.sormuras.bach/main/java")))
        .withTestSpace(
            test ->
                test.withModule("test.base/test/java/module-info.java")
                    .withModule("test.integration/test/java/module-info.java")
                    .withModule(
                        "test.projects/test/java/module-info.java",
                        module -> module.withResources("test.projects/test/resources"))
                    .withModule(
                        "com.github.sormuras.bach/test/java-module/module-info.java",
                        module -> module.withSources("com.github.sormuras.bach/test/java"))
                    .with(PatchMode.SOURCES)
                    .withPatchModule(
                        "com.github.sormuras.bach", "com.github.sormuras.bach/main/java")
                    .withModulePaths(".bach/workspace/modules", ".bach/external-modules"))
        .withRequiresExternalModules("org.junit.platform.console")
        .withExternalModuleLocators(JUnit.V_5_8_0_M1, build::locate)
        .with(options);
  }

  static Optional<ExternalModuleLocation> locate(String module) {
    var central =
        switch (module) {
          case "junit" -> Maven.central("junit", "junit", "4.13.2");
          case "org.hamcrest" -> Maven.central("org.hamcrest", "hamcrest", "2.2");
          default -> null;
        };
    return Optional.ofNullable(central).map(uri -> new ExternalModuleLocation(module, uri));
  }

  static Settings settings(Options options) {
    return Settings.of()
        .withBrowserConnectTimeout(9)
        .withWorkflowCheckpointHandler(build::smile)
        .withWorkflowTweakHandler(build::tweak)
        .with(options);
  }

  static void smile(Checkpoint checkpoint) {
    if (checkpoint instanceof BuildWorkflow.ErrorCheckpoint) {
      System.err.println(")-:");
      return;
    }
    if (checkpoint instanceof BuildWorkflow.SuccessCheckpoint point) {
      MavenConsumerPomGenerator.generate(point.workflow().bach());
    }
    System.out.printf("(-: %s%n", checkpoint.getClass());
  }

  static Call tweak(Tweak tweak) {
    if (tweak.call() instanceof CompileMainSpaceJavacCall javac) {
      return javac.with("-g").with("-parameters").with("-Werror").with("-Xlint");
    }
    if (tweak.call() instanceof TestCall test) {
      return test.with("1", "2", "3");
    }
    if (tweak.call() instanceof JUnitCall junit) {
      return junit.with("--fail-if-no-tests");
    }
    return tweak.call();
  }

  static class MyBach extends Bach {
    MyBach(Project project, Settings settings) {
      super(project, settings);
    }

    @Override
    public void build() {
      logbook.out().println("| BEGIN");
      super.build();
      logbook.out().println("| END.");
    }
  }
}

class MavenConsumerPomGenerator {

  static void generate(Bach bach) {
    var module = "com.github.sormuras.bach";
    var version = bach.project().version().value().toString();
    var file = module + "@" + version + ".jar";
    var jar = bach.folders().workspace("modules", file);

    bach.log(System.Logger.Level.INFO, "Generate Maven consumer POM file");
    try {
      // deploy/maven/
      var maven = Files.createDirectories(bach.folders().workspace("deploy", "maven"));
      var pom = generate(maven, module, version, jar);
      bach.log(System.Logger.Level.INFO, "  " + pom);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static Path generate(Path maven, String module, String version, Path jar) throws Exception {
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
            .add("-Dfile=" + jar)
            .add("-Dsources=" + emptyZip)
            .add("-Djavadoc=" + emptyZip);
    Files.writeString(maven.resolve(module + ".files"), files.toString());
    return pomFile;
  }
}
