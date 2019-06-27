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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

interface Build {

  Path SOURCE = Paths.get("src/modules/de.sormuras.bach/main/java/de/sormuras/bach");
  Path TARGET = Paths.get("target", "build");

  static void main(String... args) {
    try {
      generate();
      build();
    } catch (Throwable throwable) {
      System.err.printf("Build failed due to: %s%n", throwable);
      throwable.printStackTrace();
      System.exit(1);
    }
  }

  static void generate() throws Exception {
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
    for (var dragon : generateDragons()) {
      if (dragon.equals(SOURCE.resolve("Bach.java"))) {
        continue;
      }
      System.out.println("Processing " + dragon + "...");
      dragons.add("");
      generate(dragons, dragon, imports, "  ");
    }
    generated.addAll(indexOfDragons, dragons);
    generated.addAll(
        indexOfImports,
        imports.stream().filter(i -> !i.startsWith("import static")).collect(Collectors.toList()));
    generated.add(indexOfImports, "");
    generated.addAll(
        indexOfImports,
        imports.stream().filter(i -> i.startsWith("import static")).collect(Collectors.toList()));

    // write generated lines to temporary file
    var generatedPath = TARGET.resolve("Bach.java");
    Files.createDirectories(TARGET);
    Files.deleteIfExists(generatedPath);
    Files.write(generatedPath, generated);
    System.out.println();
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
      System.out.println("Generated hash code is 0x" + Integer.toHexString(temporaryHash));
    }
  }

  static List<Path> generateDragons() throws Exception {
    var dragons = new ArrayList<Path>();
    try (var stream = Files.newDirectoryStream(SOURCE, "*.java")) {
      stream.forEach(dragons::add);
    }
    dragons.sort(Comparator.comparing(Path::toString));
    return dragons;
  }

  static void generate(List<String> target, Path source, Set<String> imports, String indentation)
      throws Exception {
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

  static void build() throws Exception {
    System.out.printf("%n[build]%n%n");

    System.setProperty("debug".substring(1), "true");
    var bach = new Bach();
    bach.build();
  }
}
