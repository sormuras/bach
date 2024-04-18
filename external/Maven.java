/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.external;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

/**
 * Apache Maven installer.
 *
 * @see <a href="https://maven.apache.org">https://maven.apache.org</a>
 */
public record Maven(String version) implements ToolInstaller {
  public static final String DEFAULT_VERSION = "3.9.6";

  public static void main(String... args) {
    var version = System.getProperty("version", DEFAULT_VERSION);
    new Maven(version).install().run(args.length == 0 ? new String[] {"--version"} : args);
  }

  public Maven() {
    this(DEFAULT_VERSION);
  }

  @Override
  public ToolProvider install(Path into) throws Exception {
    var base = "https://repo.maven.apache.org/maven2/org/apache/maven";
    var mavenWrapperProperties = into.resolve("maven-wrapper.properties");
    if (!Files.exists(mavenWrapperProperties))
      try {
        Files.writeString(
            mavenWrapperProperties,
            // language=properties
            """
            distributionUrl=%s/apache-maven/%s/apache-maven-%s-bin.zip
            """
                .formatted(base, version, version));
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    var uri = URI.create(base + "/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar#SIZE=62547");
    var mavenWrapperJar = into.resolve("maven-wrapper.jar");
    download(mavenWrapperJar, uri);
    return ToolProgram.findJavaDevelopmentKitTool(
            "java",
            "-D" + "maven.multiModuleProjectDirectory=.",
            "--class-path=" + mavenWrapperJar,
            "org.apache.maven.wrapper.MavenWrapperMain")
        .orElseThrow();
  }
}
