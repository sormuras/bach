/*
 * Bach - Library Support Tool
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
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.lang.model.SourceVersion;

/**
 * Library Support Tool.
 *
 * <p>Algorithm outline:
 *
 * <ul>
 *   <li>Collect names of all system modules
 *   <li>Collect names of all project modules below {@code src/}
 *   <li>Collect names of all library modules in {@code lib/} directory
 *   <li>Parse project's {@code module-info.java} files for missing modules
 *   <li>Download missing modules
 *   <li>Flood-fill {@code lib} directory
 * </ul>
 *
 * <p>Usage samples:
 *
 * <ul>
 *   <li>{@code java Lib.java} -- resolve all missing modules
 *   <li>{@code java Lib.java [org.junit.jupiter[@5.5.1] ...]} -- also resolve specified module(s)
 * </ul>
 */
public class Lib {

  public static void main(String... args) {
    var project = new Project();
    var commandRequires = project.requires(Set.of(args));
    var missing = project.missing(commandRequires, project.requires, project.libraryRequires());
    if (!missing.isEmpty()) {
      var worker = new Worker(project);
      worker.load(missing);
      while (true) {
        var libraries = project.missing(project.libraryRequires());
        if (libraries.isEmpty()) {
          break;
        }
        worker.load(libraries);
      }
    }
    project.libraryList();
  }

  static class Project {

    final Path lib = Path.of(System.getProperty("lib", "lib"));
    final Path src = Path.of(System.getProperty("src", "src"));
    final Set<String> modules = modules(System.getProperty("module", "*"));
    final Map<String, Set<Version>> requires = requires();
    final Set<String> systems = mapAll(ModuleFinder.ofSystem(), ModuleDescriptor::name);

    /** Declared modules of this project. */
    private Set<String> modules(String module) {
      if (!"*".equals(module)) {
        return Set.of(module.split(","));
      }
      if (!Files.isDirectory(src)) {
        return Set.of();
      }
      try (var entries = Files.newDirectoryStream(src, Files::isDirectory)) {
        return StreamSupport.stream(entries.spliterator(), false)
            .map(Path::getFileName)
            .map(Path::toString)
            .filter(SourceVersion::isName)
            .collect(Collectors.toCollection(TreeSet::new));
      } catch (IOException e) {
        throw new UncheckedIOException("reading directory failed: " + src, e);
      }
    }

    private boolean isModuleInfo(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
    }

    private Map<String, Set<Version>> requires() {
      var map = new TreeMap<String, Set<Version>>();
      var requiresPattern =
          Pattern.compile(
              "(?:requires)" // key word
                  + "(?:\\s+[\\w.]+)?" // optional modifiers
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                  + ";"); // end marker
      for (var module : modules) {
        var root = src.resolve(module);
        try (var stream = Files.find(root, 9, (p, __) -> isModuleInfo(p))) {
          for (var moduleInfo : stream.collect(Collectors.toSet())) {
            var source = Files.readString(moduleInfo);
            var requiresMatcher = requiresPattern.matcher(source);
            while (requiresMatcher.find()) {
              var requiredName = requiresMatcher.group(1);
              Optional.ofNullable(requiresMatcher.group(2))
                  .ifPresentOrElse(
                      version ->
                          map.merge(
                              requiredName,
                              Set.of(Version.parse(version)),
                              (a, b) ->
                                  Set.of(
                                      Stream.concat(a.stream(), b.stream())
                                          .toArray(Version[]::new))),
                      () -> map.putIfAbsent(requiredName, Set.of()));
            }
          }
        } catch (IOException e) {
          throw new UncheckedIOException("reading module-info.java files failed: " + root, e);
        }
      }
      return map;
    }

    /**
     * Compute requires map from strings.
     *
     * <p>String: {@code "a", "b@1", "c", "b@2", "a"} Result: {@code { a=[], b=[1, 2], c=[] }}
     */
    private Map<String, Set<Version>> requires(Iterable<String> strings) {
      var map = new TreeMap<String, Set<Version>>();
      for (var string : strings) {
        var versionMarkerIndex = string.indexOf('@');
        if (versionMarkerIndex == -1) {
          map.putIfAbsent(string, Set.of());
          continue;
        }
        var module = string.substring(0, versionMarkerIndex);
        var versions = Set.of(Version.parse(string.substring(versionMarkerIndex + 1)));
        map.merge(
            module,
            versions,
            (a, b) -> Set.of(Stream.concat(a.stream(), b.stream()).toArray(Version[]::new)));
      }
      return Collections.unmodifiableMap(map);
    }

    private Set<String> mapAll(ModuleFinder finder, Function<ModuleDescriptor, String> mapper) {
      return finder.findAll().stream()
          .map(ModuleReference::descriptor)
          .map(mapper)
          .collect(Collectors.toCollection(TreeSet::new));
    }

    Set<String> libraryModules() {
      return mapAll(ModuleFinder.of(lib), ModuleDescriptor::name);
    }

