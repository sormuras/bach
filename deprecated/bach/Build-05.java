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
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

interface Build {

  Path TOOLS = Paths.get(".bach", "tools");
  Path SOURCE_MAIN = Paths.get("src", "main", "java");
  Path SOURCE_TEST = Paths.get("src", "test", "java");
  Path TARGET = Paths.get("target", "build");
  Path TARGET_MAIN = TARGET.resolve("classes/main");
  Path TARGET_TEST = TARGET.resolve("classes/test");
  Path JAVADOC = TARGET.resolve("javadoc");
  Path ARTIFACTS = TARGET.resolve("artifacts");

  String JUNIT_JUPITER_VERSION = "5.0.0-RC3";
  String JUNIT_PLATFORM_VERSION = "1.0.0-RC3";
  String OPENTEST4J_VERSION = "1.0.0-RC1";

  static void main(String... args) {
    System.setProperty("bach.verbose", "true");
    try {
      format();
      Basics.resolve("org.junit.jupiter", "junit-jupiter-engine", JUNIT_JUPITER_VERSION);
      clean();
      generate();
      compile();
      test();
      javadoc();
      jar();
      jdeps();
    } catch (Throwable throwable) {
      System.err.println("build failed due to: " + throwable);
      throwable.printStackTrace();
      System.exit(1);
    }
  }

  static void format() throws IOException {
    System.out.printf("%n[format]%n%n");

    String mode = Boolean.getBoolean("bach.format.replace") ? "replace" : "validate";
    String repo = "https://jitpack.io";
    String user = "com/github/sormuras";
    String name = "google-java-format";
    String version = "validate-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    Path jar = Basics.download(uri, TOOLS.resolve(name));
    JdkTool.Command format = new JdkTool.Java().toCommand();
    format.add("-jar");
    format.add(jar);
    format.add("--" + mode);
    format.mark(5);
    List<Path> roots = List.of(Paths.get("src"), Paths.get("demo"));
    format.addAll(roots, Basics::isJavaFile);
    format.run();
  }

  static void clean() throws IOException {
    System.out.printf("%n[clean]%n%n");

    Basics.treeDelete(TARGET);
    System.out.println("deleted " + TARGET);
  }

