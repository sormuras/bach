package bach.info;

import com.github.sormuras.bach.api.BuildMainSpaceAPI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

public interface MainSpaceBuilder extends BuildMainSpaceAPI {

  @Override
  default void buildMainSpace() throws Exception {
    BuildMainSpaceAPI.super.buildMainSpace();

    var module = "com.github.sormuras.bach";
    var version = bach().project().versionNumberAndPreRelease();
    var modules = bach().folders().workspace("modules");
    var file = modules.resolve(bach().buildMainJarFileName(module));

    say("Generate and check Maven consumer POM file");
    var pom = generateMavenConsumerPom(module, version, file);
    var pomcheck = PomChecker.install(bach()).checkMavenCentral(pom);
    bach().run(pomcheck).requireSuccessful();
  }

  private Path generateMavenConsumerPom(String module, String version, Path file) throws Exception {
    var maven = Files.createDirectories(bach().folders().workspace("deploy", "maven"));

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
