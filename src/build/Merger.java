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
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.spi.ToolProvider;

/** Merge types of module/package {@code de.sormuras.bach} into {@code Bach.java}. */
@SuppressWarnings("WeakerAccess")
class Merger {
  static Path SOURCE = Path.of("src/modules/de.sormuras.bach/main/java/de/sormuras/bach");
  static Path TARGET = Path.of("bin/merged");
  static List<String> TYPES = List.of("Command", "Task", "Util");

  public static void main(String[] args) throws Exception {
    System.out.printf("Merging %d types into Bach.java {%n", TYPES.size());
    TYPES.forEach(type -> System.out.printf("  %s.java%n", type));
    System.out.printf("}%n");

    new Merger().merge();
  }

  List<String> generated = new ArrayList<>();
  Set<String> imports = new TreeSet<>();

  void merge() throws Exception {
    generateLicense();
    var indexOfImports = generated.size();
    generated.add("");
    read(SOURCE.resolve("Bach.java"), generated, "");
    var indexOfTypes = generated.size() - 1;
    generateTypes(indexOfTypes);
    generateImports(indexOfImports);

    var generatedFile = TARGET.resolve("Bach.java");
    var publishedFile = Path.of("src", "bach", "Bach.java");
    write(generatedFile);
    javac(generatedFile);
    copy(generatedFile, publishedFile);
  }

  void generateLicense() {
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
  }

  void generateImports(int index) {
    var imports = new TreeSet<>(this.imports);
    imports.removeIf(i -> i.startsWith("import static"));
    var statics = new TreeSet<>(this.imports);
    statics.removeAll(imports);
    generated.addAll(index, imports);
    generated.add(index, "");
    generated.addAll(index, statics);
  }

  void generateTypes(int index) throws Exception {
    var list = new ArrayList<String>();
    for (var type : TYPES) {
      list.add("");
      read(SOURCE.resolve(type + ".java"), list, "  ");
    }
    generated.addAll(index, list);
  }

  void read(Path source, List<String> list, String indentation) throws Exception {
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
      if (line.isBlank()) {
        list.add("");
        continue;
      }
      var newLine = line.replace("/*STATIC*/", "static");
      list.add(indentation + newLine);
    }
  }

  void write(Path path) throws Exception {
    Files.createDirectories(TARGET);
    Files.deleteIfExists(path);
    Files.write(path, generated);
    System.out.println();
    System.out.println("Wrote " + path + " with " + generated.size() + " lines.");
  }

  void javac(Path path) {
    var code =
        ToolProvider.findFirst("javac")
            .orElseThrow()
            .run(System.out, System.err, "-Xlint:-serial", "-Werror", path.toString());
    if (code != 0) {
      throw new Error("javac failed to compile: " + path);
    }
  }

  /** Copy if content changed. Ignoring initial line, which contains the generation date. */
  void copy(Path generatedPath, Path publishedPath) throws Exception {
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
      Thread.sleep(123);
    }
  }
}
