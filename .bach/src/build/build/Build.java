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
import de.sormuras.bach.Project;
import de.sormuras.bach.project.Feature;
import de.sormuras.bach.project.Link;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var project =
        Project.of()
            /*
             * Configure basic component values.
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
            // .tweakJavacCall(javac -> javac.with("-verbose"))
            // .tweakJarCall(jar -> jar.with(0, "--verbose"))
            .tweakJlinkCall(jlink -> jlink.with("--verbose"))
            .tweakJavadocCall(
                javadoc ->
                    javadoc
                        .with("-windowtitle", "\uD83C\uDFBC Bach.java " + Bach.VERSION)
                        .with("-header", "\uD83C\uDFBC Bach.java " + Bach.VERSION)
                        .with("-footer", "\uD83C\uDFBC Bach.java " + Bach.VERSION)
                        .with("-use")
                        .with("-linksource")
                        .with("-link", "https://docs.oracle.com/en/java/javase/11/docs/api")
                        .without("-Xdoclint")
                        .with("-Xdoclint:-missing")
                        .with("-Xwerror") // https://bugs.openjdk.java.net/browse/JDK-8237391
                )
            /*
             * Configure test code space.
             */
            .withTestModule("src/de.sormuras.bach/test/java-module/module-info.java")
            .withTestModule("src/test.base/test/java/module-info.java")
            .withTestModule("src/test.modules/test/java/module-info.java")
            // .tweakJUnitCall(junit -> junit.with("--fail-if-no-tests"))
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

    Bach.of(project).build();
  }
}
