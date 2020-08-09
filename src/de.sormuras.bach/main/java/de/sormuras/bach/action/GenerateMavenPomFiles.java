/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

package de.sormuras.bach.action;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.project.CodeUnit;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.file.Files;
import java.util.TreeSet;

/** An action that generates a {@code pom.xml} file for each main code unit. */
public abstract class GenerateMavenPomFiles implements Action {

  private final Bach bach;
  private final String indent;

  public GenerateMavenPomFiles(Bach bach, String indent) {
    this.bach = bach;
    this.indent = indent;
  }

  @Override
  public Bach bach() {
    return bach;
  }

  public abstract String computeMavenGroupId(CodeUnit unit);

  public String computeMavenArtifactId(CodeUnit unit) {
    return unit.name();
  }

  @Override
  public void execute() {
    for (var unit : main().units().map().values()) {
      var pom = new Pom(indent);
      generateHeader(pom);
      generateCoordinates(pom, unit);
      generateDependencies(pom, unit);
      generateFooter(pom);

      try {
        var module = unit.name();
        var nameAndVersion = module + '@' + project().version();

        var maven = base().workspace("deploy", "maven");
        Files.createDirectories(maven);

        // deploy/maven/${MODULE}@${VERSION}.pom.xml
        var pomFile = maven.resolve(nameAndVersion + ".pom.xml");
        Files.writeString(pomFile, pom.text.toString());

        // deploy/maven/${MODULE}@${VERSION}.files
        var files =
            Call.tool("mvn")
                .with(String.format("-DpomFile=\"%s\"", pomFile))
                .with(String.format("-Dfile=\"%s\"", project().toMainModuleArchive(module)))
                .with(String.format("-Dsources=\"%s\"", project().toMainSourceArchive(module)))
                .with(String.format("-Djavadoc=\"%s\"", project().toMainApiDocumentationArchive()));
        var cliFile = maven.resolve(nameAndVersion + ".files");
        Files.writeString(cliFile, String.join(" ", files.toStrings()));
      } catch (Exception e) {
        throw new RuntimeException("Write Maven-related file failed: " + e, e);
      }
    }
  }

  public void generateHeader(Pom pom) {
    pom.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    pom.addNewLine().append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"");
    pom.addNewLine().append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    pom.addNewLine()
        .append("         ")
        .append("xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0")
        .append(" ")
        .append("http://maven.apache.org/xsd/maven-4.0.0.xsd\">");
    pom.depth(+1).addNewLine().add("modelVersion", "4.0.0");
  }

  public void generateCoordinates(Pom pom, CodeUnit unit) {
    pom.append("\n");
    pom.addNewLine().add("groupId", computeMavenGroupId(unit));
    pom.addNewLine().add("artifactId", computeMavenArtifactId(unit));
    pom.addNewLine().add("version", project().version().toString());
  }

  public void generateDependencies(Pom pom, CodeUnit unit) {
    if (unit.descriptor().requires().isEmpty()) return;
    var declared = project().toDeclaredModuleNames();
    var external = project().toExternalModuleNames();
    pom.append("\n");
    pom.addNewLine().depth(+1).append("<dependencies>");
    for (var requires : new TreeSet<>(unit.descriptor().requires())) {
      var name = requires.name();
      if (declared.contains(name)) {
        var required = main().units().findUnit(name).orElseThrow();
        generateDependencyForDeclaredModule(pom, required);
        continue;
      }
      if (external.contains(name)) {
        generateDependencyForExternalModule(pom, requires);
        continue;
      }
      pom.addNewLine().append("<!-- system module: " + name + " -->");
    }
    pom.depth(-1).addNewLine().append("</dependencies>");
  }

  public void generateDependencyForExternalModule(Pom pom, Requires requires) {
    pom.addNewLine().append("<!-- " + requires + " -->");
  }

  public void generateDependencyForDeclaredModule(Pom pom, CodeUnit required) {
    pom.addNewLine().depth(+1).append("<dependency>");
    pom.addNewLine().add("groupId", computeMavenGroupId(required));
    pom.addNewLine().add("artifactId", computeMavenArtifactId(required));
    pom.addNewLine().add("version", project().version());
    pom.depth(-1).addNewLine().append("</dependency>");
  }

  public void generateFooter(Pom pom) {
    pom.depth(-1).addNewLine().append("</project>").addNewLine();
  }

  /** An extensible and mutable code builder. */
  public static final class Pom {

    private final StringBuilder text = new StringBuilder();
    private int depth = 0;
    private final String indent;

    public Pom(String indent) {
      this.indent = indent;
    }

    public Pom append(CharSequence sequence) {
      text.append(sequence);
      return this;
    }

    public Pom addNewLine() {
      return append("\n").append(indent.repeat(depth));
    }

    public Pom depth(int delta) {
      this.depth += delta;
      return this;
    }

    public Pom add(String name, Object value) {
      return append("<" + name + ">").append(String.valueOf(value)).append("</" + name + ">");
    }
  }
}
