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
import java.util.NoSuchElementException;
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

  static class Library {

    private final Path path;
    private final Properties uris;
    private final Properties versions;
    private final Iterable<String> versionStars;

    Library(Path path) {
      this.path = path;
      var home = Path.of(System.getProperty("user.home"));
      try {
        this.uris = new Properties();
        this.versions =
            new Properties(
                newDefaultProperties(
                    home.resolve(".bach/modules/module-version.properties"),
                    URI.create(
                        "https://github.com/sormuras/modules/raw/master/module-version.properties")));
        try (var reader = Files.newBufferedReader(path.resolve("module-uri.properties"))) {
          uris.load(reader);
        }
        try (var reader = Files.newBufferedReader(path.resolve("module-version.properties"))) {
          versions.load(reader);
          this.versionStars =
              versions.keySet().stream()
                  .map(Object::toString)
                  .filter(key -> key.endsWith("*"))
                  .collect(Collectors.toSet());
        }
      } catch (IOException e) {
        throw new UncheckedIOException("reading properties failed", e);
      }
    }

    private Properties newDefaultProperties(Path path, URI uri) throws IOException {
      var properties = new Properties();
      if (Files.notExists(path)) {

      }
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      }
      return properties;
    }

    String getUri(String module) {
      var uri = uris.getProperty(module);
      if (uri != null) {
        return uri;
      }
      var group = getGroup(module);
      var artifact = getArtifact(module);
      var version = getVersion(module);

      throw new NoSuchElementException("uri for module '" + module + "' not mapped");
    }

    String getGroup(String module) {
      throw new NoSuchElementException("artifact for module '" + module + "' not mapped");
    }

    String getArtifact(String module) {
      throw new NoSuchElementException("group for module '" + module + "' not mapped");
    }

    String getVersion(String module) {
      var version = versions.getProperty(module);
      if (version != null) {
        return version;
      }
      for (var star : versionStars) {
        if (module.startsWith(star.substring(0, star.length() - 1))) {
          return versions.getProperty(star);
        }
      }
      throw new NoSuchElementException("version for module '" + module + "' not mapped");
    }
  }

  public static void main(String[] args) {
    var library = new Library(Path.of("lib"));
    System.out.println(library.getVersion("org.junit.jupiter.bla"));

    var moduleSourcePath = "src/*/main/java;src/*/test/java;src/*/test/module;src/*/main/java-9";
    var resolver = new Resolver(library, moduleSourcePath);
    var moduleCompilationUnits = resolver.findModuleCompilationUnits();
    var missingDirect = resolver.findMissingModules(moduleCompilationUnits);
    var missingIndirect = resolver.findMissingRequiredModules();

    // System.out.println("module-info.java files = " + moduleCompilationUnits);
    // System.out.println("modules in library = " + findNamesAndVersions(ModuleFinder.of(library)));
    // System.out.println("missing modules, directly required = " + missingDirect);
    // System.out.println("missing modules, indirectly required = " + missingIndirect);

    var allMissingModules = new TreeSet<String>();
    allMissingModules.addAll(missingDirect);
    allMissingModules.addAll(missingIndirect);
    resolver.resolve(allMissingModules);

    var modules =
        resolver.mapAll(ModuleFinder.of(library.path), ModuleDescriptor::toNameAndVersion);
    System.out.printf("%d module(s) in directory '%s'%n", modules.size(), library.path.toUri());
    modules.forEach(System.out::println);
  }

  private final Library library;
  private final String moduleSourcePath;
  private final UnaryOperator<String> uris;

  private Resolver(Library library, String moduleSourcePath) {
    this.library = library;
    this.moduleSourcePath = moduleSourcePath;
    this.uris = library::getUri;
  }

  private Set<String> mapAll(ModuleFinder finder, Function<ModuleDescriptor, String> mapper) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(mapper)
        .collect(Collectors.toCollection(TreeSet::new));
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
      if (Files.isDirectory(library.path)) {
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
    var systemModuleFinder = ModuleFinder.ofSystem();
    var libraryModuleFinder = ModuleFinder.of(library.path);
    var libraryModuleNames = mapAll(libraryModuleFinder, ModuleDescriptor::name);
    return libraryModuleFinder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::requires)
        .flatMap(Collection::stream)
        // .filter(!ModuleDescriptor.Requires.Modifier.STATIC)
        .map(ModuleDescriptor.Requires::name)
        .sorted()
        .distinct()
        .filter(Predicate.not(libraryModuleNames::contains))
        .filter(name -> systemModuleFinder.find(name).isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private void resolve(Iterable<String> modules) {
    // System.out.println("resolving modules = " + modules);
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
      var target = library.path.resolve(module + ".jar");
      download(httpClient, source, target);
    }
  }

  private void download(HttpClient httpClient, URI uri, Path file) {
      var request = HttpRequest.newBuilder(uri).setHeader("If-Modified-Since", "").GET().build();
      var handler = HttpResponse.BodyHandlers.ofFile(file);
      try {
        var response = httpClient.send(request, handler);
        if (response.statusCode() == 200) {
          System.out.println("Loaded " + response.body());
        }
        if (response.statusCode() == 304) {
          System.out.println("Already " + response.body());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

  }
}
