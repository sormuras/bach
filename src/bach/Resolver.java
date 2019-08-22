/*
 * Bach - External Module Resolver
 *
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

// default package

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** External Module Resolver. */
public class Resolver {

  public static void main(String[] args) {
    var library = Path.of("lib");
    var moduleSourcePath = "src/*/main/java;src/*/test/java;src/*/test/module;src/*/main/java-9";
    var moduleUriProperties = new Properties();
    try (var reader = Files.newBufferedReader(library.resolve("module-uri.properties"))) {
      moduleUriProperties.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("reading properties failed", e);
    }

    var resolver = new Resolver(library, moduleSourcePath, moduleUriProperties::getProperty);
    var moduleCompilationUnits = resolver.findModuleCompilationUnits();
    var missingDirect = resolver.findMissingModules(moduleCompilationUnits);
    var missingIndirect = resolver.findMissingRequiredModules();

    System.out.println("module-info.java files = " + moduleCompilationUnits);
    System.out.println("modules in library = " + findNamesAndVersions(ModuleFinder.of(library)));
    System.out.println("missing modules, directly required = " + missingDirect);
    System.out.println("missing modules, indirectly required = " + missingIndirect);

    var allMissingModules = new TreeSet<String>();
    allMissingModules.addAll(missingDirect);
    allMissingModules.addAll(missingIndirect);
    resolver.resolve(allMissingModules);

    System.out.printf("Modules in library '%s'%n", library);
    findNamesAndVersions(ModuleFinder.of(library)).forEach(System.out::println);
  }

  private static Set<String> findNamesAndVersions(ModuleFinder finder) {
    return findDescriptions(finder, ModuleDescriptor::toNameAndVersion);
  }

  private static Set<String> findDescriptions(
      ModuleFinder finder, Function<ModuleDescriptor, String> mapper) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(mapper)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private final Path library;
  private final String moduleSourcePath;
  private final UnaryOperator<String> uris;

  private Resolver(Path library, String moduleSourcePath, UnaryOperator<String> uris) {
    this.library = library;
    this.moduleSourcePath = moduleSourcePath;
    this.uris = uris;
  }

  private Set<Path> findModuleCompilationUnits() {
    var declarations = new TreeSet<Path>();
    for (var element : moduleSourcePath.split("[:;]")) {
      var asterisk = element.indexOf('*');
      var root = Path.of(asterisk < 0 ? element : element.substring(0, asterisk));
      var offset = Path.of(asterisk < 0 ? "" : element.substring(asterisk + 2));
      try (var directories = Files.newDirectoryStream(root, Files::isDirectory)) {
        for (var directory : directories) {
          var declaration = directory.resolve(offset).resolve("module-info.java");
          if (Files.exists(declaration)) {
            declarations.add(declaration);
          }
        }
      } catch (IOException e) {
        throw new UncheckedIOException("", e);
      }
    }
    return declarations;
  }

  private Set<String> findMissingModules(Iterable<Path> moduleCompilationUnits) {
    var out = new StringWriter();
    var writer = new PrintWriter(out, true);
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    for (var unit : moduleCompilationUnits) {
      var args = new ArrayList<String>();
      args.add("-d");
      args.add("bin/temp");
      args.add("--module-source-path");
      args.add(moduleSourcePath);
      if (Files.isDirectory(library)) {
        args.add("--module-path");
        args.add(library.toString());
      }
      args.add("-implicit:none"); // Don't generate class files for implicitly referenced files
      args.add("-nowarn"); // Generate no warnings
      args.add(unit.toString());
      javac.run(writer, writer, args.toArray(String[]::new));
    }
    var marker = "error: module not found: ";
    return out.toString()
        .lines()
        .filter(line -> line.contains(marker))
        .map(line -> line.substring(line.indexOf(marker) + marker.length()))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Set<String> findMissingRequiredModules() {
    var finder = ModuleFinder.of(library);
    var found = findDescriptions(finder, ModuleDescriptor::name);
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::requires)
        .flatMap(Collection::stream)
        // .filter(!ModuleDescriptor.Requires.Modifier.STATIC)
        .map(ModuleDescriptor.Requires::name)
        .filter(Predicate.not(found::contains))
        .filter(name -> ModuleFinder.ofSystem().find(name).isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private void resolve(Iterable<String> modules) {
    System.out.println("resolving modules = " + modules);
    download(modules);
    var stillMissingModules = findMissingRequiredModules();
    if (stillMissingModules.isEmpty()) {
      return;
    }
    resolve(stillMissingModules);
  }

  private void download(Iterable<String> modules) {
    var httpClient = HttpClient.newHttpClient();
    for (var module : modules) {
      var source = URI.create(uris.apply(module));
      var target = library.resolve(module + ".jar");
      var request = HttpRequest.newBuilder(source).GET().build();
      var handler = HttpResponse.BodyHandlers.ofFile(target);
      try {
        var response = httpClient.send(request, handler);
        if (response.statusCode() == 200) {
          System.out.println("Resolved " + response.body());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
