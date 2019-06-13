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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

interface Build {

  Path SOURCE = Paths.get("src/modules/de.sormuras.bach/main/java/de/sormuras/bach");
  Path TARGET = Paths.get("target", "build");

  static void main(String... args) {
    try {
      // format();
      // Basics.resolve("org.junit.jupiter", "junit-jupiter-engine", JUNIT_JUPITER_VERSION);
      // clean();
      generate();
      // compile();
      // test();
      // javadoc();
      // jar();
      // jdeps();
    } catch (Throwable throwable) {
      System.err.printf("build failed due to: %s%n", throwable);
      throwable.printStackTrace();
      System.exit(1);
    }
  }

  //  static void format() throws IOException {
  //    System.out.printf("%n[format]%n%n");
  //
  //    String mode = Boolean.getBoolean("bach.format.replace") ? "replace" : "validate";
  //    String repo = "https://jitpack.io";
  //    String user = "com/github/sormuras";
  //    String name = "google-java-format";
  //    String version = "validate-SNAPSHOT";
  //    String file = name + "-" + version + "-all-deps.jar";
  //    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
  //    Path jar = Basics.download(uri, TOOLS.resolve(name));
  //    JdkTool.Command format = new JdkTool.Java().toCommand();
  //    format.add("-jar");
  //    format.add(jar);
  //    format.add("--" + mode);
  //    format.mark(5);
  //    List<Path> roots = List.of(Paths.get("src"), Paths.get("demo"));
  //    format.addAll(roots, Basics::isJavaFile);
  //    format.run();
  //  }

  //  static void clean() throws IOException {
  //    System.out.printf("%n[clean]%n%n");
  //
  //    Basics.treeDelete(TARGET);
  //    System.out.println("deleted " + TARGET);
  //  }

  static void generate() throws IOException {
    System.out.printf("%n[generate]%n%n");

    var imports = new TreeSet<String>();
    var dragons = new ArrayList<String>();
    var generated = new ArrayList<String>();
    generated.add("// THIS FILE WAS GENERATED ON " + Instant.now());
    generated.add("/*");
    generated.add(" * Bach - Java Shell Builder");
    generated.add(" * Copyright (C) 2019 Christian Stein");
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
    generate(generated, SOURCE.resolve("Bach.java"), imports, "");
    int indexOfDragons = generated.size() - 1;
    dragons.add("");
    generate(dragons, SOURCE.resolve("Project.java"), imports, "  ");
    dragons.add("");
    generate(dragons, SOURCE.resolve("Command.java"), imports, "  ");
    dragons.add("");
    generate(dragons, SOURCE.resolve("Run.java"), imports, "  ");
    generated.addAll(indexOfDragons, dragons);
    generated.addAll(indexOfImports, imports.stream().filter(i -> !i.startsWith("import static")).collect(Collectors.toList()));
    generated.add(indexOfImports, "");
    generated.addAll(indexOfImports, imports.stream().filter(i -> i.startsWith("import static")).collect(Collectors.toList()));

    // write generated lines to temporary file
    var generatedPath = TARGET.resolve("Bach.java");
    Files.createDirectories(TARGET);
    Files.deleteIfExists(generatedPath);
    Files.write(generatedPath, generated);
    System.out.println("Generated " + generatedPath + " with " + generated.size() + " lines.");

    // only copy if content changed - ignoring initial line, which contains the generation date
    var publishedPath = Path.of("src", "bach", "Bach.java");
    var published = Files.readAllLines(publishedPath);
    published.set(0, "");
    generated.set(0, "");
    int publishedHash = published.hashCode();
    int temporaryHash = generated.hashCode();
    System.out.println("Generated hash code is 0x" + Integer.toHexString(temporaryHash));
    System.out.println("Published hash code is 0x" + Integer.toHexString(publishedHash));
    if (publishedHash != temporaryHash) {
      publishedPath.toFile().setWritable(true);
      Files.copy(generatedPath, publishedPath, StandardCopyOption.REPLACE_EXISTING);
      publishedPath.toFile().setWritable(false);
      System.out.println("New version of Bach.java generated - don't forget to publish it!");
    }
  }

