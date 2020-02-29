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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Bach's base module. */
class SingleFileSourceCodeGenerator {

  public static void main(String... args) throws Exception {
    generate(
        Path.of("src", "bach", "Bach.java.template"),
        Path.of("src", "bach", "Bach.java"),
        Path.of("src", "de.sormuras.bach", "main", "java").toString());
  }

  public static void generate(Path template, Path target, String... inputs) throws Exception {
    var templates = Files.readAllLines(template);

    var files = new ArrayList<Path>();
    for (var input : inputs) {
      var path = Path.of(input);
      if (Files.isRegularFile(path)) {
        files.add(path);
        continue;
      }
      try (var stream = Files.walk(path)) {
        stream
            .sorted()
            .filter(SingleFileSourceCodeGenerator::isRegularJavaSourceFile)
            .forEach(files::add);
      }
    }

    var sources = files.stream().map(Source::of).collect(Collectors.toList());
    sources.forEach(
        source -> {
          System.out.println(source.path);
          System.out.println("package = " + source.packageName);
          System.out.println("imports = " + source.imports);
          System.out.println("  lines = " + source.lines.size());
        });

    var generator = new SingleFileSourceCodeGenerator(templates, sources);
    var lines = generator.toLines();
    // lines.forEach(System.out::println);
    Files.write(target, lines);
    System.out.printf("%nGenerated %d lines: %s%n", lines.size(), target.toUri());
  }

  private static boolean isRegularJavaSourceFile(Path path) {
    if (Files.isDirectory(path)) return false;
    var name = path.getFileName().toString();
    if (!name.endsWith(".java")) return false;
    return name.indexOf('-') == -1; // package-info.java, module-info.java
  }

  private final List<String> templates;
  private final List<Source> sources;

  public SingleFileSourceCodeGenerator(List<String> templates, List<Source> sources) {
    this.templates = templates;
    this.sources = sources;
  }

  public List<String> toLines() {
    var packages = sources.stream().map(source -> source.packageName).collect(Collectors.toList());
    var imports =
        sources.stream()
            .flatMap(source -> source.imports.stream())
            .filter(
                statement -> {
                  for (var name : packages) {
                    if (statement.startsWith("import static " + name)) return false;
                    if (statement.startsWith("import " + name)) return false;
                  }
                  return true;
                })
            .collect(Collectors.toList());
    var lines = new ArrayList<String>();
    for (var template : templates) {
      if (template.equals("${IMPORTS}")) {
        lines.addAll(imports);
        continue;
      }
      if (template.endsWith("${SOURCES}")) {
        var indent = "  "; // extract indentation from marker line
        for (var source : sources) {
          lines.add(indent + "// " + source.path.toString().replace('\\', '/'));
          for (var line : source.lines) {
            if (line.isEmpty()) continue;
            var mangled = line.replace("/*static*/", "static");
            lines.add(indent + mangled);
          }
        }
        continue;
      }
      lines.add(template);
    }
    return lines;
  }

  public static final class Source {

    public static Source of(Path file) {
      var packageName = "";
      var imports = new ArrayList<String>();
      var lines = new ArrayList<String>();
      var lookingForPackageName = true;
      try {
        for (var line : Files.readAllLines(file)) {
          var trim = line.trim();
          if (lookingForPackageName) {
            if (trim.startsWith("package ")) {
              packageName = trim.substring("package ".length(), trim.indexOf(';'));
              lookingForPackageName = false;
            }
            continue;
          }
          if (lines.isEmpty() && line.isEmpty()) continue;
          if (trim.startsWith("import ")) {
            imports.add(trim);
            continue;
          }
          lines.add(line);
        }
      } catch (Exception e) {
        throw new Error(e);
      }
      if (lookingForPackageName) throw new IllegalStateException("No package line found: " + file);
      return new Source(file, packageName, imports, lines);
    }

    private final Path path;
    private final String packageName;
    private final Set<String> imports;
    private final List<String> lines;

    public Source(Path path, String packageName, Collection<String> imports, List<String> lines) {
      this.path = path;
      this.packageName = packageName;
      this.imports = new TreeSet<>(imports);
      this.lines = lines;
    }
  }
}
