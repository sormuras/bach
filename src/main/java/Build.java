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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Build {

  private static final Path TARGET = Paths.get("target", "build");
  private static final Path CLASSES = TARGET.resolve("classes");
  private static final Path JAVADOC = TARGET.resolve("javadoc");
  private static final Path ARTIFACTS = TARGET.resolve("artifacts");

  public static void main(String... args) throws Exception {
    new Build().build();
  }

  private final Bach bach;

  private Build() {
    this.bach = new Bach();
  }

  private void build() throws IOException {
    format();
    resolve();
    Bach.Util.cleanTree(TARGET, true);
    compile();
    jar();
    test();
  }

  private void resolve() {
    bach.resolve("org.junit.jupiter", "junit-jupiter-api", "5.0.0-SNAPSHOT");
    bach.resolve("org.junit.platform", "junit-platform-commons", "1.0.0-SNAPSHOT");
    bach.resolve("org.opentest4j", "opentest4j", "1.0.0-SNAPSHOT");
  }

  private void compile() throws IOException {
    // main
    bach.call("javac", "-d", CLASSES, "src/main/java/Bach.java");
    // javadoc
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
    // test
    List<Path> classPathEntries = new ArrayList<>();
    classPathEntries.add(CLASSES);
    Files.walk(Paths.get(".bach/resolved"))
        .filter(Bach.Util::isJarFile)
        .forEach(classPathEntries::add);
    Bach.Command javac = bach.new Command("javac");
    javac.add("-d");
    javac.add(CLASSES);
    javac.add("--class-path");
    javac.add(classPathEntries);
    javac.mark(1);
    javac.addAll(Paths.get("src", "test", "java"), Bach.Util::isJavaFile);
    bach.execute(javac);
  }

  private void jar() throws IOException {
    Files.createDirectories(ARTIFACTS);
    jar("bach", CLASSES, "Bach.class");
    jar("bach-sources", "src/main/java", "Bach.java");
    jar("bach-javadoc", JAVADOC, ".");
  }

  private void jar(String artifact, Object... contents) {
    Bach.Command jar = bach.new Command("jar");
    Path file = ARTIFACTS.resolve(artifact + ".jar");
    jar.add("--create");
    jar.add("--file=" + file);
    jar.add("-C");
    Arrays.stream(contents).forEach(jar::add);
    bach.execute(jar);
  }

  private void format() throws IOException {
    String mode = Boolean.getBoolean("bach.format.replace") ? "replace" : "validate";
    String repo = "https://jitpack.io";
    String user = "com/github/sormuras";
    String name = "google-java-format";
    String version = "validate-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    Path jar = Bach.Util.download(uri, Paths.get(".bach/tools").resolve(name));
    Bach.Command format = bach.new Command("java");
    format.add("-jar");
    format.add(jar);
    format.add("--" + mode);
    format.mark(10);
    Path root = Paths.get("src");
    format.addAll(root, unit -> Bach.Util.isJavaFile(unit) && !unit.endsWith("module-info.java"));
    bach.execute(format);
  }

  private void test() throws IOException {
    String repo = "https://oss.sonatype.org/content/repositories/snapshots";
    String user = "org/junit/platform";
    String name = "junit-platform-console-standalone";
    String version = "1.0.0-SNAPSHOT";
    String file = name + "-1.0.0-20170624.111938-249.jar";
    URI uri = URI.create(String.join("/", repo, user, name, version, file));
    Path jar = Bach.Util.download(uri, Paths.get(".bach/tools").resolve(name));
    bach.call("java", "-ea", "-jar", jar, "--scan-classpath", CLASSES, "--class-path", CLASSES);
  }
}