  static void generate(List<String> target, Path source, Set<String> imports, String indentation)
      throws IOException {
    var lines = Files.readAllLines(source);
    boolean head = true;
    for (var line : lines) {
      if (head) {
        if (line.startsWith("import")) {
          imports.add(line);
        }
        if (line.equals("/*BODY*/")) {
          head = false;
        }
        continue;
      }
      if (line.isEmpty()) {
        target.add("");
        continue;
      }
      var newLine = indentation + line.replace("/*STATIC*/", "static");
      target.add(newLine);
    }
  }

  //  static void compile() throws IOException {
  //    System.out.printf("%n[compile]%n%n");
  //
  //    // main
  //    JdkTool.Javac javac = new JdkTool.Javac();
  //    javac.generateAllDebuggingInformation = true;
  //    javac.destinationPath = TARGET_MAIN;
  //    javac.toCommand().add(TARGET.resolve("Bach.java")).run();
  //
  //    // test
  //    javac.destinationPath = TARGET_TEST;
  //    javac.classSourcePath = List.of(SOURCE_TEST);
  //    javac.classPath =
  //        List.of(
  //            TARGET_MAIN,
  //            Basics.resolve("org.junit.jupiter", "junit-jupiter-api", JUNIT_JUPITER_VERSION),
  //            Basics.resolve("org.junit.platform", "junit-platform-commons",
  // JUNIT_PLATFORM_VERSION),
  //            Basics.resolve("org.opentest4j", "opentest4j", OPENTEST4J_VERSION));
  //    javac.run();
  //  }

  //  static void javadoc() throws IOException {
  //    System.out.printf("%n[javadoc]%n%n");
  //
  //    Files.createDirectories(JAVADOC);
  //    JdkTool.run(
  //        "javadoc",
  //        "-quiet",
  //        "-Xdoclint:all,-missing",
  //        "-package",
  //        "-linksource",
  //        "-link",
  //        "http://download.java.net/java/jdk9/docs/api",
  //        "-d",
  //        JAVADOC,
  //        Paths.get("Bach.java"));
  //  }

  //  static void jar() throws IOException {
  //    System.out.printf("%n[jar]%n%n");
  //
  //    Files.createDirectories(ARTIFACTS);
  //    jar("bach.jar", TARGET_MAIN, ".");
  //    jar("bach-sources.jar", SOURCE_MAIN, ".");
  //    jar("bach-javadoc.jar", JAVADOC, ".");
  //  }

  //  static void jar(String artifact, Path path, Object... contents) {
  //    JdkTool.Jar jar = new JdkTool.Jar();
  //    jar.file = ARTIFACTS.resolve(artifact);
  //    jar.path = path;
  //    JdkTool.Command command = jar.toCommand();
  //    command.mark(5);
  //    Arrays.stream(contents).forEach(command::add);
  //    command.run();
  //  }

  //  static void jdeps() throws IOException {
  //    System.out.printf("%n[jdeps]%n%n");
  //
  //    JdkTool.Jdeps jdeps = new JdkTool.Jdeps();
  //    jdeps.summary = true;
  //    jdeps.recursive = true;
  //    jdeps.toCommand().add(ARTIFACTS.resolve("bach.jar")).run();
  //  }

  //  static void test() throws IOException {
  //    System.out.printf("%n[test]%n%n");
  //
  //    String repo = "http://repo1.maven.org/maven2";
  //    String user = "org/junit/platform";
  //    String name = "junit-platform-console-standalone";
  //    String file = name + "-" + JUNIT_PLATFORM_VERSION + ".jar";
  //    URI uri = URI.create(String.join("/", repo, user, name, JUNIT_PLATFORM_VERSION, file));
  //    Path jar = Basics.download(uri, TOOLS.resolve(name), file, p -> true);
  //    JdkTool.run(
  //        "java",
  //        "-ea",
  //        "-jar",
  //        jar,
  //        "--class-path",
  //        TARGET_TEST,
  //        "--class-path",
  //        TARGET_MAIN,
  //        "--scan-classpath");
  //  }
}