    Map<String, Set<Version>> libraryRequires() {
      var dependence =
          ModuleFinder.of(lib).findAll().stream()
              .map(ModuleReference::descriptor)
              .map(ModuleDescriptor::requires)
              .flatMap(Set::stream)
              .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC))
              .map(r -> r.name() + r.compiledVersion().map(v -> "@" + v).orElse(""))
              .collect(Collectors.toSet());
      return requires(dependence);
    }

    void libraryList() {
      var finder = ModuleFinder.of(lib);
      System.out.printf("Library '%s' contains %d module(s)%n", lib, finder.findAll().size());
      for (var nameAndVersion : mapAll(finder, ModuleDescriptor::toNameAndVersion)) {
        System.out.println(nameAndVersion);
      }
    }

    @SafeVarargs
    final Map<String, Set<Version>> missing(
        Map<String, Set<Version>> initial, Map<String, Set<Version>>... more) {
      var missing = new TreeMap<>(initial);
      for (var map : more) {
        missing.putAll(map);
      }
      missing.putAll(requires);
      missing.putAll(libraryRequires());
      modules.forEach(missing::remove);
      libraryModules().forEach(missing::remove);
      systems.forEach(missing::remove);
      return missing;
    }
  }

  static class Worker {

    class Lookup {
      final String name;
      final Properties properties;
      final Set<Pattern> patterns;

      Lookup(String name) {
        this.name = name;
        var uri = "https://github.com/sormuras/modules/raw/master/" + name;
        var home = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
        try {
          Files.createDirectories(home);
        } catch (IOException e) {
          throw new UncheckedIOException("Creating directories failed: " + home, e);
        }
        var user = loadFile(home.resolve(name), URI.create(uri));
        var defaults = load(new Properties(), user);
        this.properties = load(new Properties(defaults), project.lib.resolve(name));
        this.patterns =
            properties.keySet().stream()
                .map(Object::toString)
                .filter(key -> !SourceVersion.isName(key))
                .map(Pattern::compile)
                .collect(Collectors.toSet());
      }

      String get(String key) {
        var value = properties.getProperty(key);
        if (value != null) {
          return value;
        }
        for (var pattern : patterns) {
          if (pattern.matcher(key).matches()) {
            return properties.getProperty(pattern.pattern());
          }
        }
        throw new IllegalStateException("No lookup value mapped for: " + key);
      }

      @Override
      public String toString() {
        var size = properties.size();
        var names = properties.stringPropertyNames().size();
        return String.format(
            "module properties {name: %s, size: %d, names: %d}", name, size, names);
      }
    }

    final Project project;
    final HttpClient browser;
    final Properties moduleUri;
    final Lookup moduleMaven, moduleVersion;

    Worker(Project project) {
      this.project = project;
      this.browser = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      this.moduleUri = load(new Properties(), project.lib.resolve("module-uri.properties"));
      this.moduleMaven = new Lookup("module-maven.properties");
      this.moduleVersion = new Lookup("module-version.properties");
    }

    Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (IOException e) {
          throw new UncheckedIOException("Reading properties failed: " + path, e);
        }
      }
      return properties;
    }

    void load(Map<String, Set<Version>> modules) {
      try {
        Files.createDirectories(project.lib);
      } catch (IOException e) {
        throw new UncheckedIOException("Creating library directory failed: " + project.lib, e);
      }
      for (var entry : modules.entrySet()) {
        loadFile(entry.getKey(), entry.getValue());
      }
    }

    private void loadFile(String module, Set<Version> versions) {
      var uri = moduleUri.getProperty(module);
      if (uri != null) {
        loadFile(project.lib.resolve(module + ".jar"), URI.create(uri));
        return;
      }
      var maven = moduleMaven.get(module).split(":");
      var group = maven[0];
      var artifact = maven[1];
      var version = version(versions).orElse(moduleVersion.get(module));
      loadFile(module, group, artifact, version);
    }

    private void loadFile(String module, String group, String artifact, String version) {
      var host = "https://repo1.maven.org/maven2";
      var file = artifact + '-' + version + ".jar";
      var uri = String.join("/", host, group.replace('.', '/'), artifact, version, file);
      var path = project.lib.resolve(module + "-" + version + ".jar");
      loadFile(path, URI.create(uri));
    }

    private Path loadFile(Path path, URI uri) {
      var request = HttpRequest.newBuilder(uri).GET();
      if (Files.exists(path)) {
        try {
          var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
          var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
          request.setHeader("If-None-Match", etag);
        } catch (Exception e) {
          // System.err.println("Couldn't get 'user:etag' file attribute: " + e);
        }
      }
      try {
        var handler = HttpResponse.BodyHandlers.ofFile(path);
        var response = browser.send(request.build(), handler);
        if (response.statusCode() == 200) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent()) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              // System.err.println("Couldn't set 'user:etag' file attribute: " + e);
            }
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            try {
              var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
              var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
              var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
              Files.setLastModifiedTime(path, fileTime);
            } catch (Exception e) {
              // System.err.println("Couldn't set last modified file attribute: " + e);
            }
          }
          System.out.println(path + " <- " + uri);
        }
      } catch (IOException | InterruptedException e) {
        System.err.println("Failed to load: " + uri + " -> " + e);
      }
      return path;
    }

    private Optional<String> version(Set<Version> versions) {
      if (versions.isEmpty()) {
        return Optional.empty();
      }
      if (versions.size() != 1) {
        throw new IllegalStateException("Too many versions: " + versions);
      }
      return Optional.of(versions.iterator().next().toString());
    }
  }
}