  static void generate() throws IOException {
    System.out.printf("%n[generate]%n%n");

    Set<String> imports = new TreeSet<>();
    List<String> generated = new ArrayList<>();
    generated.add("/* THIS FILE IS GENERATED -- " + Instant.now() + " */");
    generated.add("/*");
    generated.add(" * Bach - Java Shell Builder");
    generated.add(" * Copyright (C) 2017 Christian Stein");
    generated.add(" *");
    generated.add(" * Licensed under the Apache License, Version 2.0 (the \"License\");");
    generated.add(" * you may not use this file except in compliance with the License.");
    generated.add(" * You may obtain a copy of the License at");
    generated.add(" *");
    generated.add(" *     https://www.apache.org/licenses/LICENSE-2.0");
    generated.add(" *");
    generated.add(" * Unless required by applicable law or agreed to in writing, software");
    generated.add(" * distributed under the License is distributed on an \"AS IS\" BASIS,");
    generated.add(" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
    generated.add(" * See the License for the specific language governing permissions and");
    generated.add(" * limitations under the License.");
    generated.add(" */");
    generated.add("");
    generated.add("// default package");
    generated.add("");
    int indexOfImports = generated.size();
    generated.add("");
    generate(generated, SOURCE_MAIN.resolve("Basics.java"), imports);
    generated.add("");
    generate(generated, SOURCE_MAIN.resolve("JdkTool.java"), imports);
    generated.add("");
    generate(generated, SOURCE_MAIN.resolve("Bach.java"), imports);
    generated.addAll(indexOfImports, imports);

    // write generated lines to temporary file
    Path generatedPath = TARGET.resolve("Bach.java");
    Files.createDirectories(TARGET);
    Files.deleteIfExists(generatedPath);
    Files.write(generatedPath, generated);
    System.out.println("generated " + generatedPath);

    // only copy if content changed - ignoring initial line, which contains the generation date
    Path publishedPath = Paths.get("Bach.java");
    if (Files.notExists(publishedPath)) {
      throw new AssertionError(publishedPath + " does not exist?!");
    }
    List<String> published = Files.readAllLines(publishedPath);
    published.set(0, "");
    generated.set(0, "");
    int publishedHash = published.hashCode();
    int temporaryHash = generated.hashCode();
    System.out.println("generated hash code is 0x" + Integer.toHexString(temporaryHash));
    System.out.println("published hash code is 0x" + Integer.toHexString(publishedHash));
    if (publishedHash != temporaryHash) {
      publishedPath.toFile().setWritable(true);
      Files.copy(generatedPath, publishedPath, StandardCopyOption.REPLACE_EXISTING);
      publishedPath.toFile().setWritable(false);
      System.out.println("new version of Bach.java generated - don't forget to publish it!");
    }
  }

  static void generate(List<String> target, Path source, Set<String> imports) throws IOException {
    List<String> lines = Files.readAllLines(source);
    boolean head = true;
    for (String line : lines) {
      if (head) {
        if (line.startsWith("import")) {
          imports.add(line);
        }
        if (line.equals("// Bach.java")) {
          head = false;
        }
        continue;
      }
      target.add(line);
    }
  }

  static void compile() throws IOException {
    System.out.printf("%n[compile]%n%n");

    // main
    JdkTool.Javac javac = new JdkTool.Javac();
    javac.generateAllDebuggingInformation = true;
    javac.destinationPath = TARGET_MAIN;
    javac.toCommand().add(TARGET.resolve("Bach.java")).run();

    // test
    javac.destinationPath = TARGET_TEST;
    javac.classSourcePath = List.of(SOURCE_TEST);
    javac.classPath =
        List.of(
            TARGET_MAIN,
            Basics.resolve("org.junit.jupiter", "junit-jupiter-api", JUNIT_JUPITER_VERSION),
            Basics.resolve("org.junit.platform", "junit-platform-commons", JUNIT_PLATFORM_VERSION),
            Basics.resolve("org.opentest4j", "opentest4j", OPENTEST4J_VERSION));
    javac.run();
  }

  static void javadoc() throws IOException {
    System.out.printf("%n[javadoc]%n%n");

    Files.createDirectories(JAVADOC);
    JdkTool.run(
        "javadoc",
        "-quiet",
        "-Xdoclint:all,-missing",
        "-package",
        "-linksource",
        "-link",
        "http://download.java.net/java/jdk9/docs/api",
        "-d",
        JAVADOC,
        Paths.get("Bach.java"));
  }

  static void jar() throws IOException {
    System.out.printf("%n[jar]%n%n");

    Files.createDirectories(ARTIFACTS);
    jar("bach.jar", TARGET_MAIN, ".");
    jar("bach-sources.jar", SOURCE_MAIN, ".");
    jar("bach-javadoc.jar", JAVADOC, ".");
  }

  static void jar(String artifact, Path path, Object... contents) {
    JdkTool.Jar jar = new JdkTool.Jar();
    jar.file = ARTIFACTS.resolve(artifact);
    jar.path = path;
    JdkTool.Command command = jar.toCommand();
    command.mark(5);
    Arrays.stream(contents).forEach(command::add);
    command.run();
  }

  static void jdeps() throws IOException {
    System.out.printf("%n[jdeps]%n%n");

    JdkTool.Jdeps jdeps = new JdkTool.Jdeps();
    jdeps.summary = true;
    jdeps.recursive = true;
    jdeps.toCommand().add(ARTIFACTS.resolve("bach.jar")).run();
  }

  static void test() throws IOException {
    System.out.printf("%n[test]%n%n");

    String repo = "http://repo1.maven.org/maven2";
    String user = "org/junit/platform";
    String name = "junit-platform-console-standalone";
    String file = name + "-" + JUNIT_PLATFORM_VERSION + ".jar";
    URI uri = URI.create(String.join("/", repo, user, name, JUNIT_PLATFORM_VERSION, file));
    Path jar = Basics.download(uri, TOOLS.resolve(name), file, p -> true);
    JdkTool.run(
        "java",
        "-ea",
        "-jar",
        jar,
        "--class-path",
        TARGET_TEST,
        "--class-path",
        TARGET_MAIN,
        "--scan-classpath");
  }
}
