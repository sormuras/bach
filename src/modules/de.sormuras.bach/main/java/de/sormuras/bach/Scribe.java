/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

/*BODY*/
/** Create API documentation. */
public /*STATIC*/ class Scribe {

  private final Bach bach;
  private final Project project;
  private final Project.Realm realm;
  private final Project.Target target;
  private final Path javadocJar;

  public Scribe(Bach bach, Project project, Project.Realm realm) {
    this.bach = bach;
    this.project = project;
    this.realm = realm;
    this.target = project.target(realm);

    var nameDashVersion = project.name + '-' + project.version;
    this.javadocJar = target.directory.resolve(nameDashVersion + "-javadoc.jar");
  }

  public void document() {
    document(realm.names());
  }

  public void document(Iterable<String> modules) {
    bach.log("Compiling %s realm's documentation: %s", realm.name, modules);
    var destination = target.directory.resolve("javadoc");
    var javadoc =
        new Command("javadoc")
            .add("-d", destination)
            .add("-encoding", "UTF-8")
            .addIff(!bach.verbose(), "-quiet")
            .add("-Xdoclint:-missing")
            .add("--module-path", project.library.modulePaths)
            .add("--module-source-path", realm.moduleSourcePath);

    for (var unit : realm.units(Project.ModuleUnit::isMultiRelease)) {
      var base = unit.sources.get(0);
      if (!unit.info.path.startsWith(base.path)) {
        javadoc.add("--patch-module", unit.name() + "=" + base.path);
      }
    }

    javadoc.add("--module", String.join(",", modules));
    bach.run(javadoc);

    bach.run(
        new Command("jar")
            .add("--create")
            .add("--file", javadocJar)
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", destination)
            .add("."));
  }

  public void generateMavenInstallScript() {
    var maven =
        String.join(
            " ",
            "mvn",
            "--batch-mode",
            "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn",
            "install:install-file");
    var lines = new ArrayList<String>();
    for (var unit : realm.units) {
      if (unit.mavenPom().isPresent()) {
        lines.add(String.join(" ", maven, generateMavenArtifactLine(unit)));
      }
    }
    if (lines.isEmpty()) {
      bach.log("No maven-install script lines generated.");
      return;
    }
    try {
      Files.write(bach.project.targetDirectory.resolve("maven-install.sh"), lines);
      Files.write(
          bach.project.targetDirectory.resolve("maven-install.bat"),
          lines.stream().map(l -> "call " + l).collect(Collectors.toList()));
    } catch (IOException e) {
      throw new UncheckedIOException("Generating install script failed: " + e.getMessage(), e);
    }
  }

  public void generateMavenDeployScript() {
    var deployment = realm.toolArguments.deployment().orElseThrow();
    var plugin = "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file";
    var repository = "repositoryId=" + deployment.mavenRepositoryId;
    var url = "url=" + deployment.mavenUri;
    var maven =
        String.join(
            " ",
            "mvn",
            "--batch-mode",
            "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn",
            plugin,
            "-D" + repository,
            "-D" + url);
    var lines = new ArrayList<String>();
    for (var unit : realm.units) {
      lines.add(String.join(" ", maven, generateMavenArtifactLine(unit)));
    }
    try {
      Files.write(bach.project.targetDirectory.resolve("maven-deploy.sh"), lines);
      Files.write(
          bach.project.targetDirectory.resolve("maven-deploy.bat"),
          lines.stream().map(l -> "call " + l).collect(Collectors.toList()));
    } catch (IOException e) {
      throw new UncheckedIOException("Deploy failed: " + e.getMessage(), e);
    }
  }

  private String generateMavenArtifactLine(Project.ModuleUnit unit) {
    var pom = "pomFile=" + Util.require(unit.mavenPom().orElseThrow(), Files::isRegularFile);
    var file = "file=" + Util.require(target.modularJar(unit), Util::isJarFile);
    var sources = "sources=" + Util.require(target.sourcesJar(unit), Util::isJarFile);
    var javadoc = "javadoc=" + Util.require(javadocJar, Util::isJarFile);
    return String.join(" ", "-D" + pom, "-D" + file, "-D" + sources, "-D" + javadoc);
  }
}
