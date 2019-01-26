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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("WeakerAccess")
class Build {

  public static void main(String... args) {
    System.exit(new Build().build());
  }

  Path MAVEN = Paths.get(".bach", "resolved");
  Path TOOLS = Paths.get(".bach", "tools");
  Path SOURCE_BACH = Paths.get("src", "bach");
  Path SOURCE_TEST = Paths.get("src", "test");
  Path TARGET = Paths.get("target", "build");
  Path TARGET_MAIN = TARGET.resolve("classes/main");
  Path TARGET_TEST = TARGET.resolve("classes/test");
  Path JAVADOC = TARGET.resolve("javadoc");
  Path ARTIFACTS = TARGET.resolve("artifacts");
  Path BACH_JAVA = SOURCE_BACH.resolve("Bach.java");

  String JUNIT_JUPITER = "5.4.0-RC1";
  String JUNIT_PLATFORM = "1.4.0-RC1";
  String OPENTEST4J = "1.1.0";
  String API_GUARDIAN = "1.0.0";

  final Bach bach = new Bach();

  int build() {
    try {
      format();
      clean();
      compile();
      test();
      javadoc();
      jar();
      jdeps();
      return 0;
    } catch (Throwable throwable) {
      System.err.println("build failed due to: " + throwable);
      throwable.printStackTrace();
      return 1;
    }
  }

  URI mavenUri(String group, String artifact, String version) {
    return mavenUri(group, artifact, version, "");
  }

  URI mavenUri(String group, String artifact, String version, String classifier) {
    if (!classifier.isEmpty() && !classifier.startsWith("-")) {
      classifier = "-" + classifier;
    }
    var repo = "https://maven-central.storage.googleapis.com/repos/central/data";
    var file = artifact + "-" + version + classifier + ".jar";
    return URI.create(String.join("/", repo, group.replace('.', '/'), artifact, version, file));
  }

  Path maven(String group, String artifact, String version) {
    return bach.util.download(mavenUri(group, artifact, version), MAVEN);
  }

  void format() {
    System.out.printf("%n[format]%n%n");
    bach.format(Boolean.getBoolean("bach.format.replace"), "src", "demo");

    System.out.println("formatted 'src/**/*.java' and 'demo/**/*.java' sources");
  }

  void clean() {
    System.out.printf("%n[clean]%n%n");
    if (Files.exists(TARGET)) {
      bach.util.removeTree(TARGET);
    }
    System.out.println("deleted " + TARGET);
  }

  void compile() {
    System.out.printf("%n[compile]%n%n");

    // main
    var javac = bach.command("javac");
    javac.setExecutableSupportsArgumentFile(true);
    javac.add("-g");
    javac.add("-d").add(TARGET_MAIN);
    javac.add("--source-path").add(SOURCE_BACH);
    javac.mark(10);
    javac.addAll(SOURCE_BACH, bach.util::isJavaFile);
    javac.run();

    // test
    var test = bach.command("javac");
    test.setExecutableSupportsArgumentFile(true);
    test.add("-g");
    test.add("-d").add(TARGET_TEST);
    test.add("--source-path").add(SOURCE_TEST);
    test.add("--class-path")
        .add(
            List.of(
                TARGET_MAIN,
                maven("org.junit.jupiter", "junit-jupiter-api", JUNIT_JUPITER),
                maven("org.junit.jupiter", "junit-jupiter-params", JUNIT_JUPITER),
                maven("org.junit.platform", "junit-platform-commons", JUNIT_PLATFORM),
                maven("org.apiguardian", "apiguardian-api", API_GUARDIAN),
                maven("org.opentest4j", "opentest4j", OPENTEST4J)));
    test.mark(10);
    test.addAll(SOURCE_TEST, bach.util::isJavaFile);
    test.run();
    //    bach.util.copyTree(Paths.get("src/test-resources"), TARGET_TEST);
  }

  void javadoc() throws Exception {
    System.out.printf("%n[javadoc]%n%n");

    Files.createDirectories(JAVADOC);
    var javadoc = bach.command("javadoc");
    javadoc.add("-d").add(JAVADOC);
    javadoc.add("-package");
    javadoc.add("-quiet");
    javadoc.add("-keywords");
    javadoc.add("-html5");
    javadoc.add("-linksource");
    javadoc.add("-Xdoclint:all,-missing");
    javadoc.add("-link").add("https://docs.oracle.com/en/java/javase/11/docs/api/");
    javadoc.add(BACH_JAVA);
    javadoc.run();
  }

  void jar() throws Exception {
    System.out.printf("%n[jar]%n%n");

    Files.createDirectories(ARTIFACTS);
    jar("bach.jar", TARGET_MAIN);
    jar("bach-sources.jar", SOURCE_BACH);
    jar("bach-javadoc.jar", JAVADOC);
  }

  void jar(String artifact, Path path) {
    var jar = bach.command("jar");
    jar.add("--create");
    jar.add("--file").add(ARTIFACTS.resolve(artifact));
    jar.add("-C").add(path).add(".");
    jar.run();
  }

  void jdeps() {
    System.out.printf("%n[jdeps]%n%n");

    var jdeps = bach.command("jdeps");
    jdeps.add("-summary");
    jdeps.add("-recursive");
    jdeps.add(ARTIFACTS.resolve("bach.jar"));
    jdeps.run();
  }

  void test() throws Exception {
    System.out.printf("%n[test]%n%n");

    var uri = mavenUri("org.junit.platform", "junit-platform-console-standalone", JUNIT_PLATFORM);
    var jar = bach.util.download(uri, TOOLS);

    var java = bach.command("java");
    java.add("-ea");
    java.add("-Dbach.offline=" + System.getProperty("bach.offline", "false"));
    java.add("-Djunit.jupiter.execution.parallel.enabled=false");
    java.add("-jar").add(jar);
    java.add("--class-path").add(TARGET_TEST);
    java.add("--class-path").add(TARGET_MAIN);
    java.add("--scan-classpath");
    java.run();
  }
}
