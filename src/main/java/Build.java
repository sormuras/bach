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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    Bach.Builder builder = new Bach.Builder();
    this.bach = builder.build();
  }

  private void build() throws IOException {
    format(Paths.get("src"));
    resolve();
    compile();
    bach.call("javadoc", "-quiet", "-Xdoclint:none", "-d", JAVADOC, "src/main/java/Bach.java");
    jar();
    test();
  }

  private URI uri(String group, String artifact, String version) {
    return Bach.Util.jcenter(group, artifact, version);
  }

  private void resolve() {
    Map<String, URI> map = new TreeMap<>();
    map.put("org.junit.jupiter.api", uri("org.junit.jupiter", "junit-jupiter-api", "5.0.0-M4"));
    map.put(
        "org.junit.platform.commons",
        uri("org.junit.platform", "junit-platform-commons", "1.0.0-M4"));
    map.put("org.opentest4j", uri("org.opentest4j", "opentest4j", "1.0.0-M2"));
    map.forEach(bach::resolve);
  }

  private void compile() throws IOException {
    // main
    bach.call("javac", "-d", CLASSES, "src/main/java/Bach.java");
    // test
    List<Object> classPathEntries = new ArrayList<>();
    classPathEntries.add(CLASSES);
    Files.walk(bach.path(Bach.Folder.DEPENDENCIES))
        .filter(Bach.Util::isJarFile)
        .forEach(classPathEntries::add);
    bach.execute(
        new Bach.Command("javac")
            .addAll("-d", CLASSES)
            .add("--class-path")
            .add(classPathEntries.stream(), File.pathSeparator)
            .mark(1)
            .addAll(Paths.get("src", "test", "java"), Bach.Util::isJavaSourceFile));
  }

  private void jar() throws IOException {
    Files.createDirectories(ARTIFACTS);
    jar("bach", CLASSES, "Bach.class");
    jar("bach-sources", "src/main/java", "Bach.java");
    jar("bach-javadoc", JAVADOC, ".");
  }

  private void jar(String artifact, Object... contents) {
    Bach.Command jar = new Bach.Command("jar");
    Path file = ARTIFACTS.resolve(artifact + ".jar");
    jar.addAll("--create", "--file=" + file);
    jar.add("-C").addAll(contents);
    bach.execute(jar);
  }

  private void format(Path... paths) throws IOException {
    String mode = Boolean.getBoolean("bach.format.replace") ? "replace" : "validate";
    String repo = "https://jitpack.io";
    String user = "com/github/sormuras";
    String name = "google-java-format";
    String version = "validate-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    Path jar = Bach.Util.download(uri, bach.path(Bach.Folder.TOOLS).resolve(name));
    Bach.Command command = bach.java("-jar", jar, "--" + mode).mark(10);
    for (Path path : paths) {
      command.addAll(path, Bach.Util::isJavaSourceFile);
      bach.execute(command);
    }
  }

  private void test() {
    String artifact = "junit-platform-console-standalone";
    URI uri = Bach.Util.jcenter("org.junit.platform", artifact, "1.0.0-M4");
    Path jar = Bach.Util.download(uri, bach.path(Bach.Folder.TOOLS).resolve(artifact));
    Bach.Command command = bach.java("-ea", "-jar", jar);
    command.add("--scan-classpath").add(CLASSES);
    command.add("--class-path").add(CLASSES);
    bach.execute(command);
  }
}
