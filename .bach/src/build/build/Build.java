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

package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Project;
import de.sormuras.bach.action.GenerateMavenPomFiles;
import de.sormuras.bach.project.CodeUnit;
import de.sormuras.bach.project.Feature;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.tool.DefaultTweak;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Javadoc;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var project =
        Project.of()
            /*
             * Configure basic information.
             */
            .name("bach")
            .version(Bach.VERSION)
            /*
             * Configure main code space.
             */
            .module("src/de.sormuras.bach/main/java/module-info.java")
            .targetJavaRelease(11)
            .with(Feature.CREATE_API_DOCUMENTATION)
            .with(Feature.INCLUDE_SOURCES_IN_MODULAR_JAR)
            .without(Feature.CREATE_CUSTOM_RUNTIME_IMAGE)
            /*
             * Configure test code space.
             */
            .withTestModule("src/de.sormuras.bach/test/java-module/module-info.java")
            .withTestModule("src/test.base/test/java/module-info.java")
            .withTestModule("src/test.modules/test/java/module-info.java")
            /*
             * Configure test-preview code space.
             */
            .withTestPreviewModule("src/test.preview/test-preview/java/module-info.java")
            /*
             * Configure external library resolution.
             */
            .with(
                Link.ofJUnitPlatform("commons", "1.7.0-M1"),
                Link.ofJUnitPlatform("console", "1.7.0-M1"),
                Link.ofJUnitPlatform("engine", "1.7.0-M1"),
                Link.ofJUnitPlatform("launcher", "1.7.0-M1"),
                Link.ofJUnitPlatform("reporting", "1.7.0-M1"),
                Link.ofJUnitPlatform("testkit", "1.7.0-M1"))
            .with(
                Link.ofJUnitJupiter("", "5.7.0-M1"),
                Link.ofJUnitJupiter("api", "5.7.0-M1"),
                Link.ofJUnitJupiter("engine", "5.7.0-M1"),
                Link.ofJUnitJupiter("params", "5.7.0-M1"))
            .with(
                Link.ofCentral("org.apiguardian.api", "org.apiguardian:apiguardian-api:1.1.0"),
                Link.ofCentral("org.opentest4j", "org.opentest4j:opentest4j:1.2.0"))
            .withLibraryRequires("org.junit.platform.console");

    var configuration = Configuration.ofSystem().tweak(new Tweak());
    new Bach(configuration, project).build(Build::sequence);
  }

  static void sequence(Bach bach) {
    bach.deleteClassesDirectories();
    bach.executeDefaultBuildActions();
    new GeneratePoms(bach).execute();
  }

  static class Tweak extends DefaultTweak {
    @Override
    public JUnit tweakJUnit(JUnit junit) {
      return junit.with("--fail-if-no-tests");
    }

    @Override
    public Javadoc tweakJavadoc(Javadoc javadoc) {
      return javadoc
          .with("-windowtitle", "\uD83C\uDFBC Bach.java " + Bach.VERSION)
          .with("-header", "\uD83C\uDFBC Bach.java " + Bach.VERSION)
          .with("-footer", "\uD83C\uDFBC Bach.java " + Bach.VERSION)
          .with("-use")
          .with("-linksource")
          .with("-link", "https://docs.oracle.com/en/java/javase/11/docs/api")
          .without("-Xdoclint")
          .with("-Xdoclint:-missing")
          .with("-Xwerror"); // https://bugs.openjdk.java.net/browse/JDK-8237391
    }
  }

  static class GeneratePoms extends GenerateMavenPomFiles {

    GeneratePoms(Bach bach) {
      super(bach, "    ");
    }

    @Override
    public String computeMavenGroupId(CodeUnit unit) {
      return "de.sormuras.bach";
    }

    @Override
    public void generateCoordinates(Pom pom, CodeUnit unit) {
      super.generateCoordinates(pom, unit);

      if (unit.name().equals("de.sormuras.bach")) {
        var title = "\uD83C\uDFBC Java Shell Builder - Build modular Java projects with JDK tools";
        pom.append("\n");
        pom.addNewLine().add("name", "Bach.java");
        pom.addNewLine().add("description", title);
      }
    }

    @Override
    public void generateFooter(GenerateMavenPomFiles.Pom pom) {
      pom.append("\n");
      pom.addNewLine().add("url", "https://github.com/sormuras/bach");
      pom.addNewLine().depth(+1).append("<scm>");
      pom.addNewLine().add("url", "https://github.com/sormuras/bach.git");
      pom.depth(-1).addNewLine().append("</scm>");

      pom.append("\n");
      pom.addNewLine().depth(+1).append("<developers>");
      pom.addNewLine().depth(+1).append("<developer>");
      pom.addNewLine().add("name", "Christian Stein");
      pom.addNewLine().add("id", "sormuras");
      pom.depth(-1).addNewLine().append("</developer>");
      pom.depth(-1).addNewLine().append("</developers>");

      pom.append("\n");
      pom.addNewLine().depth(+1).append("<licenses>");
      pom.addNewLine().depth(+1).append("<license>");
      pom.addNewLine().add("name", "Apache License, Version 2.0");
      pom.addNewLine().add("url", "https://www.apache.org/licenses/LICENSE-2.0.txt");
      pom.addNewLine().add("distribution", "repo");
      pom.depth(-1).addNewLine().append("</license>");
      pom.depth(-1).addNewLine().append("</licenses>");

      pom.append("\n");
      super.generateFooter(pom);
    }
  }
}
