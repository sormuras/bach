/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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
    try {
      new Build().build();
    } catch (Throwable throwable) {
      System.err.println("build failed due to: " + throwable);
      throwable.printStackTrace();
      System.exit(1);
    }
  }

  Path TOOLS = Paths.get(".bach", "tools");
  Path MAVEN = Paths.get(".bach", "resolved");
  Path SOURCE_BACH = Paths.get("src", "bach");
  Path SOURCE_TEST = Paths.get("src", "test");
  Path TARGET = Paths.get("target", "build");
  Path TARGET_MAIN = TARGET.resolve("classes/main");
  Path TARGET_TEST = TARGET.resolve("classes/test");
  Path JAVADOC = TARGET.resolve("javadoc");
  Path ARTIFACTS = TARGET.resolve("artifacts");
  Path BACH_JAVA = SOURCE_BACH.resolve("Bach.java");

  String JUNIT_JUPITER = "5.1.0";
  String JUNIT_PLATFORM = "1.1.0";
  String OPENTEST4J = "1.0.0";
  String API_GUARDIAN = "1.0.0";

  final Bach bach = new Bach();

  void build() throws Exception {
    format();
    clean();
    compile();
    test();
    javadoc();
    jar();
    jdeps();
  }

  Path maven(String group, String artifact, String version) throws Exception {
    return maven(group, artifact, version, "");
  }

  Path maven(String group, String artifact, String version, String classifier) throws Exception {
    if (!classifier.isEmpty() && !classifier.startsWith("-")) {
      classifier = "-" + classifier;
    }
    var repo = "http://central.maven.org/maven2";
    var file = artifact + "-" + version + classifier + ".jar";
    var uri = URI.create(String.join("/", repo, group.replace('.', '/'), artifact, version, file));
    return download(uri, MAVEN, file);
  }

  /** Download the resource from URI to the target directory using the provided file name. */
  Path download(URI uri, Path directory, String fileName) throws Exception {
    return bach.util.download(uri, directory, fileName);
  }

  void format() throws Exception {
    System.out.printf("%n[format]%n%n");
    /*
    String repo = "https://jitpack.io";
    String user = "com/github/google";
    String name = "google-java-format";
    String version = "master-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    */
    var version = "1.5";
    var base = "https://github.com/google/";
    var name = "google-java-format";
    var file = name + "-" + version + "-all-deps.jar";
    var uri = URI.create(base + name + "/releases/download/" + name + "-" + version + "/" + file);
    var jar = download(uri, TOOLS.resolve(name), file);
    var java = new JdkTool.Java();
    java.jar = jar;
    java.toCommand(bach).add("--version").run();
    // format
    var format = java.toCommand(bach);
    if (Boolean.getBoolean("bach.format.replace")) {
      format.add("--replace");
    } else {
      format.add("--dry-run");
      format.add("--set-exit-if-changed");
    }
    format.mark(5);
    format.addAllJavaFiles(List.of(Paths.get("src"), Paths.get("demo")));
    format.run();
  }

  void clean() {
    System.out.printf("%n[clean]%n%n");
    bach.util.removeTree(TARGET);
    System.out.println("deleted " + TARGET);
  }

  void compile() throws Exception {
    System.out.printf("%n[compile]%n%n");

    // main
    var javac = new JdkTool.Javac();
    javac.generateAllDebuggingInformation = true;
    javac.destination = TARGET_MAIN;
    javac.classSourcePath = List.of(SOURCE_BACH);
    bach.run(javac);

    // test
    javac.destination = TARGET_TEST;
    javac.classSourcePath = List.of(SOURCE_TEST);
    javac.classPath =
        List.of(
            TARGET_MAIN,
            maven("org.junit.jupiter", "junit-jupiter-api", JUNIT_JUPITER),
            maven("org.junit.platform", "junit-platform-commons", JUNIT_PLATFORM),
            maven("org.apiguardian", "apiguardian-api", API_GUARDIAN),
            maven("org.opentest4j", "opentest4j", OPENTEST4J));
    bach.run(javac);
    bach.util.copyTree(Paths.get("src/test-resources"), TARGET_TEST);
  }

  void javadoc() throws Exception {
    System.out.printf("%n[javadoc]%n%n");

    Files.createDirectories(JAVADOC);
    var javadoc = new JdkTool.Javadoc();
    javadoc.destination = JAVADOC;
    javadoc.doclint = "all,-missing";
    javadoc.link = List.of("https://docs.oracle.com/javase/9/docs/api");
    javadoc.linksource = true;
    javadoc.showTypes = JdkTool.Javadoc.Visibility.PACKAGE;
    javadoc.showMembers = JdkTool.Javadoc.Visibility.PACKAGE;
    javadoc.toCommand(bach).add(BACH_JAVA).run();
  }

  void jar() throws Exception {
    System.out.printf("%n[jar]%n%n");

    Files.createDirectories(ARTIFACTS);
    jar("bach.jar", TARGET_MAIN);
    jar("bach-sources.jar", SOURCE_BACH);
    jar("bach-javadoc.jar", JAVADOC);
  }

  void jar(String artifact, Path path) {
    var jar = new JdkTool.Jar();
    jar.file = ARTIFACTS.resolve(artifact);
    jar.path = path;
    bach.run(jar);
  }

  void jdeps() {
    System.out.printf("%n[jdeps]%n%n");

    var jdeps = new JdkTool.Jdeps();
    jdeps.summary = true;
    jdeps.recursive = true;
    jdeps.toCommand(bach).add(ARTIFACTS.resolve("bach.jar")).run();
  }

  void test() throws Exception {
    System.out.printf("%n[test]%n%n");

    var name = "junit-platform-console-standalone";
    var repo = "http://central.maven.org/maven2";
    var user = "org/junit/platform";
    var file = name + "-" + JUNIT_PLATFORM + ".jar";
    var uri = URI.create(String.join("/", repo, user, name, JUNIT_PLATFORM, file));
    var jar = download(uri, TOOLS.resolve(name), file);

    var java = bach.command("java");
    java.add("-ea");
    java.add("-Dbach.offline=" + System.getProperty("bach.offline", "false"));
    java.add("-jar").add(jar);
    java.add("--class-path").add(TARGET_TEST);
    java.add("--class-path").add(TARGET_MAIN);
    java.add("--scan-classpath");
    java.run();
  }
}
