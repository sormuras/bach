/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

class Build {

  private static final Path TARGET = Paths.get("target", "build");
  private static final Path CLASSES = TARGET.resolve("classes");
  private static final Path JAVADOC = TARGET.resolve("javadoc");
  private static final Path ARTIFACTS = TARGET.resolve("artifacts");

  public static void main(String... args) throws Exception {
    new Build().build();
  }

  private final Bach bach = new Bach();

  private void build() throws IOException {
    // TODO bach.cleanTree(TARGET, true);
    format();
    compile();
    javadoc();
    jar();
    test();
  }

  private void compile() throws IOException {
    // main
    bach.call("javac", "-d", CLASSES, "src/main/java/Bach.java");
    // test
    bach.javac(
        options -> {
          options.destinationPath = CLASSES;
          options.classPaths =
              List.of(
                  CLASSES,
                  bach.resolve("org.junit.jupiter", "junit-jupiter-api", "5.0.0-M5"),
                  bach.resolve("org.junit.platform", "junit-platform-commons", "1.0.0-M5"),
                  bach.resolve("org.opentest4j", "opentest4j", "1.0.0-SNAPSHOT"));
          options.classSourcePaths = List.of(Paths.get("src/test/java"));
          return options;
        });
  }

  private void javadoc() throws IOException {
    Files.createDirectories(JAVADOC);
    bach.call(
        "javadoc",
        "-quiet",
        "-Xdoclint:all,-missing",
        "-package",
        "-linksource",
        "-link",
        "http://download.java.net/java/jdk9/docs/api",
        "-d",
        JAVADOC,
        "src/main/java/Bach.java");
  }

  private void jar() throws IOException {
    Files.createDirectories(ARTIFACTS);
    Path main = jar("bach", CLASSES, "Bach.class");
    jar("bach-sources", "src/main/java", "Bach.java");
    jar("bach-javadoc", JAVADOC, ".");

    bach.jdeps(UnaryOperator.identity(), main);
  }

  private Path jar(String artifact, Object... contents) {
    Bach.Command jar = bach.new Command("jar");
    Path file = ARTIFACTS.resolve(artifact + ".jar");
    jar.add("--create");
    jar.add("--file=" + file);
    jar.add("-C");
    Arrays.stream(contents).forEach(jar::add);
    bach.call(jar);
    return file;
  }

  private void format() throws IOException {
    String mode = Boolean.getBoolean("bach.format.replace") ? "replace" : "validate";
    String repo = "https://jitpack.io";
    String user = "com/github/sormuras";
    String name = "google-java-format";
    String version = "validate-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    Path jar = bach.download(uri, Paths.get(".bach/tools").resolve(name));
    Bach.Command format = bach.new Command("java");
    format.add("-jar");
    format.add(jar);
    format.add("--" + mode);
    format.mark(10);
    Path root = Paths.get("src");
    format.addAll(root, unit -> bach.isJavaFile(unit) && !unit.endsWith("module-info.java"));
    bach.call(format);
  }

  private void test() throws IOException {
    String repo = "http://repo1.maven.org/maven2";
    String user = "org/junit/platform";
    String name = "junit-platform-console-standalone";
    String version = "1.0.0-M5";
    String file = name + "-" + version + ".jar";
    URI uri = URI.create(String.join("/", repo, user, name, version, file));
    Path path = Paths.get(".bach/tools").resolve(name);
    Path jar = bach.download(uri, path, file, p -> true);
    bach.call("java", "-ea", "-jar", jar, "--class-path", CLASSES, "--scan-classpath");
  }
}
