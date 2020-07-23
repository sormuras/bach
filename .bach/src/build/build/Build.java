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
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSpace;

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
            .withMainSpaceUnit("src/de.sormuras.bach/main/java/module-info.java")
            .withMainSpaceCompiledForJavaRelease(11)
            .with(MainSpace.Modifier.INCLUDE_SOURCES_IN_MODULAR_JAR)
            .with(MainSpace.Modifier.API_DOCUMENTATION)
            .without(MainSpace.Modifier.CUSTOM_RUNTIME_IMAGE)
            // .withMainSpaceJavacTweak(javac -> javac.with("-verbose"))
            // .withMainSpaceJarTweak(jar -> jar.with(0, "--verbose"))
            // .withMainSpaceJlinkTweak(jlink -> jlink.with("--verbose"))
            .withMainSpaceJavadocTweak(
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
            .withTestSpaceUnit("src/de.sormuras.bach/test/java-module/module-info.java")
            .withTestSpaceUnit("src/test.base/test/java/module-info.java")
            .withTestSpaceUnit("src/test.modules/test/java/module-info.java")
            // .withTestSpaceJUnitTweak(junit -> junit.with("--fail-if-no-tests"))
            /*
             * Configure test-preview code space.
             */
            .withTestSpacePreviewUnit("src/test.preview/test-preview/java/module-info.java")
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
